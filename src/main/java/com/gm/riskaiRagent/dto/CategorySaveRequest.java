package com.gm.riskaiRagent.dto;

import lombok.Data;

/**
 * 知识分类保存请求体，承载新增或编辑分类时的表单字段。
 */
@Data
public class CategorySaveRequest {

    private String name;
    private String description;
}
