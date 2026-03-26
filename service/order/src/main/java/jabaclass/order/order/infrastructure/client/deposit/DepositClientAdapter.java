package jabaclass.order.order.infrastructure.client.deposit;

import jabaclass.order.order.application.client.DepositClient;
import jabaclass.order.order.infrastructure.client.deposit.dto.DepositUseRequestDto;
import jabaclass.order.order.infrastructure.client.deposit.dto.DepositValidateRequestDto;
import jabaclass.order.order.infrastructure.client.deposit.dto.DepositValidateResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DepositClientAdapter implements DepositClient {

    private final RestTemplate restTemplate;

    @Value("${external.deposits.base-url:http://localhost:9003}")
    private String depositBaseUrl;

    @Override
    public boolean isValid(UUID userId, BigDecimal depositAmount) {
        DepositValidateResponseDto response = restTemplate.postForObject(
            depositBaseUrl + "/api/v1/deposits/validate",
            new DepositValidateRequestDto(userId, depositAmount),
            DepositValidateResponseDto.class
        );

        return (response != null) && (response.valid());
    }

    @Override
    public void use(UUID userId, BigDecimal amount) {
        restTemplate.postForLocation(
            depositBaseUrl + "/api/v1/deposits/use",
            new DepositUseRequestDto(userId, amount)
        );
    }
}
