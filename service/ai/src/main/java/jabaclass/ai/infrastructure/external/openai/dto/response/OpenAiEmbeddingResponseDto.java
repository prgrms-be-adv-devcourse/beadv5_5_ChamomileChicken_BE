package jabaclass.ai.infrastructure.external.openai.dto.response;

import java.util.List;

import lombok.Getter;

@Getter
public class OpenAiEmbeddingResponseDto {

	private List<EmbeddingData> data;

	@Getter
	public static class EmbeddingData {
		private List<Double> embedding;
	}
}
