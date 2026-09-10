package com.xiuxian.model.dto;

import com.xiuxian.util.PhoneUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 登录请求：使用手机号作为用户名 + 密码
 */
public record LoginRequest(
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = PhoneUtil.REGEX, message = PhoneUtil.MESSAGE)
        String username,

        @NotBlank String password
) {
}
