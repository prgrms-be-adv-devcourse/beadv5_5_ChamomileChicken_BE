package jabaclass.frontend.client;

import jabaclass.frontend.dto.DepositHistoryItemDto;
import jabaclass.frontend.dto.LoginRequest;
import jabaclass.frontend.dto.LoginResponse;
import jabaclass.frontend.dto.SignupRequest;
import jabaclass.frontend.dto.UserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public void sendVerificationCode(String email) {
        restTemplate.postForEntity(
            userUrl + "/api/v1/email/verifications",
            Map.of("email", email),
            Void.class
        );
    }

    public String verifyEmailCode(String email, String code) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
            userUrl + "/api/v1/email/verifications/confirm",
            Map.of("email", email, "code", code),
            Map.class
        );
        return (String) response.getBody().get("verifiedToken");
    }

    public void checkEmailDuplicate(String email) {
        restTemplate.postForEntity(
            userUrl + "/api/v1/users/email-check",
            Map.of("email", email),
            Void.class
        );
    }

    public UserInfoDto getMyInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> response = restTemplate.exchange(
            userUrl + "/api/v1/users/me",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class
        );
        Map<String, Object> body = response.getBody();
        UserInfoDto dto = new UserInfoDto();
        dto.setUserId(body.get("userId") != null ? UUID.fromString(body.get("userId").toString()) : null);
        dto.setName((String) body.get("name"));
        dto.setEmail((String) body.get("email"));
        dto.setPhone((String) body.get("phone"));
        dto.setDeposit(body.get("deposit") != null ? new BigDecimal(body.get("deposit").toString()) : BigDecimal.ZERO);
        return dto;
    }

    public List<DepositHistoryItemDto> getDepositHistories(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> response = restTemplate.exchange(
            userUrl + "/api/v1/deposits",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class
        );
        List<Map> items = (List<Map>) response.getBody().get("items");
        if (items == null) return List.of();
        return items.stream().map(m -> {
            DepositHistoryItemDto dto = new DepositHistoryItemDto();
            dto.setType(m.get("type") != null ? m.get("type").toString() : null);
            dto.setAmount(m.get("amount") != null ? new BigDecimal(m.get("amount").toString()) : BigDecimal.ZERO);
            dto.setCreatedAt(m.get("createdAt") != null ? m.get("createdAt").toString() : null);
            return dto;
        }).toList();
    }

    public void register(SignupRequest request) {
        restTemplate.postForEntity(
            userUrl + "/api/v1/users/register",
            request,
            Void.class
        );
    }
}