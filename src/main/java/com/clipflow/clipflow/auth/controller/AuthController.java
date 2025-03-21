package com.clipflow.clipflow.auth.controller;

import com.clipflow.clipflow.auth.dto.NaverUserResponse;
import com.clipflow.clipflow.auth.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/naverLogin") // 네이버 로그인 URL 생성 (네이버 로그인 페이지로 리디렉트)
    public ResponseEntity<String> naverlogin(HttpSession session) {

        String naverLoginUrl = authService.getNaverLoginUrl(session);
        return ResponseEntity.ok(naverLoginUrl);

    }

    @GetMapping("/callback") // 네이버 로그인 콜백 (사용자 정보 세션 저장)
    public ResponseEntity<NaverUserResponse> naverCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpSession session) {

        // 네이버 API에서 액세스 토큰, 사용자 정보 가져오기
        NaverUserResponse userResponse = authService.naverLogin(code, state);

        // 세션에 사용자 정보 저장하기
        session.setAttribute("user", userResponse);

        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/me") // 현재 로그인한 사용자 정보 조회
    public ResponseEntity<NaverUserResponse> getCurrentUser(HttpSession session) {
        NaverUserResponse user = (NaverUserResponse) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout") // 로그아웃 (세션 삭제)
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate(); // 세션 삭제
        return ResponseEntity.ok().build();
    }

}
