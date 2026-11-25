package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 论坛帖子精华设置（必填，避免空请求体被默认值误判）
 */
@Data
public class ForumFeaturedDTO {

    /** 是否设为精华 */
    @NotNull(message = "featured 不能为空")
    private Boolean featured;
}
