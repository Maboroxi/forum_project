package com.example.user.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("db_email_record")
public class EmailRecord {
    @TableId(type = IdType.AUTO)
    Integer id;
    String email;
    String title;
    String content;
    Date time;
    Integer status; // 0=sending, 1=success, 2=failure

    public EmailRecord(String email, String title, String content) {
        this.email = email;
        this.title = title;
        this.content = content;
        this.status = 0;
        this.time = new Date();
    }
}
