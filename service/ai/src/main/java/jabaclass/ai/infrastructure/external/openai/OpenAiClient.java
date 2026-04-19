package jabaclass.ai.infrastructure.external.openai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.ai.application.dto.CandidateClassDto;
import jabaclass.ai.application.port.external.AiGatewayPort;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.infrastructure.external.openai.dto.request.OpenAiRequestDto;
import jabaclass.ai.infrastructure.external.openai.dto.response.OpenAiResponseDto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OpenAiClient implements AiGatewayPort {

	private final RestTemplate restTemplate;

	@Value("${openai.api.key}")
	private String apiKey;

	private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

	// Candidate 리스트 → 추천 이유 Map으로 변환
	@Override
	public Map<UUID, String> generateRecommendationReasons(
		UserVector userVector,
		List<CandidateClassDto> candidates
	) {

		Map<UUID, String> result = new HashMap<>();

		for (CandidateClassDto c : candidates) {

			String prompt = buildPrompt(c);

			OpenAiRequestDto requestBody = OpenAiRequestDto.of(prompt);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(apiKey);

			HttpEntity<OpenAiRequestDto> request =
				new HttpEntity<>(requestBody, headers);

			// OpenAI 서버로 요청
			OpenAiResponseDto response = restTemplate.postForObject(
				OPENAI_URL,
				request,
				OpenAiResponseDto.class
			);

			String content = extractContent(response);

			result.put(c.productId(), content);
		}

		return result;
	}

	// GPT 결과에서 텍스트만 꺼냄
	private String extractContent(OpenAiResponseDto response) {
		if (response == null ||
			response.getChoices() == null ||
			response.getChoices().isEmpty()) {
			return "추천 이유를 생성하지 못했습니다.";
		}
		return response.getChoices().get(0).getMessage().getContent();
	}

	// 프롬프트 작성
	private String buildPrompt(CandidateClassDto c) {
		return """
            다음 클래스를 사용자에게 추천하는 이유를 한 문장으로 작성해줘.

            제목: %s
            설명: %s
            가격: %s
            위치: %s

            조건:
            - 20자 이상 60자 이하
            - 자연스럽고 설득력 있게
            - "~합니다" 형태로 끝날 것
            """.formatted(
			c.title(),
			c.description(),
			c.price(),
			c.roadAddress()
		);
	}
}
