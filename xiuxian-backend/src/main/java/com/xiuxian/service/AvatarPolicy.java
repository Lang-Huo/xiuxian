package com.xiuxian.service;

import com.xiuxian.model.dto.AvatarView;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置头像库：12 款水墨修仙风头像（单字 + 淡色底 + 墨/朱砂字），外加一款「本名」。
 *
 * 默认头像 = 昵称首字（{@link #AUTO_CODE}），未设置或编码非法时自动回退到它，
 * 因此 {@link #of(String, String)} 需要昵称，且绝不返回 null。
 *
 * 新增预设头像只需往 AVATARS 里加一行，前后端自动生效，无需改表结构。
 */
public final class AvatarPolicy {

    /** 「本名」头像编码：头像字取昵称第一个字 */
    public static final String AUTO_CODE = "AUTO";

    /** 全部可选预设头像 */
    public static final List<AvatarView> AVATARS = List.of(
            new AvatarView("XIAN", "仙缘", "仙", "#9e3b34", "#f7ece9"),
            new AvatarView("DAO", "问道", "道", "#1f1d1a", "#eceae6"),
            new AvatarView("JIAN", "剑心", "剑", "#3f6a9e", "#e9eff6"),
            new AvatarView("DAN", "丹火", "丹", "#8a312b", "#f7e9e6"),
            new AvatarView("FU", "符箓", "符", "#7a5aa0", "#f0ebf6"),
            new AvatarView("HE", "孤鹤", "鹤", "#4a7c6f", "#e7f1ee"),
            new AvatarView("SONG", "苍松", "松", "#3f6b4f", "#e8f0ea"),
            new AvatarView("YUN", "闲云", "云", "#5b6b7a", "#eaedf0"),
            new AvatarView("YUE", "明月", "月", "#4a5a8a", "#ebedf4"),
            new AvatarView("LIAN", "青莲", "莲", "#a05a86", "#f7ecf3"),
            new AvatarView("ZHU", "翠竹", "竹", "#4f7a3a", "#ecf2e8"),
            new AvatarView("JIU", "醉仙", "酒", "#8a5a2b", "#f5eee6")
    );

    /** 「本名」头像的兜底字（昵称为空时用） */
    private static final String FALLBACK_GLYPH = "仙";

    private AvatarPolicy() {
    }

    /**
     * 「本名」头像：头像字取昵称第一个字。
     * 昵称改动后头像会自动跟着变，无需用户重新设置。
     */
    public static AvatarView auto(String nickname) {
        return new AvatarView(AUTO_CODE, "本名", firstChar(nickname), "#9e3b34", "#f7ece9");
    }

    /** 头像列表（供「换头像」面板使用），首项为「本名」 */
    public static List<AvatarView> all(String nickname) {
        List<AvatarView> list = new ArrayList<>();
        list.add(auto(nickname));
        list.addAll(AVATARS);
        return list;
    }

    /**
     * 解析头像：编码有效取对应预设，为空/非法则返回「本名」（昵称首字）。
     * 绝不返回 null，前端无需判空。
     */
    public static AvatarView of(String code, String nickname) {
        if (code != null && !code.isBlank() && !AUTO_CODE.equalsIgnoreCase(code.trim())) {
            String target = code.trim();
            for (AvatarView a : AVATARS) {
                if (a.code().equalsIgnoreCase(target)) {
                    return a;
                }
            }
        }
        return auto(nickname);
    }

    /** 是否为可接受头像编码（预设头像或「本名」） */
    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        if (AUTO_CODE.equalsIgnoreCase(code.trim())) {
            return true;
        }
        String target = code.trim();
        return AVATARS.stream().anyMatch(a -> a.code().equalsIgnoreCase(target));
    }

    /** 取昵称第一个字（按 Unicode 码点，兼容生僻字与 emoji）；空昵称回退为「仙」 */
    private static String firstChar(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return FALLBACK_GLYPH;
        }
        String s = nickname.trim();
        return s.substring(0, s.offsetByCodePoints(0, 1));
    }
}
