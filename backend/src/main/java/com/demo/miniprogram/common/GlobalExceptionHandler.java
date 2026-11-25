package com.demo.miniprogram.common;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器 - 统一拦截各类异常，返回规范的 Result 响应
 *
 * 处理优先级：
 *   1. 业务异常 BusinessException → 返回对应的业务错误码
 *   2. 参数校验异常 MethodArgumentNotValidException → 返回 400 + 校验失败字段
 *   3. 兜底异常 Exception → 返回 500 + 日志记录
 *
 * @author demo
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常 - Service 层主动抛出的异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常 - @Valid 注解触发的校验失败
     * 提取第一个字段的校验错误信息返回
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", errorMsg);
        return Result.fail(ResultCode.PARAM_ERROR, errorMsg);
    }

    /**
     * 处理参数类型不匹配 - 路径/查询参数传了非数字(如 /api/university/undefined)
     * 返回 400 而不是 500，避免用户看到"服务异常"
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, TypeMismatchException.class})
    public Result<?> handleTypeMismatch(Exception e) {
        log.warn("参数类型不匹配: {}", e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR, "请求参数格式不正确");
    }

    /**
     * 处理缺少必填参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数: {}", e.getParameterName());
        return Result.fail(ResultCode.PARAM_ERROR, "缺少参数: " + e.getParameterName());
    }

    /**
     * 处理未匹配路由（NoResourceFoundException）
     * 未命中任何 Controller 的路径属于正常 404（如探测性请求），不应当作 ERROR 上报 Sentry
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResource(NoResourceFoundException e) {
        log.warn("未匹配路由: {}", e.getResourcePath());
        return Result.fail(ResultCode.NOT_FOUND, "接口不存在");
    }

    /**
     * 兜底异常处理 - 捕获所有未被处理的异常
     * 返回 500 并记录完整堆栈，避免敏感信息泄露给前端
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        Sentry.captureException(e);
        return Result.fail(ResultCode.SERVER_ERROR, "服务器繁忙，请稍后重试");
    }
}
