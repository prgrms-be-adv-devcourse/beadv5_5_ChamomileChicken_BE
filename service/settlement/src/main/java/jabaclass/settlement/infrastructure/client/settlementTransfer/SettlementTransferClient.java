package jabaclass.settlement.infrastructure.client.settlementTransfer;

import java.math.BigDecimal;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.settlement.application.dto.SettlementTransferCommand;
import jabaclass.settlement.application.dto.SettlementTransferResult;
import jabaclass.settlement.application.port.external.SettlementTransferPort;
import jabaclass.settlement.infrastructure.config.ClientProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class SettlementTransferClient implements SettlementTransferPort {

	private final RestTemplate restTemplate;
	private final ClientProperties clientProperties;

	@Override
	public SettlementTransferResult transfer(SettlementTransferCommand command) {
		TransferRequest request = new TransferRequest(
			command.settlementId().toString(),
			command.sellerId().toString(),
			command.bankCode(),
			command.accountNumber(),
			command.accountHolder(),
			command.amount()
		);

		TransferResponse response = restTemplate.exchange(
			clientProperties.transfer().baseUrl() + "/internal/transfers/settlements",
			HttpMethod.POST,
			new HttpEntity<>(request),
			TransferResponse.class
		).getBody();

		if (response == null) {
			return SettlementTransferResult.fail("송금 응답이 비어 있습니다.");
		}

		return new SettlementTransferResult(response.success(), response.message());
	}

	public record TransferRequest(
		String settlementId,
		String sellerId,
		String bankCode,
		String accountNumber,
		String accountHolder,
		BigDecimal amount
	) {
	}

	public record TransferResponse(
		boolean success,
		String message
	) {
	}
}