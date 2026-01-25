package com.ssafy.home.model.dao;

import com.ssafy.home.model.dto.FavoriteApartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteDao {
    List<FavoriteApartment> selectByUserId(@Param("userId") int userId);
    FavoriteApartment selectByUnique(@Param("userId") int userId, @Param("aptSeq") String aptSeq);
    int insert(FavoriteApartment item);
    int deleteByIdAndUserId(@Param("id") int id, @Param("userId") int userId);
    int updateLastDeal(@Param("id") int id, @Param("amount") Long amount, @Param("yyyymm") String yyyymm);
}