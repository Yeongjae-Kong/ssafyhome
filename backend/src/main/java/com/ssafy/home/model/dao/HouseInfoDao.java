package com.ssafy.home.model.dao;

import com.ssafy.home.model.dto.HouseInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HouseInfoDao {
    List<HouseInfo> selectBySggCd(@Param("sggCd") String sggCd);
    List<HouseInfo> selectBySggCdAndUmdCd(@Param("sggCd") String sggCd, @Param("umdCd") String umdCd);
    HouseInfo selectById(@Param("aptSeq") String aptSeq);
    List<HouseInfo> searchByKeyword(@Param("q") String q);
    List<HouseInfo> searchByKeywordInSgg(@Param("sggCd") String sggCd, @Param("q") String q);
    List<HouseInfo> searchByKeywordInUmd(@Param("sggCd") String sggCd, @Param("umdCd") String umdCd, @Param("q") String q);
    List<HouseInfo> selectWithinBbox(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                     @Param("minLon") double minLon, @Param("maxLon") double maxLon);
}
