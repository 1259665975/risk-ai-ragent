package com.gm.riskaiRagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class A2ASendRequest {

    @NotBlank(message = "fromAgentId 不能为空")
    @Size(max = 100, message = "fromAgentId 长度不能超过 100")
    private String fromAgentId;

    @NotBlank(message = "toAgentId 不能为空")
    @Size(max = 100, message = "toAgentId 长度不能超过 100")
    private String toAgentId;

    @NotBlank(message = "message 不能为空")
    @Size(max = 4000, message = "message 长度不能超过 4000")
    private String message;
}
