package com.gm.riskaiRagent.service;

import com.gm.riskaiRagent.util.KeywordScorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 Redis 倒排索引的 BM25 关键词召回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordRetrievalService {

    private final ChunkIndexService chunkIndexService;

    /**
     * 遍历 Redis chunk 索引并计算 BM25 分数，返回关键词召回的 topK 文档。
     */
    public List<Document> search(String query, List<Long> categoryIds, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Map<String, ChunkIndexService.ChunkIndexEntry> indexed = chunkIndexService.listAll();
        if (indexed.isEmpty()) {
            return List.of();
        }

        Set<String> allowedCategories = categoryIds == null || categoryIds.isEmpty()
                ? null
                : categoryIds.stream().map(String::valueOf).collect(Collectors.toSet());

        List<ScoredDocument> scored = new ArrayList<>();
        for (Map.Entry<String, ChunkIndexService.ChunkIndexEntry> entry : indexed.entrySet()) {
            ChunkIndexService.ChunkIndexEntry chunk = entry.getValue();
            if (chunk == null || chunk.text() == null || chunk.text().isBlank()) {
                continue;
            }
            if (allowedCategories != null) {
                String categoryId = chunk.categoryId();
                if (categoryId == null || !allowedCategories.contains(categoryId)) {
                    continue;
                }
            }
            double score = KeywordScorer.bm25(query, chunk.text());
            if (score <= 0) {
                continue;
            }
            scored.add(new ScoredDocument(entry.getKey(), chunk, score));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
                .limit(Math.max(1, topK))
                .map(this::toDocument)
                .toList();
    }

    /**
     * 把 Redis 索引记录转换成 Spring AI Document，便于后续与向量结果融合。
     */
    private Document toDocument(ScoredDocument scored) {
        ChunkIndexService.ChunkIndexEntry chunk = scored.entry();
        return Document.builder()
                .id(scored.chunkId())
                .text(chunk.text())
                .metadata(Map.of(
                        "source", chunk.source() == null ? "" : chunk.source(),
                        "categoryId", chunk.categoryId() == null ? "" : chunk.categoryId()))
                .score(scored.score())
                .build();
    }

    private record ScoredDocument(String chunkId, ChunkIndexService.ChunkIndexEntry entry, double score) {
    }
}
