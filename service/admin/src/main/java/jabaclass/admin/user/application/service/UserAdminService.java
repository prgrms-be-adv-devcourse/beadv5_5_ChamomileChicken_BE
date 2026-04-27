package jabaclass.admin.user.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.product.domain.model.OutboxEvent;
import jabaclass.admin.product.domain.repository.OutboxEventRepository;
import jabaclass.admin.product.infrastructure.outbox.EventType;
import jabaclass.admin.user.domain.dto.UserSearchCondition;
import jabaclass.admin.user.application.usecase.UserAdminUseCase;
import jabaclass.admin.user.domain.repository.UserAdminRepository;
import jabaclass.admin.user.infrastructure.kafka.AdminUserEvent;
import jabaclass.admin.user.presentation.dto.response.UserAdminResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService implements UserAdminUseCase {

	private final UserAdminRepository userAdminRepository;
	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional(readOnly = true)
	public Page<UserAdminResponseDto> getUsers(Pageable pageable, UserSearchCondition condition) {
		return userAdminRepository.findAll(condition, pageable)
			.map(UserAdminResponseDto::from);
	}

	@Override
	@Transactional(readOnly = true)
	public UserAdminResponseDto getUser(UUID userId) {
		return userAdminRepository.findById(userId)
			.map(UserAdminResponseDto::from)
			.orElseThrow(() -> new BusinessException(AdminErrorCode.USER_NOT_FOUND));
	}

	@Override
	@Transactional
	public void approveSeller(UUID userId) {
		userAdminRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(AdminErrorCode.USER_NOT_FOUND))
			.approveSeller();

		try {
			String payload = objectMapper.writeValueAsString(
				AdminUserEvent.sellerApproved(userId, LocalDateTime.now())
			);
			outboxEventRepository.save(OutboxEvent.create(
				"user",
				userId.toString(),
				EventType.USER_SELLER_APPROVED,
				payload
			));
		} catch (JsonProcessingException e) {
			log.error("OutboxEvent 직렬화 실패. userId={}", userId, e);
			throw new BusinessException(AdminErrorCode.OUTBOX_SERIALIZATION_FAILED);
		}
	}
}
