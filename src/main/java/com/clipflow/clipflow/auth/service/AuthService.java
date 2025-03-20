package com.clipflow.clipflow.auth.service;

import com.clipflow.clipflow.auth.dto.NaverUserResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import net.minidev.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final String NAVER_TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";
    private static final String CLIENT_ID = "ucxtdrGspItWJNLbdt7O";
    private static final String CLIENT_SECRET = "DJjhi5rVIX";
    private static final String REDIRECT_URI = "http://localhost:8080/api/auth/callback";
    // 배포 시 REDIRECT_URI : http://clipflow.com/api/auth/callback
    private static final String STATE_SESSION_KEY = "NAVER_STATE";

    //네이버 로그인 URL 생성
    public String getNaverLoginUrl(HttpSession session) {

        String state = UUID.randomUUID().toString(); // 매 요청마다 랜덤 state 생성
        session.setAttribute(STATE_SESSION_KEY, state); // 세션에 저장
        
        return "https://nid.naver.com/oauth2.0/authorize?client_id=" + CLIENT_ID +
                "&redirect_uri=" + REDIRECT_URI + "&response_type=code&state=" + state;

    }

    // 네이버 로그인 처리 (액세스 토큰 발급 및 사용자 정보 조회)
    public NaverUserResponse naverLogin(String code, String state) {

        // 1. 네이버에 액세스 토큰 요청
        String accessToken = getAccessTokenFromNaver(code, state);

        // 2. 액세스 토큰을 이용해 사용자 정보 요청
        return getUserInfoFromNaver(accessToken);

    }

    // 네이버에서 액세스 토큰 가져오기
    private String getAccessTokenFromNaver(String code, String state) {

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환기

        // 요청 파라미터 설정 (✅ MultiValueMap 사용)
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", CLIENT_ID);
        params.add("client_secret", CLIENT_SECRET);
        params.add("code", code);
        params.add("state", state);
        params.add("redirect_uri", REDIRECT_URI);

        // HTTP 요청 설정 (✅ Content-Type 명확히 지정)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // 네이버에 POST 요청 보내기
        ResponseEntity<String> response = restTemplate.exchange(
                NAVER_TOKEN_URL,
                HttpMethod.POST,
                request,
                String.class
        );

        System.out.println("네이버 응답: " + response.getBody()); // 응답을 로그로 출력, 디버깅용 코드
        
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                // JSON 응답을 Jackson의 JsonNode로 변환 후, access_token 가져오기
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode accessTokenNode = jsonNode.get("access_token");

                if (accessTokenNode == null) { // access_token이 없을 경우 예외 발생 방지
                    throw new RuntimeException("네이버 응답에 access_token이 없음: " + response.getBody());
                }

                return accessTokenNode.asText(); // 액세스 토큰 반환
            } catch (Exception e) {
                throw new RuntimeException("JSON 파싱 오류:" + response.getBody(), e);
            }
        } else {
            throw new RuntimeException("네이버 액세스 토큰 요청 실패: " + response.getStatusCode());
        }
    }

    // 네이버에서 사용자 정보 가져오기
    private NaverUserResponse getUserInfoFromNaver(String accessToken) {

        String url = NAVER_USER_INFO_URL;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken); // 올바른 토큰 넣었는지 확인

        HttpEntity<String> request = new HttpEntity<>(headers);
        
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환기
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                // ✅ Jackson을 사용해 JSON 파싱
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode responseNode = jsonNode.get("response"); // 사용자 정보 가져오기

                return new NaverUserResponse(
                        responseNode.get("id").asText(),
                        responseNode.get("email").asText(),
                        responseNode.get("nickname").asText(),
                        responseNode.get("profile_image").asText()
                );

            } catch (Exception e) {
                throw new RuntimeException("JSON 파싱 오류", e);
            }
        } else {
            throw new RuntimeException("Failed to get user info from Naver");
        }
    }





}
