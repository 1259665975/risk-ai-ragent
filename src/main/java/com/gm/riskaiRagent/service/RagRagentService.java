package com.gm.riskaiRagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.riskaiRagent.config.RagProperties;
import com.gm.riskaiRagent.dto.MultiHopRetrievalResult;
import com.gm.riskaiRagent.dto.RagentRequest;
import com.gm.riskaiRagent.dto.RagentResponse;
import com.gm.riskaiRagent.dto.ReferenceChunk;
import com.gm.riskaiRagent.entity.RagentLog;
import com.gm.riskaiRagent.util.WebUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Module 2 - RAG question answering for the risk domain, with Redis caching
 * (module 3) and LLM service degradation (module 3).
 */
@Slf4j
@Service
public class RagRagentService {

    /**
     * Mandatory risk-control system prompt (anti-hallucination). The model must answer
     * 100% from the provided internal reference documents only; otherwise it returns a
     * fixed "暂无相关风控规则信息" reply.
     */
    private static final String RISK_SYSTEM_PROMPT = """
            你是专业的金融风控合规顾问，仅依托提供的内部风控参考文档回答用户问题。严格遵守以下规则：
            1、所有回答必须100%溯源参考文档内容，文档无对应信息时，统一回复“暂无相关风控规则信息”，严禁编造、推演、猜测内容；
            2、回答语言简洁、专业、严谨，贴合金融风控业务规范，不输出口语化、无关、冗余内容；
            3、仅解答风控准入、审批规则、反欺诈、合规相关问题，非业务问题统一礼貌拒绝回复；
            4、严禁输出违规、不合规、超出企业内部制度的内容。

            【内部风控参考文档】
            ----------------
            {context}
            ----------------
            """;

    private static final String NO_CONTEXT_PLACEHOLDER = "（未检索到相关风控参考文档）";

    private final ChatModel chatModel;
    private final MultiHopRetrievalService multiHopRetrievalService;
    private final RagentLogService ragentLogService;
    private final RagProperties ragProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RagRagentService(@Lazy ChatModel chatModel,
                        MultiHopRetrievalService multiHopRetrievalService,
                        RagentLogService ragentLogService,
                        RagProperties ragProperties,
                        RedisTemplate<String, Object> redisTemplate,
                        ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.multiHopRetrievalService = multiHopRetrievalService;
        this.ragentLogService = ragentLogService;
        this.ragProperties = ragProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 开放问答入口，不限制知识分类范围。
     */
    public RagentResponse ask(RagentRequest request) {
        return ask(request, null);
    }

    /**
     * 核心问答流程：缓存命中优先，未命中则检索上下文、调用模型、写缓存并记录日志。
     */
    public RagentResponse ask(RagentRequest request, List<Long> categoryIds) {
        long start = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String question = request.getQuestion().trim();

        // 1) Cache lookup.
        String cacheKey = buildCacheKey(question, categoryIds);
        RagentResponse cached = readCache(cacheKey);
        if (cached != null) {
            cached.setTraceId(traceId);
            cached.setFromCache(true);
            cached.setCostMs(System.currentTimeMillis() - start);
            if (!request.isIncludeReferences()) {
                cached.setReferences(Collections.emptyList());
            }
            asyncLog(traceId, question, cached, true, false, "OK", null, start);
            return cached;
        }

        // 2) Retrieve grounding context（单跳 / 多跳由配置 risk-ai.multi-hop.enabled 控制）.
        MultiHopRetrievalResult retrievalResult;
        List<Document> retrieved;
        try {
            retrievalResult = multiHopRetrievalService.retrieve(question, categoryIds);
            retrieved = retrievalResult.getDocuments();
        } catch (Exception e) {
            log.error("Vector search failed, traceId={}", traceId, e);
            retrievalResult = MultiHopRetrievalResult.builder()
                    .documents(Collections.emptyList()).hops(1).multiHopUsed(false).build();
            retrieved = Collections.emptyList();
        }
        List<ReferenceChunk> references = toReferences(retrieved);

        // 3) Invoke the LLM under the risk-control prompt, with degradation on failure.
        boolean degraded = false;
        String status = "OK";
        String errorMsg = null;
        String answer;
        try {
            answer = callModel(question, retrieved);
        } catch (Exception e) {
            degraded = true;
            status = "FAILED";
            errorMsg = truncate(e.getMessage(), 500);
            answer = ragProperties.getDegrade().getFallbackAnswer();
            log.error("LLM invocation failed, degraded. traceId={}", traceId, e);
        }

        long cost = System.currentTimeMillis() - start;
        RagentResponse response = RagentResponse.builder()
                .traceId(traceId)
                .answer(answer)
                .references(request.isIncludeReferences() ? references : Collections.emptyList())
                .fromCache(false)
                .degraded(degraded)
                .costMs(cost)
                .multiHopUsed(retrievalResult.isMultiHopUsed())
                .retrievalHops(retrievalResult.getHops())
                .hybridRetrievalUsed(retrievalResult.isHybridUsed())
                .rerankUsed(retrievalResult.isRerankUsed())
                .build();

        // 4) Cache successful answers only.
        if (!degraded) {
            writeCache(cacheKey, RagentResponse.builder()
                    .traceId(traceId)
                    .answer(answer)
                    .references(references)
                    .fromCache(false)
                    .degraded(false)
                    .costMs(cost)
                    .multiHopUsed(retrievalResult.isMultiHopUsed())
                    .retrievalHops(retrievalResult.getHops())
                    .hybridRetrievalUsed(retrievalResult.isHybridUsed())
                    .rerankUsed(retrievalResult.isRerankUsed())
                    .build());
        }

        // 5) Persist log.
        persistLog(traceId, question, response, retrieved.size(), false, degraded, status, errorMsg, cost);
        return response;
    }

    /**
     * 把检索片段注入风控 System Prompt 后调用大模型生成答案。
     */
    private String callModel(String question, List<Document> retrieved) {
        String context = buildContext(retrieved);
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(RISK_SYSTEM_PROMPT.replace("{context}",
                context.isBlank() ? NO_CONTEXT_PLACEHOLDER : context)));
        messages.add(new UserMessage(question));

        ChatResponse chatResponse = chatModel.call(new Prompt(messages));
        return chatResponse.getResult().getOutput().getText();
    }

