package com.ssafy.home.external.molit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class MolitAptTradeClient {

    private static final Logger log = LoggerFactory.getLogger(MolitAptTradeClient.class);

    private final String baseUrl;
    private final String aptTradePath;
    private final String serviceKey;
    private final RestTemplate rest;
    private final ObjectMapper om = new ObjectMapper();
    private final boolean serviceKeyNeedEncode;

    public MolitAptTradeClient(
            @Value("${molit.base-url:http://openapi.molit.go.kr:8081}") String baseUrl,
            @Value("${molit.paths.apt-trade:/OpenAPI_ToolInstallPackage/service/rest/RTMSOBJSvc/getRTMSDataSvcAptTradeDev}") String aptTradePath,
            @Value("${molit.service-key:}") String serviceKey,
            @Value("${molit.service-key-need-encode:false}") boolean serviceKeyNeedEncode
    ) {
        this.baseUrl = baseUrl;
        this.aptTradePath = aptTradePath;
        this.serviceKey = serviceKey;
        this.serviceKeyNeedEncode = serviceKeyNeedEncode;
        this.rest = new RestTemplate();
    }

    public List<Record> fetch(String lawdCd, String dealYmd, Integer pageNo, Integer numOfRows) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("MOLIT service-key is not configured");
        }
        String keyForUse = serviceKeyNeedEncode
                ? URLEncoder.encode(serviceKey, StandardCharsets.UTF_8)
                : serviceKey;

        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(aptTradePath)
                .queryParam("serviceKey", keyForUse)
                .queryParam("LAWD_CD", lawdCd)
                .queryParam("DEAL_YMD", dealYmd)
                .queryParam("_type", "json");
        if (pageNo != null) b.queryParam("pageNo", pageNo);
        if (numOfRows != null) b.queryParam("numOfRows", numOfRows);
        URI uri = b.build(true).toUri();
        try {
            String uriForLog = uri.toString().replaceAll("serviceKey=[^&]*", "serviceKey=***");
            log.debug("[molit] GET {}", uriForLog);
        } catch (Exception ignored) {}

        ResponseEntity<String> resp = rest.exchange(uri, HttpMethod.GET, new HttpEntity<Void>(new HttpHeaders()), String.class);
        String raw = resp.getBody();
        if (raw == null || raw.isBlank()) return List.of();

        try {
            JsonNode root = om.readTree(raw);
            JsonNode items = root.path("response").path("body").path("items").path("item");
            List<Record> list = new ArrayList<>();
            if (items.isArray()) {
                for (JsonNode n : items) list.add(mapRecord(n));
            } else if (items.isObject()) {
                list.add(mapRecord(items));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("failed to parse molit response: " + e.getMessage(), e);
        }
    }

    private static String asText(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.path(k);
            if (!v.isMissingNode()) return v.asText(null);
        }
        return null;
    }

    private static Record mapRecord(JsonNode n) {
        Record r = new Record();
        // numeric strings often include commas and spaces
        r.setDealAmount(cleanAmount(asText(n, "거래금액", "dealAmount", "DEAL_AMT", "deal_amt")));
        r.setBuildYear(asText(n, "건축년도", "buildYear"));
        // year/month/day: support both korean and english field names sometimes used by apis.data.go.kr
        r.setYear(asText(n, "년", "year", "dealYear", "DEAL_YEAR"));
        r.setMonth(asText(n, "월", "month", "dealMonth", "DEAL_MONTH"));
        r.setDay(asText(n, "일", "day", "dealDay", "DEAL_DAY"));
        r.setAptName(asText(n, "아파트", "aptName", "aptNm", "APT_NAME"));
        r.setExclusiveArea(asText(n, "전용면적", "excluUseAr", "exclusiveArea", "EXCLUSE_AR"));
        r.setFloor(asText(n, "층", "floor"));
        r.setDong(asText(n, "법정동", "dong", "legalDong", "umdNm"));
        r.setJibun(asText(n, "지번", "jibun"));
        r.setRoadName(asText(n, "도로명", "roadName"));
        r.setLawdCd(asText(n, "지역코드", "LAWD_CD", "lawdCd", "sggCd"));
        r.setSerialNo(asText(n, "일련번호", "serialNo"));
        // Fallback: derive year/month from dealYmd if present and year/month missing
        String ymd = asText(n, "DEAL_YMD", "dealYmd", "ymd");
        if (r.getYear() == null && ymd != null && ymd.length() >= 6) r.setYear(ymd.substring(0,4));
        if (r.getMonth() == null && ymd != null && ymd.length() >= 6) r.setMonth(ymd.substring(4,6));
        return r;
    }

    private static String cleanAmount(String s) {
        if (s == null) return null;
        return s.replace(",", "").replace(" ", "").trim();
    }

    public static class Record {
        private String dealAmount; // 숫자 문자열(원)
        private String buildYear;
        private String year;
        private String month;
        private String day;
        private String aptName;
        private String exclusiveArea; // ㎡
        private String floor;
        private String dong;
        private String jibun;
        private String roadName;
        private String lawdCd; // 지역코드(5자리 시군구)
        private String serialNo;

        public String getDealAmount() { return dealAmount; }
        public void setDealAmount(String dealAmount) { this.dealAmount = dealAmount; }
        public String getBuildYear() { return buildYear; }
        public void setBuildYear(String buildYear) { this.buildYear = buildYear; }
        public String getYear() { return year; }
        public void setYear(String year) { this.year = year; }
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public String getDay() { return day; }
        public void setDay(String day) { this.day = day; }
        public String getAptName() { return aptName; }
        public void setAptName(String aptName) { this.aptName = aptName; }
        public String getExclusiveArea() { return exclusiveArea; }
        public void setExclusiveArea(String exclusiveArea) { this.exclusiveArea = exclusiveArea; }
        public String getFloor() { return floor; }
        public void setFloor(String floor) { this.floor = floor; }
        public String getDong() { return dong; }
        public void setDong(String dong) { this.dong = dong; }
        public String getJibun() { return jibun; }
        public void setJibun(String jibun) { this.jibun = jibun; }
        public String getRoadName() { return roadName; }
        public void setRoadName(String roadName) { this.roadName = roadName; }
        public String getLawdCd() { return lawdCd; }
        public void setLawdCd(String lawdCd) { this.lawdCd = lawdCd; }
        public String getSerialNo() { return serialNo; }
        public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
    }
}
