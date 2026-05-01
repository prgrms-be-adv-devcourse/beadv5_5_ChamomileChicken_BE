package jabaclass.user.auth.application.usecase;

import java.util.UUID;

import jabaclass.user.auth.presentation.dto.response.TokenStatusResult;

public interface TokenStatusUseCase {

	TokenStatusResult checkTokenStatus(String token, UUID userId, long tokenIssuedAtMillis);
}
