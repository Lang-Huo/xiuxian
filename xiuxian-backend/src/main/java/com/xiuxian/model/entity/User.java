package com.xiuxian.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 修仙者（用户成长主表）
 * MVP 阶段仅含：境界 / 经验 / 血量，不含灵根与宝物。
 */
@Data
@TableName("users")
public class User {

    /** 内部自增主键（不对外暴露语义） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 仙途编号：6 位数字，首位非 0，全站唯一，注册时分配，对外展示的用户ID */
    private String userNo = "";

    /** 登录用户名（唯一凭证，登录时使用） */
    private String username;

    /** 道号/昵称（展示用，可重复） */
    private String nickname = "道友";

    /** 头像编码（内置头像库，见 AvatarPolicy） */
    private String avatar = "";

    /** 登录密码（BCrypt 哈希，永不返回给前端） */
    private String password;

    /** 灵根（兼容旧字段，存中文名；新逻辑以 spiritRootCode 为准） */
    private String spiritRoot = "";

    /** 灵根编码（关联 spirit_roots.code；null 表示尚未测灵根） */
    private String spiritRootCode;

    /** 境界名称，如 凡人 / 练气 / 筑基 ... */
    private String realm = "凡人";

    /** 当前大境界内层数 1-9 */
    private int layer = 1;

    /** 累计经验 */
    private int exp = 0;

    /** 当前血量 */
    private int hp = 100;

    /** 血量上限 */
    private int maxHp = 100;

    /** 累计答对题数（用于称号/统计） */
    private int correctCount = 0;

    /** 累计答题数 */
    private int answerCount = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
