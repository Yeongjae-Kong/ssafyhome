package com.ssafy.home.model.dao;

import com.ssafy.home.model.dto.DongCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DongCodeDao {
    DongCode selectBySidoGugunDong(@Param("sido") String sido, @Param("gugun") String gugun, @Param("dong") String dong);
    List<DongCode> selectBySidoGugun(@Param("sido") String sido, @Param("gugun") String gugun);
    DongCode selectByCode(@Param("code") String code);
    List<DongCode> selectByDongNameLike(@Param("q") String q);
}
