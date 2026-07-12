package com.gm.riskaiRagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.riskaiRagent.common.BusinessException;
import com.gm.riskaiRagent.common.ResultCode;
import com.gm.riskaiRagent.dto.ChatRequest;
import com.gm.riskaiRagent.dto.RagentRequest;
import com.gm.riskaiRagent.dto.RagentResponse;
import com.gm.riskaiRagent.dto.ReferenceChunk;
import com.gm.riskaiRagent.entity.ChatMessage;
import com.gm.riskaiRagent.entity.ChatSession;
import com.gm.riskaiRagent.mapper.ChatMessageMapper;
import com.gm.riskaiRagent.mapper.ChatSessionMapper;
import com.gm.riskaiRagent.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户会话服务，负责会话管理、历史消息读写和 RAG 问答落库。
 */
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final RagRagentService ragRagentService;
    private final ObjectMapper objectMapper;

    /**
     * 查询当前登录用户的会话列表，并补充每个会话的消息数量。
     */
    public List<ChatSession> listSessions() {
        Long userId = AuthContext.userId();
        List<ChatSession> sessions = chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdatedAt));
        for (ChatSession session : sessions) {
            Long count = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, session.getId()));
            session.setMessageCount(count == null ? 0 : count.intValue());
        }
        return sessions;
    }

    /**
     * 为当前用户创建新会话，未传标题时使用默认标题。
     */
    public ChatSession createSession(String title) {
        ChatSession session = new ChatSession();
        session.setUserId(AuthContext.userId());
        session.setTitle(StringUtils.hasText(title) ? title : "新对话");
        chatSessionMapper.insert(session);
        session.setMessageCount(0);
        return session;
    }

    /**
     * 删除当前用户自己的会话，先做归属校验避免越权。
     */
    public void deleteSession(Long id) {
        ChatSession session = requireOwnedSession(id);
        chatSessionMapper.deleteById(session.getId());
    }

    /**
     * 读取指定会话的历史消息，并反序列化每轮回答的引用片段。
     */
    public List<ChatMessageVO> listMessages(Long sessionId) {
        requireOwnedSession(sessionId);
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getId))
                .stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    /**
     * 用户聊天主流程：调用 RAG 问答、保存消息，并用首问自动命名新会话。
     */
    public RagentResponse chat(ChatRequest request) {
        ChatSession session = requireOwnedSession(request.getSessionId());
        RagentRequest ragentRequest = new RagentRequest();
        ragentRequest.setQuestion(request.getQuestion());
        ragentRequest.setIncludeReferences(request.isIncludeReferences());
        RagentResponse response = ragRagentService.ask(ragentRequest, request.getCategoryIds());

        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setQuestion(request.getQuestion());
        message.setAnswer(response.getAnswer());
        message.setReferenceDocs(toJson(response.getReferences()));
        message.setFromCache(response.isFromCache() ? 1 : 0);
        message.setDegraded(response.isDegraded() ? 1 : 0);
        message.setCostMs(response.getCostMs());
        chatMessageMapper.insert(message);

        if ("新对话".equals(session.getTitle())) {
            session.setTitle(truncate(request.getQuestion(), 30));
            chatSessionMapper.updateById(session);
        }

        return response;
    }

    /**
     * 会话归属校验，所有会话读写入口都必须先经过这里。
     */
    private ChatSession requireOwnedSession(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "sessionId 不能为空");
        }
        ChatSession session = chatSessionMapper.selectById(id);
        if (session == null || !AuthContext.userId().equals(session.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "会话不存在");
        }
        return session;
    }

    private ChatMessageVO toVo(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setQuestion(message.getQuestion());
        vo.setAnswer(message.getAnswer());
        vo.setReferences(readReferences(message.getReferenceDocs()));
        vo.setFromCache(message.getFromCache() != null && message.getFromCache() == 1);
        vo.setDegraded(message.getDegraded() != null && message.getDegraded() == 1);
        vo.setCostMs(message.getCostMs());
        return vo;
    }

    private List<ReferenceChunk> readReferences(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ReferenceChunk>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
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

    @lombok.Data
    public static class ChatMessageVO {
        private String question;
        private String answer;
        private List<ReferenceChunk> references;
        private boolean fromCache;
        private boolean degraded;
        private long costMs;
    }
}
