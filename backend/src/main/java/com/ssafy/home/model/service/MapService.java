package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.ListResponse;
import com.ssafy.home.model.dto.HouseInfo;

public interface MapService {
    ListResponse<HouseInfo> itemsInBbox(double minLat, double maxLat, double minLon, double maxLon);
}
