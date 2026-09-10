package com.xiuxian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuxian.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 按登录用户名查询用户 */
    default User selectByUsername(String username) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("username", username));
    }

    /** 按仙途编号查询用户（注册分配编号时用于查重） */
    default User selectByUserNo(String userNo) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("user_no", userNo));
    }

    /** 按道号/昵称查询用户（昵称可重复，谨慎使用） */
    default User selectByNickname(String nickname) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("nickname", nickname));
    }
}
