package com.gm.riskaiRagent.service;

import com.gm.riskaiRagent.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 增强检索测试，覆盖向量召回、关键词融合和 Rerank 开关下的编排结果。
 */
@ExtendWith(MockitoExtension.class)
class EnhancedRetrievalServiceTest {

    @Mock
    private VectorStoreService vectorStoreService;
    @Mock
    private KeywordRetrievalService keywordRetrievalService;
    @Mock
    private DashScopeRerankService dashScopeRerankService;

    private RagProperties ragProperties;
    private EnhancedRetrievalService enhancedRetrievalService;

    @BeforeEach
    void setUp() {
        ragProperties = new RagProperties();
        enhancedRetrievalService = new EnhancedRetrievalService(
                vectorStoreService, keywordRetrievalService, dashScopeRerankService, ragProperties);
    }

    @Test
    void retrieveUsesHybridAndRerankWhenEnabled() {
        Document vectorDoc = doc("v1", "vector chunk");
        Document keywordDoc = doc("k1", "keyword chunk");
        Document rerankedDoc = doc("v1", "vector chunk", 0.99);

        when(vectorStoreService.similaritySearch(eq("question"), eq(null), eq(20)))
                .thenReturn(List.of(vectorDoc));
        when(keywordRetrievalService.search("question", null, 20))
                .thenReturn(List.of(keywordDoc));
        when(dashScopeRerankService.rerank(eq("question"), any(), eq(5)))
                .thenReturn(List.of(rerankedDoc));

        var result = enhancedRetrievalService.retrieve("question", null, 5);

        assertTrue(result.isHybridUsed());
        assertTrue(result.isRerankUsed());
        assertEquals(1, result.getDocuments().size());
        assertEquals("v1", result.getDocuments().get(0).getId());
        verify(dashScopeRerankService).rerank(eq("question"), any(), eq(5));
    }

    @Test
    void retrieveSkipsHybridAndRerankWhenDisabled() {
        ragProperties.getRetrieval().getHybrid().setEnabled(false);
        ragProperties.getRetrieval().getRerank().setEnabled(false);
        Document vectorDoc = doc("v1", "vector chunk");

        when(vectorStoreService.similaritySearch(eq("question"), eq(null), eq(20)))
                .thenReturn(List.of(vectorDoc));

        var result = enhancedRetrievalService.retrieve("question", null, 5);

        assertFalse(result.isHybridUsed());
        assertFalse(result.isRerankUsed());
        assertEquals(1, result.getDocuments().size());
        verify(keywordRetrievalService, never()).search(any(), any(), anyInt());
        verify(dashScopeRerankService, never()).rerank(any(), any(), anyInt());
    }

    private static Document doc(String id, String text) {
        return doc(id, text, 0.5);
    }

    private static Document doc(String id, String text, double score) {
        return Document.builder()
                .id(id)
                .text(text)
                .metadata(Map.of("source", "policy.pdf"))
                .score(score)
                .build();
    }
}
