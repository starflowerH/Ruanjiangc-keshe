package com.demo.miniprogram.common;

/**
 * 统一响应状态码
 *
 * 设计思路：
 *   200     - 请求成功
 *   4xx     - 客户端错误（参数校验、权限不足等）
 *   5xx     - 服务端错误
 *   1xxxx   - 业务错误码（便于前端针对性处理）
 *
 * @author demo
 */
public interface ResultCode {

    /* ========== 通用状态码 ========== */

    /** 请求成功 */
    int SUCCESS = 200;

    /** 参数校验失败 */
    int PARAM_ERROR = 400;

    /** 未登录 / Token过期 */
    int UNAUTHORIZED = 401;

    /** 无权限访问 */
    int FORBIDDEN = 403;

    /** 资源不存在 */
    int NOT_FOUND = 404;

    /** 服务器内部错误 */
    int SERVER_ERROR = 500;

    /* ========== 业务错误码（1xxxx） ========== */

    /** 微信登录失败 */
    int WX_LOGIN_ERROR = 10001;

    /** 用户已存在 */
    int USER_ALREADY_EXISTS = 10002;

    /** 报名已提交 */
    int ALREADY_APPLIED = 10003;

    /** 审核状态异常（如重复审核） */
    int AUDIT_STATUS_ERROR = 10004;

    /** 组队失败（如没有已确认的志愿者） */
    int TEAM_BUILD_ERROR = 10005;

    /** 问答内容包含敏感词 */
    int CONTENT_SENSITIVE = 10006;

    /** 模板消息发送失败 */
    int TEMPLATE_MSG_ERROR = 10007;
}
