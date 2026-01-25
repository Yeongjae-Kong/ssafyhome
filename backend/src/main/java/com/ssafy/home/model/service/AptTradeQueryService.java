package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.AptTradeRecord;
import com.ssafy.home.model.dto.ListResponse;

public interface AptTradeQueryService {
    ListResponse<AptTradeRecord> queryBySgg(String sggCd, String yyyymm, Integer page, Integer size);
}

