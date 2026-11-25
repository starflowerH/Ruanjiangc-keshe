package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 举报处理请求（必填 founded，防止空请求体被默认 false 误判为"不成立"）
 */
@Data
public class ForumReportReviewDTO {

    /** 举报是否成立 */
    @NotNull(message = "founded 不能为空")
    private Boolean founded;

    /** 处理备注 */
    @Size(max = 500)
    private String note;
}
