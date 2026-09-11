package com.xiuxian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuxian.model.entity.Topic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TopicMapper extends BaseMapper<Topic> {

    /**
     * 统计某用户修炼过的主题数（排除测灵根占用的主题记录，详见 SpiritRootPolicy#TEST_TOPIC_PREFIX）
     */
    @Select("select count(*) from topics where user_id = #{userId} and keyword not like '【测灵根】%'")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 最近修炼的主题（倒序取前 N 条；排除测灵根）
     * Topic 无 JSON typeHandler 字段，自动映射即可（依赖 map-underscore-to-camel-case）
     */
    @Select("select id, user_id, keyword, difficulty, `count`, created_at " +
            "from topics where user_id = #{userId} and keyword not like '【测灵根】%' " +
            "order by id desc limit #{limit}")
    List<Topic> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}
