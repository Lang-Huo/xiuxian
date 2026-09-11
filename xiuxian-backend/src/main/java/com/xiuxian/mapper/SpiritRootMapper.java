package com.xiuxian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuxian.model.entity.SpiritRoot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SpiritRootMapper extends BaseMapper<SpiritRoot> {

    /** 加载全部灵根（启动时缓存到 Policy，避免每次查库） */
    @Select("SELECT id, code, name, multiplier, rarity, description, created_at FROM spirit_roots ORDER BY id")
    List<SpiritRoot> findAll();

    /** 按编码查 */
    default SpiritRoot selectByCode(String code) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SpiritRoot>().eq("code", code));
    }
}
