package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 申诉复核请求（必填 approved，防止空请求体被默认 false 误判为驳回）
 */
@Data
public class ForumAppealReviewDTO {

    /** 申诉是否通过 */
    @NotNull(message = "approved 不能为空")
    private Boolean approved;

    /** 是否恢复帖子公开 */
    private Boolean restorePost;

    /** 复核备注 */
    @Size(max = 500)
    private String note;
}
