package com.demo.miniprogram.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体 - 所有 API 接口都使用此类包装返回
 *
 * 使用示例：
 *   Result.ok(data)           → {"code":200, "message":"success", "data":{...}}
 *   Result.fail("参数错误")    → {"code":400, "message":"参数错误", "data":null}
 *   Result.fail(10001, "微信登录失败")
 *
 * @param <T> data 的泛型类型
 * @author demo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 状态码，对应 ResultCode 中的常量 */
    private int code;

    /** 提示信息（给前端展示或调试用） */
    private String message;

    /** 响应数据，可为 null */
    private T data;

    /* ========== 便捷静态工厂方法 ========== */

    /** 成功（无数据） */
    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.SUCCESS, "success", null);
    }

    /** 成功（有数据） */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS, "success", data);
    }

    /** 成功（自定义消息） */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(ResultCode.SUCCESS, message, data);
    }

    /** 失败（默认错误码 400） */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.PARAM_ERROR, message, null);
    }

    /** 失败（自定义错误码 + 消息） */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
