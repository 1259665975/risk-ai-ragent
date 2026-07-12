package com.gm.riskaiRagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gm.riskaiRagent.common.BusinessException;
import com.gm.riskaiRagent.common.ResultCode;
import com.gm.riskaiRagent.dto.CategorySaveRequest;
import com.gm.riskaiRagent.entity.SysCategory;
import com.gm.riskaiRagent.entity.SysDocument;
import com.gm.riskaiRagent.mapper.SysCategoryMapper;
import com.gm.riskaiRagent.mapper.SysDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 知识分类服务，负责分类列表、新增、编辑、删除和存在性校验。
 */
@Service
@RequiredArgsConstructor
public class SysCategoryService {

    private final SysCategoryMapper sysCategoryMapper;
    private final SysDocumentMapper sysDocumentMapper;

    public List<SysCategory> listAll() {
        List<SysCategory> categories = sysCategoryMapper.selectList(new LambdaQueryWrapper<SysCategory>()
                .orderByDesc(SysCategory::getId));
        for (SysCategory category : categories) {
            Long count = sysDocumentMapper.selectCount(new LambdaQueryWrapper<SysDocument>()
                    .eq(SysDocument::getCategoryId, category.getId()));
            category.setDocumentCount(count == null ? 0 : count.intValue());
        }
        return categories;
    }

    public SysCategory create(CategorySaveRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分类名称不能为空");
        }
        SysCategory category = new SysCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        sysCategoryMapper.insert(category);
        category.setDocumentCount(0);
        return category;
    }

    public SysCategory update(Long id, CategorySaveRequest request) {
        SysCategory category = requireCategory(id);
        if (StringUtils.hasText(request.getName())) {
            category.setName(request.getName());
        }
        category.setDescription(request.getDescription());
        sysCategoryMapper.updateById(category);
        return category;
    }

    public void delete(Long id) {
        requireCategory(id);
        Long count = sysDocumentMapper.selectCount(new LambdaQueryWrapper<SysDocument>()
                .eq(SysDocument::getCategoryId, id));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分类下仍有文档，无法删除");
        }
        sysCategoryMapper.deleteById(id);
    }

    public SysCategory requireCategory(Long id) {
        SysCategory category = sysCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return category;
    }
}
