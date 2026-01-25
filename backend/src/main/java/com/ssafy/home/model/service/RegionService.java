package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.HouseInfo;
import com.ssafy.home.model.dto.ListResponse;
import com.ssafy.home.model.dto.RegionCodeResponse;

import java.util.List;
import java.util.Optional;

public interface RegionService {
    Optional<String> findDongCode(String sidoName, String gugunName, String dongName);
    List<String> listDongCodes(String sidoName, String gugunName);

    List<HouseInfo> getHouseInfos(String sidoName, String gugunName, String dongName);
    List<HouseInfo> getHouseInfosByGugun(String sidoName, String gugunName);

    // Thin-controller friendly methods
    RegionCodeResponse getRegionCode(String sidoName, String gugunName, String dongName);
    ListResponse<HouseInfo> getHouses(String sidoName, String gugunName, String dongName);
}
