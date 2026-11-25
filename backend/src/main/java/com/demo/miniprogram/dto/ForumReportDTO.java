package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForumReportDTO {
    @NotNull(message = "帖子ID不能为空")
    private Integer postId;
    /** 举报对象：post(帖子) / comment(评论)，默认 post */
    @Size(max = 20, message = "举报对象类型过长")
    private String targetType;
    /** 被举报评论ID（targetType=comment 时必填） */
    private Integer commentId;
    @NotBlank(message = "请选择举报原因")
    @Size(max = 50, message = "举报原因过长")
    private String reason;
    @Size(max = 500, message = "补充说明不能超过500字")
    private String detail;
}
