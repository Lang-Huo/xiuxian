package com.xiuxian.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习主题：用户想要修炼的知识领域
 */
@Data
@TableName("topics")
public class Topic {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户（MVP 默认单用户，预留字段） */
    private Long userId = 1L;

    /** 用户自由输入的主题 */
    private String keyword;

    /** 难度：入门 / 进阶 / 精通 */
    private String difficulty = "入门";

    /** 生成题目数量 */
    private int count = 5;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
