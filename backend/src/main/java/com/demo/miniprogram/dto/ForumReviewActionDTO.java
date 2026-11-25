package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 论坛帖子审核动作（通过/拒绝）
 */
@Data
public class ForumReviewActionDTO {

    /** 审核动作: APPROVED / REJECTED */
    @NotBlank(message = "审核动作不能为空")
    @Size(max = 20)
    private String action;
}
