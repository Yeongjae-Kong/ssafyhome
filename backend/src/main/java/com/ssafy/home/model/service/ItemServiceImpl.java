package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.HouseInfo;
import com.ssafy.home.model.dao.HouseInfoDao;
import com.ssafy.home.model.service.ItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final HouseInfoDao houseInfoDao;

    public ItemServiceImpl(HouseInfoDao houseInfoDao) {
        this.houseInfoDao = houseInfoDao;
    }

    @Override
    public HouseInfo getItem(String aptSeq) {
        return houseInfoDao.selectById(aptSeq);
    }
}
