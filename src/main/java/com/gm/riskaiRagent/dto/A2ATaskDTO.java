package com.gm.riskaiRagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class A2ATaskDTO {
    private String taskId;
    private String fromAgentId;
    private String toAgentId;
    private String message;
    private String status;
    private String detailMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
