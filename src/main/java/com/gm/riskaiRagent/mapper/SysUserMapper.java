package com.gm.riskaiRagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gm.riskaiRagent.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper，封装 sys_user 表的基础增删改查。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
