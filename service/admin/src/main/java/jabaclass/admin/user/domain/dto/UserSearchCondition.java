package jabaclass.admin.user.domain.dto;

public record UserSearchCondition(
	String role,
	String name,
	String email
) {
}