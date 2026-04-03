package jabaclass.frontend.config;

import jabaclass.frontend.exception.SessionExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenRefreshInterceptor implements ClientHttpRequestInterceptor {

    private final HttpServletRequest httpServletRequest;

    @Value("${services.user-url}")
    private String userUrl;

    // reissue 호출 전용 RestTemplate — 이 인터셉터를 거치지 않아 무한 루프 방지
    private final RestTemplate reissueTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);

        if (response.getStatusCode().value() != 401) {
            return response;
        }

        response.close();
        String oldToken = (String) httpServletRequest.getSession(false) != null
                ? (String) httpServletRequest.getSession(false).getAttribute("accessToken") : "none";
        log.debug("401 감지 — 토큰 재발급 시도 [uri={}, oldToken={}...]",
                request.getURI(), oldToken != null && oldToken.length() > 10 ? oldToken.substring(0, 10) : oldToken);

        HttpSession session = httpServletRequest.getSession(false);
        if (session == null || session.getAttribute("refreshToken") == null) {
            throw new SessionExpiredException();
        }

        String refreshToken = (String) session.getAttribute("refreshToken");

        try {
            HttpHeaders reissueHeaders = new HttpHeaders();
            reissueHeaders.setContentType(MediaType.APPLICATION_JSON);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> reissueResponse = reissueTemplate.postForEntity(
                    userUrl + "/api/v1/auth/reissue",
                    new org.springframework.http.HttpEntity<>(Map.of("refreshToken", refreshToken), reissueHeaders),
                    Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) reissueResponse.getBody().get("data");
            String newAccessToken = (String) data.get("accessToken");
            String newRefreshToken = (String) data.get("refreshToken");

            session.setAttribute("accessToken", newAccessToken);
            session.setAttribute("refreshToken", newRefreshToken);
            log.debug("토큰 재발급 성공 — newToken={}... 원래 요청 재시도",
                    newAccessToken.length() > 10 ? newAccessToken.substring(0, 10) : newAccessToken);

            // 새 토큰으로 원래 요청 재시도
            HttpRequest retryRequest = new HttpRequestWrapper(request) {
                @Override
                public HttpHeaders getHeaders() {
                    HttpHeaders headers = new HttpHeaders();
                    headers.putAll(super.getHeaders());
                    headers.setBearerAuth(newAccessToken);
                    return headers;
                }
            };
            return execution.execute(retryRequest, body);

        } catch (Exception e) {
            log.warn("토큰 재발급 실패 — 세션 만료 처리: {}", e.getMessage());
            if (session != null) {
                try { session.invalidate(); } catch (IllegalStateException ignored) {}
            }
            throw new SessionExpiredException();
        }
    }
}
