package com.xiuxian.model.dto;

/**
 * 背包操作结果：提示语 + 操作后的最新背包（前端直接整体刷新，避免本地状态不一致）
 */
public record BagActionResult(
        String message,
        BagView bag
) {
}
