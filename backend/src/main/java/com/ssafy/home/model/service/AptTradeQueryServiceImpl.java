package com.ssafy.home.model.service;

import com.ssafy.home.external.molit.MolitAptTradeClient;
import com.ssafy.home.model.dto.AptTradeRecord;
import com.ssafy.home.model.dto.ListResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AptTradeQueryServiceImpl implements AptTradeQueryService {

    private final MolitAptTradeClient client;

    public AptTradeQueryServiceImpl(MolitAptTradeClient client) {
        this.client = client;
    }

    @Override
    public ListResponse<AptTradeRecord> queryBySgg(String sggCd, String yyyymm, Integer page, Integer size) {
        int p = page == null ? 1 : Math.max(1, page);
        int s = size == null ? 100 : Math.max(1, size);
        List<MolitAptTradeClient.Record> items = client.fetch(sggCd, yyyymm, p, s);
        List<AptTradeRecord> list = items.stream().map(AptTradeQueryServiceImpl::convert).collect(Collectors.toList());
        return new ListResponse<>(list);
    }

    private static AptTradeRecord convert(MolitAptTradeClient.Record r) {
        AptTradeRecord a = new AptTradeRecord();
        a.setDealAmount(r.getDealAmount());
        a.setDealYear(parseInt(r.getYear()));
        a.setDealMonth(parseInt(r.getMonth()));
        a.setDealDay(parseInt(r.getDay()));
        a.setAptName(r.getAptName());
        a.setExclusiveArea(parseDouble(r.getExclusiveArea()));
        a.setFloor(parseInt(r.getFloor()));
        a.setDong(r.getDong());
        a.setJibun(r.getJibun());
        a.setRoadName(r.getRoadName());
        a.setLawdCd(r.getLawdCd());
        a.setSerialNo(r.getSerialNo());
        return a;
    }

    private static int parseInt(String s) {
        try { return s == null ? 0 : Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
    private static Double parseDouble(String s) {
        try { return s == null ? null : Double.parseDouble(s.trim()); } catch (Exception e) { return null; }
    }
}

