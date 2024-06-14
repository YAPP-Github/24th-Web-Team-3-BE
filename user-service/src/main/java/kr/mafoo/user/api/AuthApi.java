package kr.mafoo.user.api;

import io.swagger.v3.oas.annotations.Operation;
import kr.mafoo.user.controller.dto.request.KakaoLoginRequest;
import kr.mafoo.user.controller.dto.request.TokenRefreshRequest;
import kr.mafoo.user.controller.dto.response.LoginResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@RequestMapping("/v1/auth")
public interface AuthApi {
    @Operation(summary = "카카오 로그인", description = "카카오 인가 코드로 로그인(토큰 발행)합니다.")
    @PostMapping("/login/kakao")
    Mono<LoginResponse> loginWithKakao(
            @RequestBody KakaoLoginRequest request
    );

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 기존 토큰을 갱신합니다.")
    @PostMapping("/refresh")
    Mono<LoginResponse> loginWithRefreshToken(
            @RequestBody TokenRefreshRequest request
    );
}
