package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 问答明细表 - 高中生与大学生之间的一对一问答
 *
 * 业务流程（对应业务文档阶段5-线上答疑期 + 准实时问答机制）：
 *   【提问】高中生浏览学长学姐列表 → 点击某人 → 提交问题
 *          → 系统存库 + 微信模板消息推送给对应大学生
 *   【回答】大学生收到模板消息 → 打开H5答疑页面 → 回答问题
 *          → 系统存库 + 微信模板消息推送给高中生
 *   【审核】后台管理员审核问答内容 → 筛选优质问答 → 公开到高中生平台
 *
 * 注意：表名 "Youtube" 是数据库中的命名，实际业务是问答系统
 *
 * @author demo
 */
@Data
@TableName("Youtube")
public class QuestionAnswer {

    /** 问答ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 提问的高中生ID，关联 high_school_student 表（大学生提问时为 null） */
    private Integer askerStudentId;

    /** 提问的大学生ID，关联 volunteer 表（高中生提问时为 null） */
    private Integer askerVolunteerId;

    /** 被提问的大学生ID（志愿者），关联 volunteer 表（论坛模式时为 null） */
    private Integer targetVolunteerId;

    /** 论坛帖关联大学；null 表示全部高校，大学生发帖时自动取本人大学 */
    private Integer universityId;

    /** 发帖身份: student / volunteer */
    private String authorType;

    /** 帖子分类: admission(报考咨询) / major(专业选择) / campus(校园生活) / study(学习方法) / other(其他) */
    private String category;

    /** 帖子标题（论坛模式用，一对一提问时为 null） */
    private String title;

    /** 提问内容（文本） */
    private String questionContent;

    /** 回答内容（文本），未回答时为 NULL */
    private String answerContent;

    /** 提问时间，数据库自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime askedAt;

    /** 回答时间，大学生提交回答时更新 */
    private LocalDateTime answeredAt;

    /**
     * 审核状态
     * PENDING  - 待审核（默认）
     * APPROVED - 审核通过（可公开显示）
     * REJECTED - 审核拒绝（违规内容）
     */
    private String auditStatus;

    /**
     * 是否公开为精选问答
     * 0 = 私密（仅提问者和回答者可见）
     * 1 = 公开（在"精选问答"板块展示给所有高中生）
     */
    private Integer isPublic;

    /** 是否精华帖 */
    private Integer isFeatured;

    /** 大学生审核员ID */
    private Integer reviewedByVolunteerId;

    /** 管理员审核人ID */
    private Integer reviewedByAdminId;

    /** 审核时间 */
    private LocalDateTime reviewedAt;

    /** 精华设置人类型: admin/volunteer */
    private String featuredByType;

    /** 精华设置人ID */
    private Integer featuredById;

    /** 精华设置时间 */
    private LocalDateTime featuredAt;

    /** 浏览次数，默认0 */
    private Integer viewCount;
}
