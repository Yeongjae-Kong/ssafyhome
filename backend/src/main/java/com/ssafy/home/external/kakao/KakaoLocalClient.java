package com.ssafy.home.external.kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class KakaoLocalClient {

    private final String baseUrl;
    private final String restKey;
    private final RestTemplate rest;

    public KakaoLocalClient(
            @Value("${kakao.local.base:https://dapi.kakao.com}") String baseUrl,
            @Value("${kakao.local.key:}") String restKey,
            @Value("${kakao.local.connect-timeout-ms:1000}") int connectTimeoutMs,
            @Value("${kakao.local.read-timeout-ms:1500}") int readTimeoutMs
    ) {
        this.baseUrl = baseUrl;
        this.restKey = restKey;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        try {
            rf.setConnectTimeout(Math.max(100, connectTimeoutMs));
            rf.setReadTimeout(Math.max(200, readTimeoutMs));
        } catch (Exception ignored) {}
        this.rest = new RestTemplate(rf);
    }

    public KakaoPoi searchNearestByCategory(double lat, double lon, String categoryCode, int radiusMeters) {
        if (restKey == null || restKey.isBlank()) {
            throw new IllegalStateException("Kakao Local REST key is not configured");
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v2/local/search/category.json")
                .queryParam("category_group_code", categoryCode)
                .queryParam("x", lon)
                .queryParam("y", lat)
                .queryParam("radius", radiusMeters)
                .queryParam("sort", "distance")
                .build(true).toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "KakaoAK " + restKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map body = null;
        int attempts = 0;
        RuntimeException lastEx = null;
        while (attempts < 2) {
            try {
                ResponseEntity<Map> resp = rest.exchange(uri, HttpMethod.GET, entity, Map.class);
                body = resp.getBody();
                break;
            } catch (RuntimeException ex) {
                lastEx = ex;
                attempts++;
                try { Thread.sleep(80L * attempts); } catch (InterruptedException ignored) {}
            }
        }
        if (body == null && lastEx != null) {
            return null;
        }
        if (body == null) return null;
        Object docsObj = body.get("documents");
        if (!(docsObj instanceof List)) return null;
        List docs = (List) docsObj;
        if (docs.isEmpty()) return null;
        Map first = (Map) docs.get(0);
        String name = (String) first.getOrDefault("place_name", "");
        String xStr = (String) first.getOrDefault("x", null); // lon
        String yStr = (String) first.getOrDefault("y", null); // lat
        String distanceStr = String.valueOf(first.getOrDefault("distance", ""));
        Double poiLon = xStr == null ? null : Double.valueOf(xStr);
        Double poiLat = yStr == null ? null : Double.valueOf(yStr);
        Integer distanceM = null;
        try {
            if (distanceStr != null && !distanceStr.isBlank()) {
                distanceM = Integer.parseInt(distanceStr);
            }
        } catch (NumberFormatException ignored) {}
        KakaoPoi poi = new KakaoPoi();
        poi.setName(name);
        poi.setLat(poiLat);
        poi.setLon(poiLon);
        poi.setDistanceM(distanceM);
        return poi;
    }

    public static class KakaoPoi {
        private String name;
        private Double lat;
        private Double lon;
        private Integer distanceM; // Kakao가 계산한 거리(미터), null일 수 있음

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
        public Double getLon() { return lon; }
        public void setLon(Double lon) { this.lon = lon; }
        public Integer getDistanceM() { return distanceM; }
        public void setDistanceM(Integer distanceM) { this.distanceM = distanceM; }
    }
}
