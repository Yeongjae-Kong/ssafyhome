package com.ssafy.home.model.service;

import com.ssafy.home.external.kakao.KakaoLocalClient;
import com.ssafy.home.model.dao.HouseInfoDao;
import com.ssafy.home.model.dto.AccessSummaryResponse;
import com.ssafy.home.model.dto.HouseInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(readOnly = true)
public class AccessSummaryServiceImpl implements AccessSummaryService {

    private static class CacheEntry { long ts; AccessSummaryResponse data; }
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMillis = 24L * 60 * 60 * 1000; // 24h TTL

    private final HouseInfoDao houseInfoDao;
    private final KakaoLocalClient kakaoLocalClient;

    public AccessSummaryServiceImpl(HouseInfoDao houseInfoDao, KakaoLocalClient kakaoLocalClient) {
        this.houseInfoDao = houseInfoDao;
        this.kakaoLocalClient = kakaoLocalClient;
    }

    private static final Map<String, String> CATEGORY_MAP = Map.of(
            "지하철", "SW8",
            "마트", "MT1",
            "편의점", "CS2",
            "학교", "SC4",
            "병원", "HP8",
            "약국", "PM9",
            "카페", "CE7"
    );

    @Override
    public AccessSummaryResponse summarize(String aptSeq) {
        if (aptSeq == null || aptSeq.isBlank()) throw new IllegalArgumentException("aptSeq is required");
        // 캐시 조회 (TTL)
        AccessSummaryResponse cached = getCached(aptSeq);
        if (cached != null) return cached;
        // 고정 반경 1000m
        int radius = 1000;

        HouseInfo info = houseInfoDao.selectById(aptSeq);
        if (info == null) throw new IllegalArgumentException("apartment not found");

        double lat = parseDouble(info.getLatitude());
        double lon = parseDouble(info.getLongitude());
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            throw new IllegalArgumentException("apartment has no valid coordinates");
        }

        AccessSummaryResponse res = new AccessSummaryResponse();
        res.setAptSeq(aptSeq);
        res.setOrigin(new AccessSummaryResponse.Origin(lat, lon));

        List<AccessSummaryResponse.Item> items = new ArrayList<>();
        // 보장된 순서를 위해 LinkedHashMap 기반으로 목록 생성
        Map<String, String> ordered = new LinkedHashMap<>(CATEGORY_MAP);
        List<Map.Entry<String, String>> cats = new ArrayList<>(ordered.entrySet());
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(6, cats.size()));
        Map<String, Future<com.ssafy.home.external.kakao.KakaoLocalClient.KakaoPoi>> futures = new HashMap<>();
        for (Map.Entry<String, String> e : cats) {
            Callable<com.ssafy.home.external.kakao.KakaoLocalClient.KakaoPoi> task = () -> {
                try { return kakaoLocalClient.searchNearestByCategory(lat, lon, e.getValue(), radius); }
                catch (Exception ex) { return null; }
            };
            futures.put(e.getKey(), pool.submit(task));
        }
        for (Map.Entry<String, String> e : cats) {
            com.ssafy.home.external.kakao.KakaoLocalClient.KakaoPoi poi = null;
            try { poi = futures.get(e.getKey()).get(); }
            catch (InterruptedException | ExecutionException ignored) {}
            AccessSummaryResponse.Item item = new AccessSummaryResponse.Item();
            item.setType(e.getKey());
            if (poi == null || poi.getLat() == null || poi.getLon() == null) {
                item.setName("");
                item.setDistanceM(radius);
                item.setOnFootMin("15분 이상");
            } else {
                int distanceM = poi.getDistanceM() != null ? poi.getDistanceM() : (int) Math.round(haversineMeters(lat, lon, poi.getLat(), poi.getLon()));
                int onFootMin = (int) Math.ceil(distanceM / 80.0);
                item.setName(poi.getName());
                item.setDistanceM(distanceM);
                item.setOnFootMin(onFootMin + "분");
            }
            items.add(item);
        }
        pool.shutdown();

        res.setItems(items);
        putCache(aptSeq, res);
        return res;
    }

    private AccessSummaryResponse getCached(String aptSeq) {
        try {
            CacheEntry e = cache.get(aptSeq);
            if (e == null) return null;
            long now = System.currentTimeMillis();
            if (now - e.ts <= ttlMillis) return e.data;
            cache.remove(aptSeq, e);
            return null;
        } catch (Exception ignored) { return null; }
    }

    private void putCache(String aptSeq, AccessSummaryResponse res) {
        try {
            CacheEntry e = new CacheEntry();
            e.ts = System.currentTimeMillis();
            e.data = res;
            cache.put(aptSeq, e);
        } catch (Exception ignored) {}
    }

    private static double parseDouble(String s) {
        try { return s == null ? Double.NaN : Double.parseDouble(s); }
        catch (Exception e) { return Double.NaN; }
    }

    // Haversine formula (meters)
    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}

