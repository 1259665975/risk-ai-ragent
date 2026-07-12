package com.gm.riskaiRagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gm.riskaiRagent.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天会话 Mapper，封装 chat_session 表的基础增删改查。
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
