package com.gm.riskaiRagent.service;

import com.gm.riskaiRagent.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量库服务，封装 Spring AI VectorStore 的写入、相似度检索和删除操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    /**
     * 批量写入切片到向量库，空集合直接跳过。
     */
    public void add(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        vectorStore.add(documents);
        log.info("Added {} chunks to vector store", documents.size());
    }

    public List<Document> similaritySearch(String query) {
        return similaritySearch(query, null);
    }

    public List<Document> similaritySearch(String query, List<Long> categoryIds) {
        return similaritySearch(query, categoryIds, ragProperties.getRag().getTopK());
    }

    /**
     * 向量相似度检索（可指定 topK，供多跳检索每一跳使用）。
     */
    public List<Document> similaritySearch(String query, List<Long> categoryIds, int topK) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(Math.max(1, topK))
                .similarityThreshold(ragProperties.getRag().getSimilarityThreshold());

        String filter = buildCategoryFilter(categoryIds);
        if (filter != null) {
            builder.filterExpression(filter);
        }

        return vectorStore.similaritySearch(builder.build());
    }

    /**
     * 按 chunk ID 从向量库删除片段，空集合直接跳过。
     */
    public void delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        vectorStore.delete(ids);
        log.info("Deleted {} chunks from vector store", ids.size());
    }

    /**
     * 把分类 ID 列表转换成 Milvus 过滤表达式。
     */
    private String buildCategoryFilter(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        return categoryIds.stream()
                .map(id -> "categoryId == '" + id + "'")
                .collect(Collectors.joining(" || "));
    }
}
