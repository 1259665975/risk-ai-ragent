package com.gm.riskaiRagent.controller.api.admin;

import com.gm.riskaiRagent.common.Result;
import com.gm.riskaiRagent.common.ResultCode;
import com.gm.riskaiRagent.dto.PageResult;
import com.gm.riskaiRagent.entity.SysDocument;
import com.gm.riskaiRagent.service.PortalDocumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 管理端文档接口，负责分类文档分页、上传入库和删除。
 */
@Tag(name = "AdminDocuments")
@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final PortalDocumentService portalDocumentService;

    @GetMapping
    public Result<PageResult<SysDocument>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.success(portalDocumentService.page(page, size, categoryId, keyword));
    }

    @PostMapping("/upload")
    public Result<SysDocument> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam("categoryId") Long categoryId) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error(ResultCode.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        return Result.success(portalDocumentService.upload(file, categoryId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        portalDocumentService.delete(id);
        return Result.success();
    }
}
