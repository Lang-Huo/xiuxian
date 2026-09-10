package com.xiuxian.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuxian.model.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /** 按主题查询题目（使用 MP 自动结果映射，确保 options JSON 正确反序列化） */
    default List<Question> findByTopicId(Long topicId) {
        return selectList(new QueryWrapper<Question>().eq("topic_id", topicId).orderByAsc("id"));
    }

    /**
     * 统计某用户累计作答过的题目数（经 topics 关联）
     * 用参数化 @Select，避免在 SQL 片段里拼接 userId
     */
    @Select("select count(*) from questions q inner join topics t on q.topic_id = t.id where t.user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);
}
