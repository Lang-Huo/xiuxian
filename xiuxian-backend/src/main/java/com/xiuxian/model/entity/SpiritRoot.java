package com.xiuxian.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 灵根配置表（静态配置）：name / multiplier / rarity / description 全部走这一张表，
 * 用户通过 spirit_root_code 关联，不再用中文名作业务关联。
 */
@Data
@TableName("spirit_roots")
public class SpiritRoot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 灵根编码（FIVE/DOUBLE/SINGLE/VARIANT/SKY/CHAOS，程序用） */
    private String code = "";

    /** 灵根名（前端展示） */
    private String name = "";

    /** 道行倍率（答题经验乘此值） */
    private BigDecimal multiplier = BigDecimal.ONE;

    /** 稀有度 */
    private String rarity = "常见";

    /** 描述 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
