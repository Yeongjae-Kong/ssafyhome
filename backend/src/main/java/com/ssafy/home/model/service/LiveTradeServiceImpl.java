package com.ssafy.home.model.service;

import com.ssafy.home.external.molit.MolitAptTradeClient;
import com.ssafy.home.model.dao.DongCodeDao;
import com.ssafy.home.model.dao.HouseInfoDao;
import com.ssafy.home.model.dto.DongCode;
import com.ssafy.home.model.dto.HouseDeal;
import com.ssafy.home.model.dto.HouseInfo;
import com.ssafy.home.model.dto.ListResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LiveTradeServiceImpl implements LiveTradeService {

    private final MolitAptTradeClient client;
    private final DongCodeDao dongCodeDao;
    private final HouseInfoDao houseInfoDao;

    public LiveTradeServiceImpl(MolitAptTradeClient client, DongCodeDao dongCodeDao, HouseInfoDao houseInfoDao) {
        this.client = client;
        this.dongCodeDao = dongCodeDao;
        this.houseInfoDao = houseInfoDao;
    }

    @Override
    public ListResponse<HouseDeal> regionDeals(String sido, String gugun, String dong, String yyyymm, Integer page, Integer size) {
        String sgg = resolveSggCd(sido, gugun, dong);
        List<MolitAptTradeClient.Record> rows = client.fetch(sgg, yyyymm, page, size);
        List<HouseDeal> items = mapToHouseDeals(rows);
        return new ListResponse<>(items);
    }

    @Override
    public ListResponse<HouseDeal> apartmentDeals(String aptSeq, String yyyymm, Integer page, Integer size) {
        HouseInfo info = houseInfoDao.selectById(aptSeq);
        if (info == null) return new ListResponse<>(List.of());
        String sgg = info.getSggCd();
        String aptNm = info.getAptNm();
        List<MolitAptTradeClient.Record> rows = client.fetch(sgg, yyyymm, page, size);
        List<HouseDeal> items = new ArrayList<>();
        for (MolitAptTradeClient.Record r : rows) {
            if (matchesAptName(aptNm, r.getAptName())) {
                items.add(mapToHouseDeal(r));
            }
        }
        return new ListResponse<>(items);
    }

    private String resolveSggCd(String sido, String gugun, String dong) {
        if (dong != null && !dong.isBlank()) {
            DongCode d = dongCodeDao.selectBySidoGugunDong(sido, gugun, dong);
            if (d != null && d.getDongCode() != null && d.getDongCode().length() >= 5) {
                return d.getDongCode().substring(0,5);
            }
        }
        List<DongCode> list = dongCodeDao.selectBySidoGugun(sido, gugun);
        if (!list.isEmpty() && list.get(0).getDongCode() != null && list.get(0).getDongCode().length() >= 5) {
            return list.get(0).getDongCode().substring(0,5);
        }
        throw new IllegalArgumentException("cannot resolve sgg code");
    }

    private static List<HouseDeal> mapToHouseDeals(List<MolitAptTradeClient.Record> rows) {
        List<HouseDeal> items = new ArrayList<>();
        for (MolitAptTradeClient.Record r : rows) items.add(mapToHouseDeal(r));
        return items;
    }

    private static HouseDeal mapToHouseDeal(MolitAptTradeClient.Record r) {
        HouseDeal h = new HouseDeal();
        h.setDealAmount(r.getDealAmount());
        h.setAptName(r.getAptName());
        h.setDealYear(parseInt(r.getYear()));
        h.setDealMonth(parseInt(r.getMonth()));
        h.setDealDay(parseInt(r.getDay()));
        h.setFloor(r.getFloor());
        h.setAptDong(r.getDong());
        try {
            if (r.getExclusiveArea() != null) h.setExcluUseAr(new BigDecimal(r.getExclusiveArea()));
        } catch (Exception ignored) {}
        return h;
    }

    private static int parseInt(String s) {
        try { return s == null ? 0 : Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    // 아파트명 정규화 매칭: 공백/특수문자 제거, 괄호내 삭제, 대소문자 무시, 부분포함 허용
    private static boolean matchesAptName(String dbName, String apiName) {
        if (dbName == null || apiName == null) return false;
        String a = normalizeName(dbName);
        String b = normalizeName(apiName);
        if (a.isEmpty() || b.isEmpty()) return false;
        if (a.equals(b)) return true;
        int min = Math.min(a.length(), b.length());
        int max = Math.max(a.length(), b.length());
        boolean similar = (min * 100 / Math.max(1, max)) >= 70;
        return similar && (a.contains(b) || b.contains(a));
    }

    private static String normalizeName(String s) {
        String x = s;
        // 괄호 내 제거
        x = x.replaceAll("\\(.*?\\)", "");
        // 공백/특수문자 제거
        x = x.replaceAll("[^0-9A-Za-z가-힣]", "");
        // 대소문자 무시
        x = x.toUpperCase();
        return x.trim();
    }
}

