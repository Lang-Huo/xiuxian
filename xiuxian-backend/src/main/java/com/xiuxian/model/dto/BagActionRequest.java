package com.xiuxian.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 背包操作请求：itemId 为背包记录ID（user_treasures.id），count 仅丢弃时使用（默认 1）
 */
public record BagActionRequest(
        @NotNull(message = "请选择宝物")
        Long itemId,

        Integer count
) {
}
