package com.demo.miniprogram.common;

import lombok.Getter;

/**
 * 业务异常 - 在 Service 层抛出，由全局异常处理器捕获
 *
 * 使用方式：
 *   throw new BusinessException(ResultCode.ALREADY_APPLIED, "您已提交过报名，请勿重复提交");
 *
 * @author demo
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.PARAM_ERROR;
    }
}
