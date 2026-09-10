package com.xiuxian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuxian.model.entity.Treasure;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TreasureMapper extends BaseMapper<Treasure> {

    /** 全部可掉落宝物（drop_weight > 0），用于答题掉落按权重抽取 */
    @Select("SELECT * FROM treasures WHERE drop_weight > 0")
    List<Treasure> findDroppable();

    /** 按编码查图鉴（发放/初始化用） */
    default Treasure selectByCode(String code) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Treasure>().eq("code", code));
    }
}
