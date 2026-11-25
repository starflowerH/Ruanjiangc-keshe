package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 屏蔽词新增请求
 */
@Data
public class SensitiveWordDTO {

    @NotBlank(message = "屏蔽词不能为空")
    @Size(max = 100, message = "屏蔽词长度应为1至100字")
    private String word;
}
