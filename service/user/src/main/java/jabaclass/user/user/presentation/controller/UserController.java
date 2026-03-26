package jabaclass.user.user.presentation.controller;


import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.user.user.application.usercase.UserUseCase;
import jabaclass.user.user.presentation.dto.request.ChangeMyEmailRequestDto;
import jabaclass.user.user.presentation.dto.request.EmailCheckRequestDto;
import jabaclass.user.user.presentation.dto.request.RegisterUserRequestDto;
import jabaclass.user.user.presentation.dto.request.UpdateUserRequestDto;
import jabaclass.user.user.presentation.dto.request.UserBulkReadRequestDto;
import jabaclass.user.user.presentation.dto.response.EmailCheckResponseDto;
import jabaclass.user.user.presentation.dto.response.UserResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

	private final UserUseCase userUseCase;

	@PostMapping("/email-check")
	public ResponseEntity<EmailCheckResponseDto> checkEmailDuplicate(
		@Valid @RequestBody EmailCheckRequestDto request
	) {
		userUseCase.checkEmailDuplicate(request.email());
		return ResponseEntity.ok(new EmailCheckResponseDto(true));
	}

	@PostMapping("/register")
	public ResponseEntity<Void> register(
		@Valid @RequestBody RegisterUserRequestDto request
	) {
		userUseCase.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponseDto> getMyInfo(
		//@AuthenticationPrincipal CustomUserPrincipal principal //Todo
	) {
		UUID userId = UUID.randomUUID();
		return ResponseEntity.ok(userUseCase.getMyInfo(userId));
	}

	@PutMapping("/me")
	public ResponseEntity<Void> updateMyInfo(
		//@AuthenticationPrincipal CustomUserPrincipal principal, //Todo
		@Valid @RequestBody UpdateUserRequestDto request
	) {
		UUID userId = UUID.randomUUID();
		userUseCase.updateMyInfo(userId, request);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/me/email")
	public ResponseEntity<Void> changeEmail(
		//@AuthenticationPrincipal CustomUserPrincipal principal, //Todo
		@Valid @RequestBody ChangeMyEmailRequestDto request
	) {
		UUID userId = UUID.randomUUID();
		userUseCase.changeEmail(userId, request);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> withdraw(
		//@AuthenticationPrincipal CustomUserPrincipal principal //Todo
	) {
		UUID userId = UUID.randomUUID();
		userUseCase.withdraw(userId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/bulk")
	public ResponseEntity<List<UserResponseDto>> getUsersByIds(
		@Valid @RequestBody UserBulkReadRequestDto request
	) {
		return ResponseEntity
			.ok(userUseCase.getUsersByIds(request.userIds()));
	}
}