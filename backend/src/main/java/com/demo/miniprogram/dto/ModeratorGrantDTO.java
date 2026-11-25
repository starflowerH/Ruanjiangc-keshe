package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 论坛审核员授权请求（必填 active，防止空请求体被默认 true 误授权）
 */
@Data
public class ModeratorGrantDTO {

    /** 是否授予审核员权限 */
    @NotNull(message = "active 不能为空")
    private Boolean active;
}
