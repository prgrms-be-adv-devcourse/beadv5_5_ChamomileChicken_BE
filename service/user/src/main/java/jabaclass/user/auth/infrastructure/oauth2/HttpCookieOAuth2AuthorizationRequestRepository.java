package jabaclass.user.auth.infrastructure.oauth2;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

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
	private static final String FALLBACK_PREFIX = "fb:";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		return CookieUtils.getCookie(request, COOKIE_NAME)
			.map(cookie -> {
				String value = cookie.getValue();
				try {
					if (value.startsWith(FALLBACK_PREFIX)) {
						String json = new String(
							Base64.getUrlDecoder().decode(value.substring(FALLBACK_PREFIX.length())),
							StandardCharsets.UTF_8);
						return objectMapper.readValue(json, OAuth2AuthorizationRequestDto.class).toRequest();
					}
					String json = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + value);
					if (json == null) return null;
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

			try {
				redisTemplate.opsForValue().set(
					REDIS_KEY_PREFIX + state, json, Duration.ofSeconds(COOKIE_MAX_AGE));
				log.info("[AUTH] OAuth2 state Redis 저장. key={}", REDIS_KEY_PREFIX + state);
				CookieUtils.addCookie(response, COOKIE_NAME, state, COOKIE_MAX_AGE);
			} catch (Exception redisEx) {
				log.warn("[AUTH] Redis 장애, OAuth2 state 쿠키 직접 저장으로 fallback.");
				String encoded = FALLBACK_PREFIX + Base64.getUrlEncoder()
					.encodeToString(json.getBytes(StandardCharsets.UTF_8));
				CookieUtils.addCookie(response, COOKIE_NAME, encoded, COOKIE_MAX_AGE);
			}
		} catch (Exception e) {
			throw new RuntimeException("OAuth2 요청 저장 실패", e);
		}
	}

	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(
		HttpServletRequest request, HttpServletResponse response) {
		OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
		if (authRequest != null) {
			CookieUtils.getCookie(request, COOKIE_NAME).ifPresent(cookie -> {
				String value = cookie.getValue();
				if (!value.startsWith(FALLBACK_PREFIX)) {
					try {
						redisTemplate.delete(REDIS_KEY_PREFIX + value);
					} catch (Exception e) {
						log.warn("[AUTH] Redis 장애, OAuth2 state 삭제 스킵.");
					}
				}
			});
			CookieUtils.deleteCookie(request, response, COOKIE_NAME);
		}
		return authRequest;
	}
}
