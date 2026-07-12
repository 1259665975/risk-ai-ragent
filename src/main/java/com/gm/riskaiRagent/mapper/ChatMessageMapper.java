package com.gm.riskaiRagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gm.riskaiRagent.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息 Mapper，封装 chat_message 表的基础增删改查。
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
