package com.gm.riskaiRagent.service;

import com.gm.riskaiRagent.common.BusinessException;
import com.gm.riskaiRagent.common.ResultCode;
import com.gm.riskaiRagent.util.DocumentParser;
import com.gm.riskaiRagent.util.SupportedDocumentTypes;
import com.gm.riskaiRagent.util.TokenTextChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.gm.riskaiRagent.dto.IngestResponse;

/**
 * Module 1 - Knowledge ingestion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final String CHUNK_ID_SET = "risk-ai:doc:chunk-ids";
    private static final String DOC_CHUNK_SET_PREFIX = "risk-ai:doc:chunks:";

    private final DocumentParser documentParser;
    private final TokenTextChunker tokenTextChunker;
    private final VectorStoreService vectorStoreService;
    private final ChunkIndexService chunkIndexService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 使用默认分类执行文档入库，兼容开放上传接口。
     */
    public IngestResponse ingest(MultipartFile file) throws IOException {
        return ingest(file, null);
    }

    /**
     * 完整入库流程：校验格式、解析正文、Token 切片、写入向量库、建立 Redis 关键词索引。
     */
    public IngestResponse ingest(MultipartFile file, Long categoryId) throws IOException {
        String fileName = file.getOriginalFilename();
        SupportedDocumentTypes.validate(fileName);

        DocumentParser.ParseResult parsed = documentParser.parse(file.getBytes(), fileName);
        String content = parsed.text();
        int tokenCount = tokenTextChunker.countTokens(content);
        List<String> chunks = tokenTextChunker.split(content);
        if (chunks.isEmpty()) {
            throw new BusinessException(ResultCode.DOC_PARSE_ERROR,
                    "文档切片结果为空 [" + fileName + "]，请检查文件内容是否有效");
        }

        String docId = UUID.randomUUID().toString().replace("-", "");

        List<Document> documents = new ArrayList<>(chunks.size());
        List<String> chunkIds = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = UUID.randomUUID().toString().replace("-", "");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("docId", docId);
            metadata.put("source", fileName);
            metadata.put("chunkIndex", i);
            if (categoryId != null) {
                metadata.put("categoryId", String.valueOf(categoryId));
            }
            documents.add(Document.builder()
                    .id(chunkId)
                    .text(chunks.get(i))
                    .metadata(metadata)
                    .build());
            chunkIds.add(chunkId);
        }

        vectorStoreService.add(documents);

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            chunkIndexService.index(doc.getId(), doc.getText(), categoryId, fileName);
        }

        if (!chunkIds.isEmpty()) {
            stringRedisTemplate.opsForSet().add(CHUNK_ID_SET, chunkIds.toArray(new String[0]));
            stringRedisTemplate.opsForSet().add(docChunkKey(docId), chunkIds.toArray(new String[0]));
        }

        log.info("Ingested '{}' docId={} categoryId={} type={} mode={} chunks={}",
                fileName, docId, categoryId, parsed.fileType(), parsed.parseMode(), chunks.size());

        return IngestResponse.builder()
                .fileName(fileName)
                .docId(docId)
                .charCount(content.length())
                .tokenCount(tokenCount)
                .chunkCount(chunks.size())
                .fileType(parsed.fileType())
                .parseMode(parsed.parseMode())
                .build();
    }

    /**
     * 按业务文档 ID 删除所有关联 chunk，保持 Milvus、关键词索引和追踪集合一致。
     */
    public void deleteByDocId(String docId) {
        if (docId == null || docId.isBlank()) {
            return;
        }
        Set<String> ids = stringRedisTemplate.opsForSet().members(docChunkKey(docId));
        if (ids != null && !ids.isEmpty()) {
            vectorStoreService.delete(new ArrayList<>(ids));
            chunkIndexService.remove(ids);
            stringRedisTemplate.opsForSet().remove(CHUNK_ID_SET, ids.toArray());
            stringRedisTemplate.delete(docChunkKey(docId));
            log.info("Deleted docId={} chunks={}", docId, ids.size());
        }
    }

    /**
     * 清空知识库向量和 Redis 索引，用于重建知识库或测试环境重置。
     */
    public long clearAll() {
        Set<String> allChunkIds = new HashSet<>();

        Set<String> globalIds = stringRedisTemplate.opsForSet().members(CHUNK_ID_SET);
        if (globalIds != null) {
            allChunkIds.addAll(globalIds);
        }

        Set<String> docKeys = stringRedisTemplate.keys(DOC_CHUNK_SET_PREFIX + "*");
        int docKeyCount = 0;
        if (docKeys != null && !docKeys.isEmpty()) {
            docKeyCount = docKeys.size();
            for (String docKey : docKeys) {
                Set<String> docChunkIds = stringRedisTemplate.opsForSet().members(docKey);
                if (docChunkIds != null) {
                    allChunkIds.addAll(docChunkIds);
                }
            }
            stringRedisTemplate.delete(docKeys);
        }

        if (!allChunkIds.isEmpty()) {
            try {
                vectorStoreService.delete(new ArrayList<>(allChunkIds));
            } catch (Exception e) {
                // Spring AI Milvus delete 可能为观测指标调用 embedding.dimensions()，API 异常时仍清理 Redis 索引
                log.warn("Milvus delete failed during clearAll, redis indexes will still be removed: {}",
                        e.getMessage());
            }
        }
        stringRedisTemplate.delete(CHUNK_ID_SET);
        chunkIndexService.clear();

        log.info("Cleared knowledge base, removed {} chunks, {} per-doc redis keys",
                allChunkIds.size(), docKeyCount);
        return allChunkIds.size();
    }

    private String docChunkKey(String docId) {
        return DOC_CHUNK_SET_PREFIX + docId;
    }
}
