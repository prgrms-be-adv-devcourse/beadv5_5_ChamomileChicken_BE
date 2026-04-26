package jabaclass.user.auth.infrastructure.oauth2;

import java.io.IOException;
import java.time.Duration;

import jabaclass.user.auth.application.service.AuthService;
import jabaclass.user.common.util.ClientIpUtils;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jabaclass.user.auth.infrastructure.jwt.TokenProvider;
import jabaclass.user.auth.presentation.dto.response.TokenResult;
import jabaclass.user.user.domain.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private final TokenProvider tokenProvider;
	private final StringRedisTemplate redisTemplate;
	private final AuthService authService;

	public OAuth2SuccessHandler(TokenProvider tokenProvider,
								StringRedisTemplate redisTemplate,
								@Lazy AuthService authService) {
		this.tokenProvider = tokenProvider;
		this.redisTemplate = redisTemplate;
		this.authService = authService;
	}

	@Value("${jwt.refresh-token-validity}")
	private long refreshTokenValidity;

	@Value("${cookie.secure}")
	private boolean cookieSecure;

	@Value("${oauth2.redirect-uri}")
	private String redirectUri;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication) throws IOException {

		CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
		User user = oAuth2User.getUser();

		String clientIp = ClientIpUtils.extractIp(request);
		String userAgent = request.getHeader("User-Agent");
		authService.handleLoginSecurity(user.getId(), clientIp, userAgent);

		TokenResult tokens = new TokenResult(
			tokenProvider.generateAccessToken(user.getId(), user.getRole()),
			tokenProvider.generateRefreshToken(user.getId(), user.getRole())
		);

		redisTemplate.opsForValue().set(
			"refresh:" + user.getId(),
			tokens.getRefreshToken(),
			Duration.ofMillis(refreshTokenValidity)
		);

		response.addHeader(HttpHeaders.SET_COOKIE,
			ResponseCookie.from("refresh_token", tokens.getRefreshToken())
				.httpOnly(true)
				.secure(cookieSecure)
				.sameSite("Strict")
				.path("/api/v1/auth")
				.maxAge(refreshTokenValidity / 1000)
				.build()
				.toString()
		);

		// access token은 redirect URL의 fragment로 전달
		// Vue가 window.location.hash에서 읽고 Pinia에 저장 후 URL 정리
		response.sendRedirect(redirectUri + "#token=" + tokens.getAccessToken());
	}
}
