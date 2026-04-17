package jabaclass.user.auth.infrastructure.oauth2;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
	implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	public static final String COOKIE_NAME = "oauth2_auth_request";
	private static final int COOKIE_MAX_AGE = 180; // 3분

	private final CookieUtils cookieUtils;

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		return CookieUtils.getCookie(request, COOKIE_NAME)
			.map(cookie -> cookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
			.orElse(null);
	}

	@Override
	public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
		HttpServletRequest request,
		HttpServletResponse response) {
		if (authorizationRequest == null) {
			CookieUtils.deleteCookie(request, response, COOKIE_NAME);
			return;
		}
		CookieUtils.addCookie(response, COOKIE_NAME,
			cookieUtils.serialize(authorizationRequest), COOKIE_MAX_AGE);
	}

	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(
		HttpServletRequest request, HttpServletResponse response) {
		return loadAuthorizationRequest(request);
	}
}
