package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交问题请求参数
 *
 * 业务流程：
 *   高中生浏览到某大学的某位学长学姐 → 点击"提问" → 填写问题内容 → 提交
 *   → 系统存库 + 微信模板消息推送给对应大学生
 *
 * @author demo
 */
@Data
public class QuestionDTO {

    /** 被提问的大学生ID（志愿者ID），论坛模式时为 null */
    private Integer targetVolunteerId;
    /** 帖子分类（论坛模式用） */
    private String category;

    /** 论坛帖关联大学；null 表示发布到全部高校，高中生可选具体大学，大学生由后端覆盖为本人大学 */
    private Integer universityId;

    /** 问题内容 */
    @NotBlank(message = "问题内容不能为空")
    private String questionContent;

    /** 帖子标题（论坛模式用） */
    private String title;
}
