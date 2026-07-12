package com.gm.riskaiRagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("a2a_agent")
public class A2aAgent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String agentId;
    private String name;
    private String description;
    private String capabilities;
    private String endpoint;
    private String status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
