package jabaclass.ai.infrastructure.external.openai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jabaclass.ai.infrastructure.external.openai.dto.request.OpenAiEmbeddingRequestDto;
import jabaclass.ai.infrastructure.external.openai.dto.response.OpenAiEmbeddingResponseDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

	private static final String OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";
	private static final int EMBEDDING_DIMENSIONS = 768;

	private final RestTemplate restTemplate;

	@Value("${openai.api.key}")
	private String apiKey;

	public float[] embedProductText(String title, String description, String roadAddress) {
		String input = buildInput(title, description, roadAddress);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(apiKey);

		HttpEntity<OpenAiEmbeddingRequestDto> request = new HttpEntity<>(
			OpenAiEmbeddingRequestDto.of(input, EMBEDDING_DIMENSIONS),
			headers
		);

		OpenAiEmbeddingResponseDto response = restTemplate.postForObject(
			OPENAI_EMBEDDINGS_URL,
			request,
			OpenAiEmbeddingResponseDto.class
		);

		if (response == null || response.getData() == null || response.getData().isEmpty()) {
			throw new IllegalStateException("상품 임베딩 생성 실패");
		}

		List<Double> values = response.getData().get(0).getEmbedding();
		if (values == null || values.isEmpty()) {
			throw new IllegalStateException("상품 임베딩 응답이 비어 있습니다.");
		}

		float[] embedding = new float[values.size()];
		for (int i = 0; i < values.size(); i++) {
			embedding[i] = values.get(i).floatValue();
		}
		return embedding;
	}

	private String buildInput(String title, String description, String roadAddress) {
		List<String> parts = new ArrayList<>();
		if (title != null && !title.isBlank()) {
			parts.add("상품명: " + title);
		}
		if (description != null && !description.isBlank()) {
			parts.add("설명: " + description);
		}
		if (roadAddress != null && !roadAddress.isBlank()) {
			parts.add("주소: " + roadAddress);
		}
		if (parts.isEmpty()) {
			throw new IllegalArgumentException("임베딩할 상품 텍스트가 없습니다.");
		}
		return String.join("\n", parts);
	}
}
