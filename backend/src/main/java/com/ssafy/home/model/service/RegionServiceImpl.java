package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.DongCode;
import com.ssafy.home.model.dto.HouseInfo;
import com.ssafy.home.model.dto.ListResponse;
import com.ssafy.home.model.dto.RegionCodeResponse;
import com.ssafy.home.model.dao.DongCodeDao;
import com.ssafy.home.model.dao.HouseInfoDao;
import com.ssafy.home.model.service.RegionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class RegionServiceImpl implements RegionService {

    private final DongCodeDao dongCodeDao;
    private final HouseInfoDao houseInfoDao;

    public RegionServiceImpl(DongCodeDao dongCodeDao,
                             HouseInfoDao houseInfoDao) {
        this.dongCodeDao = dongCodeDao;
        this.houseInfoDao = houseInfoDao;
    }

    @Override
    public Optional<String> findDongCode(String sidoName, String gugunName, String dongName) {
        DongCode d = dongCodeDao.selectBySidoGugunDong(sidoName, gugunName, dongName);
        return d == null ? java.util.Optional.empty() : java.util.Optional.ofNullable(d.getDongCode());
    }

    @Override
    public List<String> listDongCodes(String sidoName, String gugunName) {
        return dongCodeDao.selectBySidoGugun(sidoName, gugunName)
                .stream().map(DongCode::getDongCode).toList();
    }

    @Override
    public List<HouseInfo> getHouseInfos(String sidoName, String gugunName, String dongName) {
        String dongCode = findDongCode(sidoName, gugunName, dongName)
                .orElseThrow(() -> new NoSuchElementException("dong code not found"));
        String sgg = dongCode.substring(0, 5);
        String umd = dongCode.substring(5);
        return houseInfoDao.selectBySggCdAndUmdCd(sgg, umd);
    }

    @Override
    public List<HouseInfo> getHouseInfosByGugun(String sidoName, String gugunName) {
        // derive sig code from first dong under the gugun
        List<String> codes = listDongCodes(sidoName, gugunName);
        if (codes.isEmpty()) return List.of();
        String sig = codes.get(0).substring(0, 5);
        return houseInfoDao.selectBySggCd(sig);
    }

    @Override
    public RegionCodeResponse getRegionCode(String sidoName, String gugunName, String dongName) {
        RegionCodeResponse resp = new RegionCodeResponse();
        if (dongName != null && !dongName.isBlank()) {
            String code = findDongCode(sidoName, gugunName, dongName)
                    .orElseThrow(() -> new NoSuchElementException("dong code not found"));
            resp.setDongCode(code);
            resp.setSggCd(code.substring(0,5));
            resp.setUmdCd(code.substring(5));
            resp.setCount(1);
        } else {
            List<String> codes = listDongCodes(sidoName, gugunName);
            resp.setDongCodes(codes);
            resp.setCount(codes.size());
            if (!codes.isEmpty()) resp.setSggCd(codes.get(0).substring(0,5));
        }
        return resp;
    }

    @Override
    public ListResponse<HouseInfo> getHouses(String sidoName, String gugunName, String dongName) {
        List<HouseInfo> items = (dongName != null && !dongName.isBlank())
                ? getHouseInfos(sidoName, gugunName, dongName)
                : getHouseInfosByGugun(sidoName, gugunName);
        return new ListResponse<>(items);
    }

}
