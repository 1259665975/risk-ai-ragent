package com.gm.riskaiRagent.service;

import com.gm.riskaiRagent.util.DocumentParser;
import com.gm.riskaiRagent.util.TokenTextChunker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识库清空测试，验证全局 chunk 集合和按文档 chunk 集合都能被正确清理。
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceClearAllTest {

    private static final String CHUNK_ID_SET = "risk-ai:doc:chunk-ids";
    private static final String DOC_CHUNK_PREFIX = "risk-ai:doc:chunks:";

    @Mock
    private DocumentParser documentParser;
    @Mock
    private TokenTextChunker tokenTextChunker;
    @Mock
    private VectorStoreService vectorStoreService;
    @Mock
    private ChunkIndexService chunkIndexService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void clearAllDeletesPerDocChunkKeysAndGlobalSet() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(CHUNK_ID_SET)).thenReturn(Set.of("chunk-1", "chunk-2"));

        String docKeyA = DOC_CHUNK_PREFIX + "docA";
        String docKeyB = DOC_CHUNK_PREFIX + "docB";
        when(stringRedisTemplate.keys(DOC_CHUNK_PREFIX + "*"))
                .thenReturn(new LinkedHashSet<>(List.of(docKeyA, docKeyB)));
        when(setOperations.members(docKeyA)).thenReturn(Set.of("chunk-1"));
        when(setOperations.members(docKeyB)).thenReturn(Set.of("chunk-3"));

        long removed = documentService.clearAll();

        assertEquals(3, removed);
        verify(vectorStoreService).delete(anyList());
        verify(stringRedisTemplate).delete(Set.of(docKeyA, docKeyB));
        verify(stringRedisTemplate).delete(CHUNK_ID_SET);
        verify(chunkIndexService).clear();
    }

    @Test
    void clearAllStillDeletesOrphanPerDocKeysWhenGlobalSetEmpty() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(CHUNK_ID_SET)).thenReturn(Set.of());

        String orphanKey = DOC_CHUNK_PREFIX + "orphan";
        when(stringRedisTemplate.keys(DOC_CHUNK_PREFIX + "*"))
                .thenReturn(Set.of(orphanKey));
        when(setOperations.members(orphanKey)).thenReturn(Set.of("chunk-orphan"));

        long removed = documentService.clearAll();

        assertEquals(1, removed);
        verify(vectorStoreService).delete(anyList());
        verify(stringRedisTemplate).delete(Set.of(orphanKey));
        verify(stringRedisTemplate).delete(CHUNK_ID_SET);
        verify(chunkIndexService).clear();
    }

    @Test
    void clearAllStillClearsRedisWhenMilvusDeleteFails() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(CHUNK_ID_SET)).thenReturn(Set.of("chunk-1"));
        when(stringRedisTemplate.keys(DOC_CHUNK_PREFIX + "*")).thenReturn(Set.of());

        org.mockito.Mockito.doThrow(new RuntimeException("milvus down"))
                .when(vectorStoreService).delete(anyList());

        long removed = documentService.clearAll();

        assertEquals(1, removed);
        verify(stringRedisTemplate).delete(CHUNK_ID_SET);
        verify(chunkIndexService).clear();
    }
}
