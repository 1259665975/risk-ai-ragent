package com.gm.riskaiRagent.util;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BM25 关键词打分测试，验证相关文本获得更高分数。
 */
class KeywordScorerTest {

    @Test
    void bm25PrefersDocumentWithMoreQueryTerms() {
        double high = KeywordScorer.bm25("信用风险 缓释", "信用风险缓释措施包括抵押担保与保证保险");
        double low = KeywordScorer.bm25("信用风险 缓释", "市场风险计量采用历史模拟法");
        assertTrue(high > low);
    }

    @Test
    void tokenizeExtractsCjkBigrams() {
        List<String> tokens = KeywordScorer.tokenize("信用风险");
        assertTrue(tokens.contains("信用"));
        assertTrue(tokens.contains("用风"));
        assertTrue(tokens.contains("风险"));
    }
}
