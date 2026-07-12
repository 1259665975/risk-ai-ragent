package com.gm.riskaiRagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class A2AConnectivityDTO {
    private String agentId;
    private String endpoint;
    private boolean connected;
    private String message;
    private LocalDateTime checkedAt;
}
