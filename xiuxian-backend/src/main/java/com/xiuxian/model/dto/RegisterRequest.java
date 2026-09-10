package com.xiuxian.model.dto;

import com.xiuxian.util.PhoneUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求：用户名即手机号（唯一登录凭证），昵称用于展示（可重复）
 */
public record RegisterRequest(
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = PhoneUtil.REGEX, message = PhoneUtil.MESSAGE)
        String username,

        @NotBlank @Size(min = 1, max = 32) String nickname,

        @NotBlank @Size(min = 6, max = 64) String password
) {
}
