package jabaclass.user.auth.presentation.controller;

import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.user.auth.application.usecase.TokenStatusUseCase;
import jabaclass.user.auth.presentation.dto.response.TokenResult;
import jabaclass.user.auth.presentation.dto.response.TokenResponseDto;
import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.application.exception.AuthException;
import jabaclass.user.auth.application.usecase.LoginUseCase;
import jabaclass.user.auth.application.usecase.LogoutUseCase;
import jabaclass.user.auth.application.usecase.ReissueUseCase;
import jabaclass.user.auth.presentation.dto.request.LoginRequestDto;
import jabaclass.user.common.dto.ApiResponseDto;
import jabaclass.user.common.auth.CurrentUser;
import jabaclass.user.auth.application.service.ReportTheftPageService;
import jabaclass.user.auth.application.usecase.ReportTheftUseCase;
import jabaclass.user.auth.presentation.dto.response.TokenStatusResult;
import jabaclass.user.common.util.ClientIpUtils;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ReissueUseCase reissueUseCase;
    private final ReportTheftUseCase reportTheftUseCase;
    private final TokenStatusUseCase tokenStatusUseCase;
    private final ReportTheftPageService reportTheftPageService;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${internal.secret}")
    private String internalSecret;

    @GetMapping("/internal/token-status")
    public ResponseEntity<TokenStatusResult> checkTokenStatus(
        @RequestHeader("X-Token") String token,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader("X-Token-Iat") long tokenIssuedAtMillis,
        @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {

        if (!internalSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(tokenStatusUseCase.checkTokenStatus(token, userId, tokenIssuedAtMillis));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<TokenResponseDto>> login(
        @Valid @RequestBody LoginRequestDto request,
        HttpServletRequest servletRequest,
        HttpServletResponse response) {

        String clientIp = ClientIpUtils.extractIp(servletRequest);
        String userAgent = Optional.ofNullable(servletRequest.getHeader("User-Agent")).orElse("unknown");

        TokenResult result = loginUseCase.login(request, clientIp, userAgent);

        response.addHeader(HttpHeaders.SET_COOKIE,
            buildRefreshTokenCookie(result.getRefreshToken(),
                refreshTokenValidity / 1000).toString());

        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK,
            "로그인 성공",
            new TokenResponseDto(result.getAccessToken())));
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
        @CurrentUser UUID userId,
        HttpServletRequest request,
        HttpServletResponse response) {

        String accessToken = extractAccessToken(request);
        logoutUseCase.logout(userId, accessToken);

        // Cookie 만료 처리
        response.addHeader(HttpHeaders.SET_COOKIE,
            buildRefreshTokenCookie("", 0).toString());

        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, "로그아웃 성공", null));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponseDto<TokenResponseDto>> reissue(
        @CookieValue(name = "refresh_token", required = false) String refreshToken,
        HttpServletResponse response) {

        if (refreshToken == null) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        TokenResult result = reissueUseCase.reissue(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE,
            buildRefreshTokenCookie(result.getRefreshToken(), refreshTokenValidity / 1000).toString());

        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, "토큰 재발급 성공",
            new TokenResponseDto(result.getAccessToken())));
    }

    @GetMapping(value = "/report-theft", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> reportTheftPage(@RequestParam String token) {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(reportTheftPageService.buildPage(token));
    }

    @PostMapping("/report-theft")
    public ResponseEntity<ApiResponseDto<Void>> reportTheft(@RequestParam String token) {
        reportTheftUseCase.reportTheft(token);
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, "계정 보호 조치가 완료되었습니다.", null));
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
