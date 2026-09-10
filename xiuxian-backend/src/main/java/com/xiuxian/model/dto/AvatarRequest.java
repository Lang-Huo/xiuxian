package com.xiuxian.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 设置头像请求：code 为内置头像库编码
 */
public record AvatarRequest(
        @NotBlank(message = "请选择头像")
        String code
) {
}
