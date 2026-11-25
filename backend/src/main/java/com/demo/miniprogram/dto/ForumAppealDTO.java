package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForumAppealDTO {
    @NotNull(message = "处罚ID不能为空")
    private Integer penaltyId;
    @NotBlank(message = "请填写申诉理由")
    @Size(max = 500, message = "申诉理由不能超过500字")
    private String reason;
}
