package jabaclass.user.auth.infrastructure.oauth2;

import java.time.Duration;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
	implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	public static final String COOKIE_NAME = "oauth2_auth_request";
	private static final String REDIS_KEY_PREFIX = "oauth2:state:";
	private static final int COOKIE_MAX_AGE = 60; // 1분

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		return CookieUtils.getCookie(request, COOKIE_NAME)
			.map(cookie -> {
				String key = REDIS_KEY_PREFIX + cookie.getValue();
				String json = redisTemplate.opsForValue().get(key);
				if (json == null) return null;
				try {
					return objectMapper.readValue(json, OAuth2AuthorizationRequestDto.class).toRequest();
				} catch (Exception e) {
					return null;
				}
			})
			.orElse(null);
	}

	@Override
	public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
		HttpServletRequest request, HttpServletResponse response) {
		if (authorizationRequest == null) {
			CookieUtils.deleteCookie(request, response, COOKIE_NAME);
			return;
		}
		try {
			String state = authorizationRequest.getState();
			String json = objectMapper.writeValueAsString(
				OAuth2AuthorizationRequestDto.from(authorizationRequest));

				redisTemplate.opsForValue().set(
				REDIS_KEY_PREFIX + state,
					json,
					Duration.ofSeconds(COOKIE_MAX_AGE)
				);

				log.info("[AUTH] OAuth2 state 저장. key={}, ttl={}s", REDIS_KEY_PREFIX + state, COOKIE_MAX_AGE);
				CookieUtils.addCookie(response, COOKIE_NAME, state, COOKIE_MAX_AGE);
		} catch (Exception e) {
			throw new RuntimeException("OAuth2 요청 저장 실패", e);
		}
	}

	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(
		HttpServletRequest request, HttpServletResponse response) {

		return loadAuthorizationRequest(request);
	}
}
