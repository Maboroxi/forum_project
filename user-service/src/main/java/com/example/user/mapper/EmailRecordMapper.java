package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.user.entity.dto.EmailRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailRecordMapper extends BaseMapper<EmailRecord> {
}