    /**
     * 将候选知识片段拼成带序号和来源的上下文，供提示词约束模型引用。
     */
    private String buildContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Document doc : documents) {
            Object source = doc.getMetadata().get("source");
            sb.append("[").append(i++).append("]");
            if (source != null) {
                sb.append(" (source: ").append(source).append(")");
            }
            sb.append("\n").append(doc.getText()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 把检索文档转换为前端引用列表，同一 source 只保留一次以避免重复展示。
     */
    private List<ReferenceChunk> toReferences(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        // 同一文档会被切成多个 chunk，检索可能命中多个片段，引用按 source 去重
        Set<String> seenSources = new LinkedHashSet<>();
        List<ReferenceChunk> list = new ArrayList<>();
        for (Document doc : documents) {
            String source = asString(doc.getMetadata().get("source"));
            if (source != null && !seenSources.add(source)) {
                continue;
            }
            list.add(ReferenceChunk.builder()
                    .content(truncate(doc.getText(), 500))
                    .source(source)
                    .score(doc.getScore())
                    .build());
        }
        return list;
    }

    // ---- cache helpers ----

    /**
     * 缓存键包含问题、分类范围和检索策略开关，避免不同策略复用旧答案。
     */
    private String buildCacheKey(String question, List<Long> categoryIds) {
        String raw = question;
        if (categoryIds != null && !categoryIds.isEmpty()) {
            raw = raw + "|" + categoryIds.stream().sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        }
        if (ragProperties.getMultiHop().isEnabled()) {
            raw = raw + "|mh";
        }
        RagProperties.Retrieval retrieval = ragProperties.getRetrieval();
        if (retrieval.getHybrid().isEnabled()) {
            raw = raw + "|hybrid";
        }
        if (retrieval.getRerank().isEnabled()) {
            raw = raw + "|rerank";
        }
        String hash = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
        return ragProperties.getCache().getKeyPrefix() + hash;
    }

    private RagentResponse readCache(String key) {
        if (!ragProperties.getCache().isEnabled()) {
            return null;
        }
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value instanceof RagentResponse ragent ? ragent : null;
        } catch (Exception e) {
            log.warn("Cache read failed, key={}", key, e);
            return null;
        }
    }

    private void writeCache(String key, RagentResponse response) {
        if (!ragProperties.getCache().isEnabled()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, response,
                    Duration.ofMinutes(ragProperties.getCache().getTtlMinutes()));
        } catch (Exception e) {
            log.warn("Cache write failed, key={}", key, e);
        }
    }

    // ---- logging helpers ----

    private void asyncLog(String traceId, String question, RagentResponse response,
                          boolean hitCache, boolean degraded, String status, String errorMsg, long start) {
        persistLog(traceId, question, response,
                response.getReferences() == null ? 0 : response.getReferences().size(),
                hitCache, degraded, status, errorMsg, System.currentTimeMillis() - start);
    }

    private void persistLog(String traceId, String question, RagentResponse response, int matched,
                            boolean hitCache, boolean degraded, String status, String errorMsg, long cost) {
        RagentLog ragentLog = new RagentLog();
        ragentLog.setTraceId(traceId);
        ragentLog.setQuestion(question);
        ragentLog.setAnswer(response.getAnswer());
        ragentLog.setReferenceDocs(toJson(response.getReferences()));
        ragentLog.setMatchedCount(matched);
        ragentLog.setHitCache(hitCache ? 1 : 0);
        ragentLog.setDegraded(degraded ? 1 : 0);
        ragentLog.setStatus(status);
        ragentLog.setErrorMsg(errorMsg);
        ragentLog.setCostMs(cost);
        ragentLog.setClientIp(WebUtil.clientIp());
        ragentLogService.save(ragentLog);
    }

    private String toJson(Object obj) {
        try {
            return obj == null ? null : objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
