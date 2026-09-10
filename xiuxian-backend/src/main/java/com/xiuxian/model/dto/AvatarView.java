package com.xiuxian.model.dto;

/**
 * 头像（内置头像库中的一款）
 */
public record AvatarView(
        String code,     // 头像编码，如 XIAN
        String name,     // 意境名，如 仙缘
        String glyph,    // 头像上的单字，如 仙
        String fg,       // 字色
        String bg        // 底色
) {
}
