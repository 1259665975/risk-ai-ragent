package com.gm.riskaiRagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gm.riskaiRagent.entity.SysDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识文档 Mapper，封装 sys_document 表的基础增删改查。
 */
@Mapper
public interface SysDocumentMapper extends BaseMapper<SysDocument> {
}
