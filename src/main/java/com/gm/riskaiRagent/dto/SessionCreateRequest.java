package com.gm.riskaiRagent.dto;

import lombok.Data;

/**
 * 会话创建请求体，允许前端传入会话标题。
 */
@Data
public class SessionCreateRequest {

    private String title;
}
