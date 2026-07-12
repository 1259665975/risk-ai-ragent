package com.gm.riskaiRagent.service;

import com.gm.riskaiRagent.config.RagProperties;
import com.gm.riskaiRagent.dto.EnhancedRetrievalResult;
import com.gm.riskaiRagent.util.ReciprocalRankFusion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 混合检索（向量 + BM25/RRF）与 Rerank 精排编排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedRetrievalService {

    private final VectorStoreService vectorStoreService;
    private final KeywordRetrievalService keywordRetrievalService;
    private final DashScopeRerankService dashScopeRerankService;
    private final RagProperties ragProperties;

    /**
     * 单跳完整检索：向量召回 + 关键词召回 + RRF + Rerank。
     */
    public EnhancedRetrievalResult retrieve(String query, List<Long> categoryIds, int finalTopK) {
        RagProperties.Retrieval retrieval = ragProperties.getRetrieval();
        int candidateTopK = resolveCandidateTopK(finalTopK, retrieval.getHybrid().getCandidateTopK());

        List<Document> vectorCandidates = vectorStoreService.similaritySearch(query, categoryIds, candidateTopK);
        List<Document> fused = fuseCandidates(query, categoryIds, vectorCandidates, retrieval, candidateTopK);

        return finalizeWithRerank(query, fused, finalTopK, retrieval);
    }

    /**
     * 多跳合并后的精排：在已有候选上补充关键词路并 Rerank。
     */
    public EnhancedRetrievalResult refine(String query, List<Document> candidates, List<Long> categoryIds, int finalTopK) {
        RagProperties.Retrieval retrieval = ragProperties.getRetrieval();
        int candidateTopK = resolveCandidateTopK(finalTopK, retrieval.getHybrid().getCandidateTopK());

        List<Document> vectorCandidates = candidates == null ? List.of() : candidates.stream()
                .sorted(Comparator.comparing(Document::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(candidateTopK)
                .toList();

        List<Document> fused = fuseCandidates(query, categoryIds, vectorCandidates, retrieval, candidateTopK);
        return finalizeWithRerank(query, fused, finalTopK, retrieval);
    }

    /**
     * 按配置决定是否启用混合召回；同时有向量和关键词结果时使用 RRF 融合排序。
     */
    private List<Document> fuseCandidates(String query,
                                          List<Long> categoryIds,
                                          List<Document> vectorCandidates,
                                          RagProperties.Retrieval retrieval,
                                          int candidateTopK) {
        if (!retrieval.getHybrid().isEnabled()) {
            return vectorCandidates;
        }

        List<Document> keywordCandidates = keywordRetrievalService.search(query, categoryIds, candidateTopK);
        List<List<Document>> rankedLists = new ArrayList<>();
        if (!vectorCandidates.isEmpty()) {
            rankedLists.add(vectorCandidates);
        }
        if (!keywordCandidates.isEmpty()) {
            rankedLists.add(keywordCandidates);
        }
        if (rankedLists.isEmpty()) {
            return List.of();
        }
        if (rankedLists.size() == 1) {
            return rankedLists.get(0).stream().limit(candidateTopK).toList();
        }

        List<Document> fused = ReciprocalRankFusion.fuse(
                rankedLists,
                retrieval.getHybrid().getRrfK(),
                candidateTopK);
        log.info("Hybrid retrieval fused vector={} keyword={} -> {}",
                vectorCandidates.size(), keywordCandidates.size(), fused.size());
        return fused;
    }

    /**
     * 检索收尾阶段：未开启 Rerank 时直接截断，开启后调用精排模型取最终 topK。
     */
    private EnhancedRetrievalResult finalizeWithRerank(String query,
                                                       List<Document> fused,
                                                       int finalTopK,
                                                       RagProperties.Retrieval retrieval) {
        boolean hybridUsed = retrieval.getHybrid().isEnabled();
        if (fused.isEmpty()) {
            return EnhancedRetrievalResult.builder()
                    .documents(List.of())
                    .hybridUsed(hybridUsed)
                    .rerankUsed(false)
                    .build();
        }

        if (!retrieval.getRerank().isEnabled()) {
            return EnhancedRetrievalResult.builder()
                    .documents(fused.stream().limit(Math.max(1, finalTopK)).toList())
                    .hybridUsed(hybridUsed)
                    .rerankUsed(false)
                    .build();
        }

        List<Document> reranked = dashScopeRerankService.rerank(query, fused, finalTopK);
        return EnhancedRetrievalResult.builder()
                .documents(reranked)
                .hybridUsed(hybridUsed)
                .rerankUsed(true)
                .build();
    }

    private int resolveCandidateTopK(int finalTopK, int configuredCandidateTopK) {
        return Math.max(Math.max(1, finalTopK), configuredCandidateTopK);
    }
}
