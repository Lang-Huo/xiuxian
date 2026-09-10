package com.xiuxian.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户背包中的一条宝物（按 treasure_id 堆叠，同种只占一格）
 */
@Data
@TableName("user_treasures")
public class UserTreasure {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long treasureId;

    /** 持有数量 */
    private int count = 1;

    /** 是否已装备：0 未装备 / 1 已装备 */
    private int equipped = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
