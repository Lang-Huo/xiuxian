package com.xiuxian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuxian.model.entity.Treasure;
import com.xiuxian.model.entity.UserTreasure;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserTreasureMapper extends BaseMapper<UserTreasure> {

    /** 用户背包全部条目（已装备的也在其中，仍占一格） */
    @Select("SELECT id, user_id, treasure_id, `count`, equipped, created_at, updated_at "
            + "FROM user_treasures WHERE user_id = #{userId} ORDER BY id")
    List<UserTreasure> findByUserId(@Param("userId") Long userId);

    /** 用户已装备的宝物图鉴（用于计算加成与槽位冲突） */
    @Select("SELECT t.* FROM user_treasures ut "
            + "INNER JOIN treasures t ON t.id = ut.treasure_id "
            + "WHERE ut.user_id = #{userId} AND ut.equipped = 1")
    List<Treasure> findEquippedTreasures(@Param("userId") Long userId);

    /** 查某一格（用户 + 宝物唯一） */
    default UserTreasure findOne(Long userId, Long treasureId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserTreasure>()
                .eq("user_id", userId)
                .eq("treasure_id", treasureId));
    }
}
