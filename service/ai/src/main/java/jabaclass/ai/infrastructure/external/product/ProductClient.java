package jabaclass.ai.infrastructure.external.product;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.ai.application.dto.CandidateClassDto;
import jabaclass.ai.application.port.external.ProductPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductClient implements ProductPort {

	private final RestTemplate restTemplate;

	@Value("${product.service.url}")
	private String productServiceUrl;

	@Override
	public List<CandidateClassDto> getCandidates(float[] userVector) {

		String url = productServiceUrl + "/internal/products/recommendation";

		ResponseEntity<CandidateClassDto[]> response =
			restTemplate.postForEntity(
				url,
				userVector,
				CandidateClassDto[].class
			);

		return List.of(response.getBody());
	}

}
