package com.gm.riskaiRagent.util;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RRF 融合工具测试，验证多路召回排序可以稳定合并。
 */
class ReciprocalRankFusionTest {

    @Test
    void fusePromotesDocumentsPresentInBothLists() {
        Document shared = Document.builder()
                .id("shared")
                .text("shared chunk")
                .metadata(Map.of("source", "policy.pdf"))
                .score(0.6)
                .build();
        Document vectorOnly = Document.builder()
                .id("vector-only")
                .text("vector only")
                .metadata(Map.of("source", "vector.pdf"))
                .score(0.9)
                .build();
        Document keywordOnly = Document.builder()
                .id("keyword-only")
                .text("keyword only")
                .metadata(Map.of("source", "keyword.pdf"))
                .score(0.8)
                .build();

        List<Document> fused = ReciprocalRankFusion.fuse(
                List.of(
                        List.of(vectorOnly, shared),
                        List.of(shared, keywordOnly)),
                60,
                3);

        assertEquals("shared", fused.get(0).getId());
        assertEquals(3, fused.size());
    }
}
