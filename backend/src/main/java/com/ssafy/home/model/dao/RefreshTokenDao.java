package com.ssafy.home.model.dao;

import com.ssafy.home.model.dto.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenDao {
    int insert(RefreshToken token);
    RefreshToken selectByToken(@Param("token") String token);
    int deleteByToken(@Param("token") String token);
    int deleteByUserId(@Param("userId") int userId);
}

