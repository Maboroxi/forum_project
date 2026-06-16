package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.dto.AiConversationMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiConversationMessageMapper extends BaseMapper<AiConversationMessage> {
}
