package jabaclass.order.infrastructure.client.deposit;

import jabaclass.order.application.port.out.DepositPort;
import jabaclass.order.infrastructure.client.deposit.dto.DepositValidateResponseDto;
import jabaclass.order.infrastructure.client.deposit.dto.DepositUseRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DepositAdapter implements DepositPort {

    private final RestTemplate restTemplate;

    @Value("${external.deposits.base-url:http://localhost:9003}")
    private String depositBaseUrl;

    @Override
    public boolean validateAndUse(UUID userId, BigDecimal depositAmount) {
        DepositValidateResponseDto response = restTemplate.postForObject(
            depositBaseUrl + "/api/v1/deposits/use",
            new DepositUseRequestDto(userId, depositAmount),
            DepositValidateResponseDto.class
        );

        return (response != null) && (response.valid());
    }
}
