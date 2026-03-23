package jabaclass.user.user.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.user.user.application.usercase.UserUseCase;
import jabaclass.user.user.presentation.dto.response.UserResponseDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserUseCase userUseCase;

	@GetMapping("/me")
	public ResponseEntity<UserResponseDto> getMyInfo(
	) {
		UUID requesterId = UUID.randomUUID(); // Todo 추후 인증 객체에서 id 추출

		UserResponseDto response = userUseCase.getMyInfo(requesterId);
		return ResponseEntity.ok(response);
	}
}