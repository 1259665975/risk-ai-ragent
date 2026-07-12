package com.gm.riskaiRagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 门户问答请求体，支持问题、分类过滤和引用返回控制。
 */
@Data
public class PortalRagentRequest {

    @NotBlank(message = "question 不能为空")
    private String question;

    private List<Long> categoryIds;

    private boolean includeReferences = true;
}
