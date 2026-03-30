package jabaclass.settlement.infrastructure.client.user;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.settlement.application.dto.SellerSettlementAccount;
import jabaclass.settlement.application.dto.SellerSettlementDetail;
import jabaclass.settlement.application.port.external.SellerSettlementPort;
import jabaclass.settlement.infrastructure.config.ClientProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UserClient implements SellerSettlementPort {

	private final RestTemplate restTemplate;
	private final ClientProperties clientProperties;

	@Override
	public List<SellerSettlementDetail> fetchSellers(Set<UUID> sellerIds) {
		if (sellerIds == null || sellerIds.isEmpty()) {
			return List.of();
		}

		SellerBulkRequest request = new SellerBulkRequest(sellerIds.stream().toList());

		List<SellerResponse> responses = restTemplate.exchange(
			clientProperties.user().baseUrl() + "/api/v1/users/sellers/bulk",
			HttpMethod.POST,
			new HttpEntity<>(request),
			new ParameterizedTypeReference<List<SellerResponse>>() {}
		).getBody();

		if (responses == null) {
			return List.of();
		}

		return responses.stream()
			.map(item -> new SellerSettlementDetail(
				item.sellerId(),
				item.role(),
				item.accountRegistered(),
				item.accountActive()
			))
			.toList();
	}

	@Override
	public List<SellerSettlementAccount> fetchSellerSettlementAccounts(Set<UUID> sellerIds) {
		if (sellerIds == null || sellerIds.isEmpty()) {
			return List.of();
		}

		SellerBulkRequest request = new SellerBulkRequest(sellerIds.stream().toList());

		List<SellerSettlementAccountResponse> responses = restTemplate.exchange(
			clientProperties.user().baseUrl() + "/api/v1/users/sellers/settlement-accounts/bulk",
			HttpMethod.POST,
			new HttpEntity<>(request),
			new ParameterizedTypeReference<List<SellerSettlementAccountResponse>>() {}
		).getBody();

		if (responses == null) {
			return List.of();
		}

		return responses.stream()
			.map(response -> new SellerSettlementAccount(
				response.sellerId(),
				response.bankCode(),
				response.accountNumber(),
				response.accountHolder(),
				response.active()
			))
			.toList();
	}

	public record SellerBulkRequest(
		List<UUID> sellerIds
	) {
	}

	public record SellerResponse(
		UUID sellerId,
		String role,
		boolean accountRegistered,
		boolean accountActive
	) {
	}

	public record SellerSettlementAccountResponse(
		UUID sellerId,
		String bankCode,
		String accountNumber,
		String accountHolder,
		boolean active
	) {
	}
}
