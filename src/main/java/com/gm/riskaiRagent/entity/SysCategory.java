package com.gm.riskaiRagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识分类实体，用于给文档和检索范围做业务分组。
 */
@Data
@TableName("sys_category")
public class SysCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Integer documentCount;
}
