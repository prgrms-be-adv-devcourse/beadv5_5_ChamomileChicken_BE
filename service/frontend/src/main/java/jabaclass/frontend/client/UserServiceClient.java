package jabaclass.frontend.client;

import jabaclass.frontend.dto.LoginRequest;
import jabaclass.frontend.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.user-url}")
    private String userUrl;

    public LoginResponse login(LoginRequest request) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
            userUrl + "/api/v1/auth/login",
            request,
            Map.class
        );
        Map<String, Object> body = (Map<String, Object>) response.getBody().get("data");
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken((String) body.get("accessToken"));
        loginResponse.setRefreshToken((String) body.get("refreshToken"));
        return loginResponse;
    }
}