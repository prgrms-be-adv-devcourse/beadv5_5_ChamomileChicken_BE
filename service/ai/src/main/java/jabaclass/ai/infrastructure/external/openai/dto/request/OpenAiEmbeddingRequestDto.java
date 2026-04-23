package jabaclass.ai.infrastructure.external.openai.dto.request;

public record OpenAiEmbeddingRequestDto(
	String model,
	String input,
	Integer dimensions,
	String encoding_format
) {
	public static OpenAiEmbeddingRequestDto of(String input, int dimensions) {
		return new OpenAiEmbeddingRequestDto(
			"text-embedding-3-small",
			input,
			dimensions,
			"float"
		);
	}
}
