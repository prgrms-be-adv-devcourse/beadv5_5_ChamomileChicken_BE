package jabaclass.user.auth.infrastructure.oauth2;

import java.io.IOException;
import java.util.Optional;

import jabaclass.user.auth.application.service.AuthService;
import jabaclass.user.common.util.ClientIpUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jabaclass.user.auth.presentation.dto.response.TokenResult;
import jabaclass.user.user.domain.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private final AuthService authService;

	public OAuth2SuccessHandler(@Lazy AuthService authService) {
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
		String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown");
		authService.handleLoginSecurity(user.getId(), clientIp, userAgent);

		TokenResult tokens = authService.issueOAuth2Tokens(user.getId(), user.getRole());

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
