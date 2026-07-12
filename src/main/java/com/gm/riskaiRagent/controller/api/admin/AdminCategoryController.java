package com.gm.riskaiRagent.controller.api.admin;

import com.gm.riskaiRagent.common.Result;
import com.gm.riskaiRagent.dto.CategorySaveRequest;
import com.gm.riskaiRagent.entity.SysCategory;
import com.gm.riskaiRagent.service.SysCategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端知识分类接口，提供分类列表、新增、编辑和删除能力。
 */
@Tag(name = "AdminCategories")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final SysCategoryService sysCategoryService;

    @GetMapping
    public Result<List<SysCategory>> list() {
        return Result.success(sysCategoryService.listAll());
    }

    @PostMapping
    public Result<SysCategory> create(@RequestBody CategorySaveRequest request) {
        return Result.success(sysCategoryService.create(request));
    }

    @PutMapping("/{id}")
    public Result<SysCategory> update(@PathVariable Long id, @RequestBody CategorySaveRequest request) {
        return Result.success(sysCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysCategoryService.delete(id);
        return Result.success();
    }
}
