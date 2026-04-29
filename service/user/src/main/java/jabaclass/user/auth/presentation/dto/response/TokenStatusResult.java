package jabaclass.user.auth.presentation.dto.response;

public record TokenStatusResult(boolean tokenValid, String reason) {

	public static TokenStatusResult valid() {
		return new TokenStatusResult(true, null);
	}

	public static TokenStatusResult blacklisted() {
		return new TokenStatusResult(false, "BLACKLISTED");
	}

	public static TokenStatusResult forceLogout() {
		return new TokenStatusResult(false, "FORCE_LOGOUT");
	}
}
