package com.gm.riskaiRagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class A2AStatusDTO {
    private String taskId;
    private String status;
    private String message;
    private LocalDateTime updatedAt;
}
