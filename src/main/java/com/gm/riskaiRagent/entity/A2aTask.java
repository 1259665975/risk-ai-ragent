package com.gm.riskaiRagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("a2a_task")
public class A2aTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String fromAgentId;
    private String toAgentId;
    private String message;
    private String status;
    private String detailMessage;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
