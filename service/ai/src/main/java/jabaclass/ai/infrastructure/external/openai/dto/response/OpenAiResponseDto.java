package jabaclass.ai.infrastructure.external.openai.dto.response;

import java.util.List;

import lombok.Getter;

@Getter
public class OpenAiResponseDto {

	private List<Choice> choices;

	@Getter
	public static class Choice {
		private Message message;
	}

	@Getter
	public static class Message {
		private String content;
	}
}
