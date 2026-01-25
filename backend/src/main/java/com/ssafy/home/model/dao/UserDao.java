package com.ssafy.home.model.dao;

import com.ssafy.home.model.dto.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDao {
    User selectByEmail(@Param("email") String email);
    User selectById(@Param("mno") int mno);
    int insert(User user);
}
