package jabaclass.user.auth.presentation.controller;

import jabaclass.user.auth.presentation.dto.response.TokenResult;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.user.auth.presentation.dto.response.TokenResponseDto;
import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.application.exception.AuthException;
import jabaclass.user.auth.application.usecase.LoginUseCase;
import jabaclass.user.auth.application.usecase.LogoutUseCase;
import jabaclass.user.auth.application.usecase.ReissueUseCase;
import jabaclass.user.auth.presentation.dto.request.LoginRequestDto;
import jabaclass.user.common.dto.ApiResponseDto;
import jabaclass.auth.util.SecurityUtil;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ReissueUseCase reissueUseCase;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<TokenResponseDto>> login(
        @Valid @RequestBody LoginRequestDto request,
        HttpServletResponse response) {

        TokenResult result = loginUseCase.login(request);

        response.addHeader(HttpHeaders.SET_COOKIE,
            buildRefreshTokenCookie(result.getRefreshToken(),
                refreshTokenValidity / 1000).toString());

        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK,
            "로그인 성공",
            new TokenResponseDto(result.getAccessToken(),
                result.getRole())));
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
        HttpServletRequest request,
        HttpServletResponse response) {

        String accessToken = extractAccessToken(request);
        logoutUseCase.logout(SecurityUtil.getCurrentUserId(), accessToken);

        // Cookie 만료 처리
        response.addHeader(HttpHeaders.SET_COOKIE,
            buildRefreshTokenCookie("", 0).toString());

        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, "로그아웃 성공", null));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponseDto<TokenResult>> reissue(
        @CookieValue(name = "refresh_token", required = false) String refreshToken,
        HttpServletResponse response) {

        if (refreshToken == null) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        TokenResult result = reissueUseCase.reissue(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE,
            buildRefreshTokenCookie(result.getRefreshToken(), refreshTokenValidity / 1000).toString());

        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, "토큰 재발급 성공", result));
    }

    private ResponseCookie buildRefreshTokenCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from("refresh_token", value)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(maxAgeSeconds)
            .build();
    }

    private String extractAccessToken(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        throw new AuthException(AuthErrorCode.INVALID_TOKEN);
    }
}
