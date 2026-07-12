package com.gm.riskaiRagent.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户聊天请求体，包含会话 ID、问题、分类过滤和引用返回开关。
 */
@Data
public class ChatRequest {

    private Long sessionId;

    @jakarta.validation.constraints.NotBlank(message = "question 不能为空")
    private String question;

    private List<Long> categoryIds;

    private boolean includeReferences = true;
}
