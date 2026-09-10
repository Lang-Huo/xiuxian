package com.xiuxian.model.dto;

/**
 * 注册/登录返回：令牌 + 当前修仙者状态
 */
public record AuthResponse(
        String token,
        UserStateView user
) {
}
