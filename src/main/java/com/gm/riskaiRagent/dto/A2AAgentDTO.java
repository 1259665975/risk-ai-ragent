package com.gm.riskaiRagent.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class A2AAgentDTO {
    private String agentId;
    private String name;
    private String description;
    private List<String> capabilities;
    private String endpoint;
    private String status;
    private LocalDateTime registeredAt;
}
