package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.ListResponse;
import com.ssafy.home.model.dto.HouseInfo;
import com.ssafy.home.model.dao.HouseInfoDao;
import com.ssafy.home.model.service.MapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MapServiceImpl implements MapService {
    private final HouseInfoDao houseInfoDao;

    public MapServiceImpl(HouseInfoDao houseInfoDao) {
        this.houseInfoDao = houseInfoDao;
    }

    @Override
    public ListResponse<HouseInfo> itemsInBbox(double minLat, double maxLat, double minLon, double maxLon) {
        List<HouseInfo> items = houseInfoDao.selectWithinBbox(minLat, maxLat, minLon, maxLon);
        return new ListResponse<>(items);
    }
}
