package com.xiuxian.exception;

/**
 * 鉴权相关异常（未登录 / 令牌失效 / 凭证错误），由全局异常处理返回 401
 */
public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
