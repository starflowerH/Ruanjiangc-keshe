package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核操作请求参数（通用）
 *
 * 适用场景：
 *   - 志愿者报名审核：老师确认/拒绝志愿者
 *   - 问答内容审核：管理员通过/拒绝问答公开
 *
 * @author demo
 */
@Data
public class AuditDTO {

    /** 待审核记录的ID */
    @NotNull(message = "记录ID不能为空")
    private Integer id;

    /** 审核结果：APPROVED=通过, REJECTED=拒绝 */
    @NotBlank(message = "审核结果不能为空")
    private String status;

    /** 审核备注（拒绝时建议填写原因） */
    private String remark;
}
