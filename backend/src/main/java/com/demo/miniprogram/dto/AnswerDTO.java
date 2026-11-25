package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 回答问题请求参数
 *
 * 业务流程：
 *   大学生收到模板消息通知 → 打开答疑页面 → 填写回答 → 提交
 *   → 系统存库 + 微信模板消息推送给对应高中生
 *
 * @author demo
 */
@Data
public class AnswerDTO {

    /** 问答记录ID */
    @NotNull(message = "问答记录ID不能为空")
    private Integer questionId;

    /** 回答内容 */
    @NotBlank(message = "回答内容不能为空")
    private String answerContent;
}
