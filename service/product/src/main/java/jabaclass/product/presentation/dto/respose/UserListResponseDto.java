package jabaclass.product.presentation.dto.respose;

import java.util.List;

import jabaclass.product.infrastructure.acl.dto.response.UserResponseDto;

public record UserListResponseDto(
	List<UserResponseDto> data
) {
}
