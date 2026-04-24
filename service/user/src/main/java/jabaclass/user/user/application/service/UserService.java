package jabaclass.user.user.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.mail.application.usecase.EmailVerificationUseCase;
import jabaclass.user.user.application.exception.UserErrorCode;
import jabaclass.user.user.application.usercase.UserUseCase;
import jabaclass.user.user.domain.model.SellerSettlementAccount;
import jabaclass.user.user.domain.model.SocialType;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.model.UserRole;
import jabaclass.user.user.domain.repository.SellerSettlementAccountRepository;
import jabaclass.user.user.domain.repository.UserRepository;
import jabaclass.user.user.presentation.dto.request.ChangeMyEmailRequestDto;
import jabaclass.user.user.presentation.dto.request.RegisterUserRequestDto;
import jabaclass.user.user.presentation.dto.request.UpsertSellerSettlementAccountRequestDto;
import jabaclass.user.user.presentation.dto.request.UpdateUserRequestDto;
import jabaclass.user.user.presentation.dto.response.SellerSettlementAccountResponseDto;
import jabaclass.user.user.presentation.dto.response.SellerSettlementDetailResponseDto;
import jabaclass.user.user.presentation.dto.response.UserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService implements UserUseCase {

	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
	private final SellerSettlementAccountRepository sellerSettlementAccountRepository;
	private final EmailVerificationUseCase emailVerificationUseCase;


	@Override
	public void checkEmailDuplicate(String email) {
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
		}
	}

	@Override
	@Transactional
	public void register(RegisterUserRequestDto request) {
		checkEmailDuplicate(request.email());
		emailVerificationUseCase.validateVerifiedToken(request.email(), request.verifiedToken());

		User user = User.builder()
			.name(request.name())
			.email(request.email())
			.password(passwordEncoder.encode(request.password()))
			.phone(request.phone())
			.role(UserRole.USER)
			.socialType(SocialType.SYSTEM)
			.deposit(BigDecimal.ZERO)
			.build();

		saveUser(user);
	}

	@Override
	public UserResponseDto getMyInfo(UUID userId) {
		log.info("UserService.getMyInfo start userId={}", userId);
		User user = getUser(userId);
		log.info("UserService.getMyInfo found userId={}, email={}, role={}",
			user.getId(), user.getEmail(), user.getRole());
		return UserResponseDto.from(user);
	}

	@Override
	@Transactional
	public void updateMyInfo(UUID userId, UpdateUserRequestDto request) {
		User user = getUser(userId);
		user.updateProfile(request.name(), request.phone());
	}

	@Override
	@Transactional
	public void changeEmail(UUID userId, ChangeMyEmailRequestDto request) {
		checkEmailDuplicate(request.newEmail());
		emailVerificationUseCase.validateVerifiedToken(request.newEmail(), request.verifiedToken());

		User user = getUser(userId);
		user.changeEmail(request.newEmail());
	}

	@Override
	@Transactional
	public void withdraw(UUID userId) {
		User user = getUser(userId);
		userRepository.delete(user);
	}

	@Override
	@Transactional
	public SellerSettlementAccountResponseDto upsertSellerSettlementAccount(
		UUID userId,
		String currentUserRole,
		UpsertSellerSettlementAccountRequestDto request
	) {
		User user = getUser(userId);
		validateSellerSettlementAccountAccess(currentUserRole, user);

		SellerSettlementAccount account = sellerSettlementAccountRepository.findByUserId(userId)
			.map(existing -> {
				existing.updateAccount(
					request.bankCode(),
					request.accountNumber(),
					request.accountHolder(),
					request.active()
				);
				return existing;
			})
			.orElseGet(() -> SellerSettlementAccount.register(
				userId,
				request.bankCode(),
				request.accountNumber(),
				request.accountHolder(),
				request.active()
			));

		return SellerSettlementAccountResponseDto.from(sellerSettlementAccountRepository.save(account));
	}

	@Override
	public List<UserResponseDto> getUsersByIds(List<UUID> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return List.of();
		}

		List<UUID> distinctUserIds = userIds.stream()
			.distinct()
			.toList();

		Map<UUID, User> userMap = userRepository.findAllByIds(distinctUserIds).stream()
			.collect(Collectors.toMap(User::getId, Function.identity()));

		return distinctUserIds.stream()
			.map(userMap::get)
			.filter(Objects::nonNull)
			.map(UserResponseDto::from)
			.toList();
	}

	@Override
	public List<SellerSettlementDetailResponseDto> getSellerDetailsByIds(List<UUID> sellerIds) {
		if (sellerIds == null || sellerIds.isEmpty()) {
			return List.of();
		}

		List<UUID> distinctSellerIds = sellerIds.stream()
			.distinct()
			.toList();

		Map<UUID, SellerSettlementAccount> accountMap = sellerSettlementAccountRepository.findAllByUserIds(distinctSellerIds)
			.stream()
			.collect(Collectors.toMap(SellerSettlementAccount::getUserId, Function.identity()));

		Map<UUID, User> sellerMap = userRepository.findAllByIds(distinctSellerIds).stream()
			.filter(user -> user.getRole() == UserRole.SELLER)
			.collect(Collectors.toMap(User::getId, Function.identity()));

		return distinctSellerIds.stream()
			.map(sellerMap::get)
			.filter(Objects::nonNull)
			.map(seller -> {
				SellerSettlementAccount account = accountMap.get(seller.getId());
				return new SellerSettlementDetailResponseDto(
					seller.getId(),
					seller.getRole().name(),
					account != null,
					account != null && account.isActive()
				);
			})
			.toList();
	}

	@Override
	public List<SellerSettlementAccountResponseDto> getSellerSettlementAccountsByIds(List<UUID> sellerIds) {
		if (sellerIds == null || sellerIds.isEmpty()) {
			return List.of();
		}

		List<UUID> distinctSellerIds = sellerIds.stream()
			.distinct()
			.toList();

		Map<UUID, User> sellerMap = userRepository.findAllByIds(distinctSellerIds).stream()
			.filter(user -> user.getRole() == UserRole.SELLER)
			.collect(Collectors.toMap(User::getId, Function.identity()));

		return sellerSettlementAccountRepository.findAllByUserIds(distinctSellerIds).stream()
			.filter(account -> sellerMap.containsKey(account.getUserId()))
			.map(SellerSettlementAccountResponseDto::from)
			.toList();
	}

	private User getUser(UUID userId) {
		log.info("UserService.getUser lookup userId={}", userId);
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
	}

	private void validateSellerSettlementAccountAccess(String currentUserRole, User user) {
		UserRole role = resolveRole(currentUserRole);
		if (!isSellerSettlementAccountAllowed(role) || !isSellerSettlementAccountAllowed(user.getRole())) {
			throw new BusinessException(UserErrorCode.SELLER_SETTLEMENT_ACCOUNT_ACCESS_DENIED);
		}
	}

	private UserRole resolveRole(String currentUserRole) {
		if (currentUserRole == null) {
			throw new BusinessException(UserErrorCode.SELLER_SETTLEMENT_ACCOUNT_ACCESS_DENIED);
		}
		try {
			return UserRole.valueOf(currentUserRole);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(UserErrorCode.SELLER_SETTLEMENT_ACCOUNT_ACCESS_DENIED);
		}
	}

	private boolean isSellerSettlementAccountAllowed(UserRole role) {
		return role == UserRole.SELLER || role == UserRole.ADMIN;
	}

	private void saveUser(User user) {
		try {
			userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException e) {
			if (isEmailUniqueViolation(e)) {
				throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
			}
			throw e;
		}
	}

	// Todo 이 메서드 역할을 생각하여 별도 클래스로 분리 리펙터링 고려
	private boolean isEmailUniqueViolation(DataIntegrityViolationException e) {
		Throwable cause = e;
		while (cause != null) {
			String message = cause.getMessage();
			if (message != null && message.contains("uk_users_email")) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}
}
