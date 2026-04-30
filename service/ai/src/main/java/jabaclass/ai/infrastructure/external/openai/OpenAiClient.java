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
	private final ObjectMapper objectMapper;

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
		다음 사용자에게 각 클래스의 "개인화된 추천 이유"를 작성해줘.
		[사용자 벡터]
		%s
		
		[클래스 목록]
		%s
		
		조건:
		- 각 클래스별 추천 이유 1개 작성
		- 클래스의 핵심 특징 1개 이상 포함
		- 사용자 성향을 반영해 "개인화된 이유"처럼 작성
		- 30~50자, "~합니다" 형태
		
		- 아래 4가지 표현을 골고루 사용할 것:
		  1) "사진에 관심이 많으신 것 같아 ~ 추천합니다"
		  2) "~을 좋아하시는 성향을 고려해 ~ 적합합니다"
		  3) "~를 선호하실 것 같아 ~ 잘 맞습니다"
		  4) "~에 흥미를 느끼시는 것 같아 ~ 추천드립니다"
		
		- 모든 문장을 동일한 패턴으로 작성 금지
		- "사용자님께" 표현은 최대 1번만 사용
		
		예시:
		- 사진에 관심이 많으신 것 같아 감각적인 보정을 배울 수 있는 수업을 추천합니다
		- 향을 활용한 활동을 좋아하시는 것 같아 조향 클래스가 잘 맞을 것 같습니다
		
		출력(JSON):
		[
		  {"productId": "...", "reason": "..."}
		]
		""".formatted(
			java.util.Arrays.toString(userVector.vector()),
			candidateText
		);
	}

	private Map<UUID, String> parseResult(String content) {
		try {
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
