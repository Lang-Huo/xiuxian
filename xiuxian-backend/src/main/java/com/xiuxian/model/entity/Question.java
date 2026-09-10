package com.xiuxian.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 题目与解析
 * type: JUDGE(判断题) / SINGLE(单选题)
 * answer: 判断题为 "true"/"false"；单选题为选项序号 "A"/"B"/"C"/"D"
 */
@Data
@TableName("questions")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long topicId;

    @TableField("type")
    private String type = "SINGLE";

    /** 题干 */
    private String stem;

    /** 选项（判断题为 [正确, 错误]），以 JSON 文本存储，由 JacksonTypeHandler 序列化/反序列化 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> options = new ArrayList<>();

    /** 正确答案 */
    private String answer;

    /** 解析（无论对错都展示） */
    private String explanation;

    /** 知识点（用于复习本/统计） */
    private String knowledgePoint;

    /** 知识来源链接（可溯源，降低幻觉） */
    private String sourceUrl;

    /** 难度：入门 / 进阶 / 精通 */
    private String difficulty = "入门";
}
