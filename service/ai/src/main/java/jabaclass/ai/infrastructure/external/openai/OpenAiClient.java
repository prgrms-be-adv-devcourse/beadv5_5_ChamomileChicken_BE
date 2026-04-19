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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
		String prompt = buildBatchPrompt(userVector,candidates);

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

		return parseResult(content);
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
	private String buildBatchPrompt(UserVector userVector, List<CandidateClassDto> candidates) {

		String candidateText = candidates.stream()
			.map(c -> """
            id: %s
            제목: %s
            설명: %s
            가격: %s
            위치: %s
            """.formatted(
				c.productId(),
				c.title(),
				c.description(),
				c.price(),
				c.roadAddress()
			))
			.reduce("", (a, b) -> a + "\n" + b);

		return """
        다음 사용자에게 각 클래스의 추천 이유를 작성해줘.

        [사용자 벡터]
        %s

        [클래스 목록]
        %s

        조건:
        - 각 클래스별 추천 이유 작성
        - 벡터 값은 직접 언급하지 말 것
        - 사용자 취향을 반영할 것
        - 20~60자
        - "~합니다" 형태

        출력 형식(JSON):
        [
          {"productId": "...", "reason": "..."},
          ...
        ]
        """.formatted(userVector.toString(), candidateText);
	}

	private Map<UUID, String> parseResult(String content) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();

			List<Map<String, String>> list =
				objectMapper.readValue(content, new TypeReference<>() {});

			Map<UUID, String> result = new HashMap<>();

			for (Map<String, String> item : list) {
				UUID productId = UUID.fromString(item.get("productId"));
				String reason = item.get("reason");
				result.put(productId, reason);
			}

			return result;

		} catch (Exception e) {
			throw new RuntimeException("GPT 응답 파싱 실패", e);
		}
	}
}
