package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.HouseDeal;
import com.ssafy.home.model.dto.ListResponse;

public interface LiveTradeService {
    ListResponse<HouseDeal> regionDeals(String sido, String gugun, String dong, String yyyymm, Integer page, Integer size);
    ListResponse<HouseDeal> apartmentDeals(String aptSeq, String yyyymm, Integer page, Integer size);
}

