package com.gm.riskaiRagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gm.riskaiRagent.entity.SysCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识分类 Mapper，封装 sys_category 表的基础增删改查。
 */
@Mapper
public interface SysCategoryMapper extends BaseMapper<SysCategory> {
}
