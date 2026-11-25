package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("forum_sensitive_word")
public class ForumSensitiveWord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String word;
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
