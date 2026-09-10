package com.xiuxian.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宝物图鉴（静态配置）：储物法宝 / 丹药 / 法器 / 宝衣 / 功法
 */
@Data
@TableName("treasures")
public class Treasure {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 宝物编码（唯一，初始数据与掉落去重用） */
    private String code = "";

    /** 宝物名称 */
    private String name = "";

    /** 类型：EQUIP 装备 / PILL 丹药 / MATERIAL 材料 */
    private String type = "EQUIP";

    /** 装备槽：WEAPON / ARMOR / STORAGE / ART；非装备类为空 */
    private String slot;

    /** 品阶：凡品 / 灵品 / 宝品 / 仙品 / 神品 */
    private String rarity = "凡品";

    /** 装备后额外增加的背包格数 */
    private int bagBonus = 0;

    /** 装备后的经验加成百分比（10 表示 +10%） */
    private int expBonus = 0;

    /** 使用后恢复的气血值（丹药类） */
    private int hpRestore = 0;

    /** 掉落权重，0 表示不参与掉落 */
    private int dropWeight = 0;

    /** 宝物描述 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
