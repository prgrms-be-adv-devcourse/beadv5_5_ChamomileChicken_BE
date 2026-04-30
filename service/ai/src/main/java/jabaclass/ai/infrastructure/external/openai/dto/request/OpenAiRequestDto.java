package jabaclass.ai.infrastructure.external.openai.dto.request;

import java.util.List;

public record OpenAiRequestDto(
	String model,
	List<Message> messages
) {
	public static OpenAiRequestDto of(String prompt) {
		return new OpenAiRequestDto(
			"gpt-5.4-mini",
			List.of(new Message("user", prompt))
		);
	}

	public record Message(
		String role,
		String content
	) {}
}
