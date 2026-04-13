package jabaclass.product.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.application.usecase.ProductUserUseCase;
import jabaclass.product.application.usecase.ScheduleUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.ProductUser;
import jabaclass.product.domain.model.Schedule;
import jabaclass.product.domain.model.status.ReservationStatus;
import jabaclass.product.domain.model.status.ReservedStatus;
import jabaclass.product.domain.repository.ScheduleRepository;
import jabaclass.product.infrastructure.acl.dto.SellerRole;
import jabaclass.product.infrastructure.acl.dto.response.UserResponseDto;
import jabaclass.product.presentation.dto.request.CreateProductUserRequestDto;
import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
import jabaclass.product.presentation.dto.respose.AvailabilityScheduleResponseDto;
import jabaclass.product.presentation.dto.respose.DeleteScheduleResposeDto;
import jabaclass.product.presentation.dto.respose.OrderResponseDto;
import jabaclass.product.presentation.dto.respose.OrderValid;
import jabaclass.product.presentation.dto.respose.ProductUserResponseDto;
import jabaclass.product.presentation.dto.respose.SchedulesResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ScheduleService implements ScheduleUseCase {

	private final ScheduleRepository scheduleRepository;
	private final ProductUseCase productUseCase;
	private final SellerRepository sellerRepository;
	private final AuditorAwareService auditorAwareService;
	private final ProductUserUseCase productUserUseCase;

	@Override
	@Transactional
	public SchedulesResponseDto create(CreateScheduleRequestDto requestDto, UUID productId) {
		UserResponseDto seller = validateAndGetSeller();

		// 상품 존재하는지 확인
		Product product = productUseCase.findByIdOrThrow(productId);
		// 본인 상품인지 확인
		productUseCase.matchProductAndSellerId(productId, seller.userId());

		// 날짜/시간 형태
		Schedule schedule = new Schedule();
		LocalDate date = schedule.fDt(requestDto.scheduleDt());
		LocalTime startTime = schedule.fTime(requestDto.startTime());
		LocalTime endTime = schedule.fTime(requestDto.endTime());

		// 날짜 검증 추가
		validatePastDate(date);
		// 시간 검증
		validateTime(startTime, endTime);

		// 중복 검증
		// 동시에 같은 날짜 시간대를 저장할경우를 대비해 lock(비관적 lock)
		List<Schedule> conflicts = scheduleRepository.findConflictSchedules(productId, date, startTime, endTime);

		if (!conflicts.isEmpty()) {
			throw new BusinessException(CommonErrorCode.SCHEDULE_CONFLICT);
		}

		Schedule save = Schedule.builder()
			.productId(productId)
			.scheduleDt(date)
			.startTime(startTime)
			.endTime(endTime)
			.status(requestDto.status())
			.maxCapacity(product.getMaxCapacity())
			.build();

		Schedule saved = scheduleRepository.save(save);

		return SchedulesResponseDto.from(saved);
	}

	@Override
	@Transactional
	public DeleteScheduleResposeDto delete(UUID productId, UUID scheduleId) {
		UserResponseDto seller = validateAndGetSeller();

		// 상품 존재하는지 확인
		Product product = productUseCase.findByIdOrThrow(productId);
		// 상품 일자가 존재하는지
		Schedule schedule = findByIdOrThrow(scheduleId);
		// 본인 상품인지 확인
		productUseCase.matchProductAndSellerId(product.getId(), seller.userId());

		if (!schedule.getProductId().equals(product.getId())) {
			throw new BusinessException(CommonErrorCode.MATCH_FAIL);
		}

		schedule.changeStatus(ReservedStatus.CLOSED);
		schedule.changeDelete();

		return DeleteScheduleResposeDto.from(scheduleId, ReservedStatus.CLOSED);
	}

	@Override
	@Transactional
	public SchedulesResponseDto update(UpdateScheduleRequestDto requestDto, UUID productId, UUID scheduleId) {
		UserResponseDto seller = validateAndGetSeller();

		// 상품 존재하는지 확인
		productUseCase.findByIdOrThrow(productId);
		// 상품 일자 데이터가 존재하는지 확인
		Schedule schedule = findByIdOrThrow(scheduleId);
		// 본인 상품인지 확인
		productUseCase.matchProductAndSellerId(productId, seller.userId());
		// 시간 형태
		LocalTime startTime = schedule.fTime(requestDto.startTime());
		LocalTime endTime = schedule.fTime(requestDto.endTime());

		// 시간 검증
		validateTime(startTime, endTime);

		// 중복 검증
		// 동시에 같은 날짜 시간대를 저장할경우를 대비해 lock(비관적 lock)
		List<Schedule> conflicts = scheduleRepository.findConflictSchedulesNoId(productId, schedule.getScheduleDt(),
			startTime, endTime, scheduleId);

		if (!conflicts.isEmpty()) {
			throw new BusinessException(CommonErrorCode.SCHEDULE_CONFLICT);
		}

		// 업데이트
		schedule.changeStartTime(startTime);
		schedule.changeEndTime(endTime);
		schedule.changeStatus(requestDto.status());
		schedule.changeMaxCapacity(requestDto.maxCapacity());

		return SchedulesResponseDto.from(schedule);
	}

	@Override
	public List<SchedulesResponseDto> schedulesList(UUID productId) {
		List<Schedule> list = scheduleRepository.findByProductIdAndDeleteDtIsNull(productId);

		return list.stream()
			.map(SchedulesResponseDto::from)
			.toList();
	}

	// 상품 검증 -> 예약 가능 상태 return
	// 2026-04-09 수정
	@Override
	@Transactional
	public OrderResponseDto verification(OrderRequestDto requestDto) {
		// 스케줄 검색
		Schedule schedule = findByIdOrThrow(requestDto.productScheduleId());
		// 상품 검색
		Product product = productUseCase.findByIdOrThrow(schedule.getProductId());

		// 가격 검증 => 실패의 경우
		if (!product.getPrice().equals(requestDto.price())) {
			return OrderResponseDto.from(product, requestDto.quantity(), OrderValid.PRICE_MISMATCH, null);
		}

		// 스케줄에 예약 가능 인원과 받아온 인원에 대한 체크
		ProductUserResponseDto saved = null;
		int quantity = requestDto.quantity();

		// 예약 검증 및 재고 차감/재고가 0일 경우 상태값 변경
		int count = scheduleRepository.verification(quantity, schedule.getId());

		// count 0일경우 실패, 1일경우 성공
		if (count == 0) { // 실패
			return OrderResponseDto.from(product, requestDto.quantity(), OrderValid.OUT_OF_STOCK, null);
		} else { // 성공
			CreateProductUserRequestDto dto = new CreateProductUserRequestDto(
				requestDto.productScheduleId(),
				requestDto.userId(),
				quantity,
				ReservationStatus.RESERVED
			);
			saved = productUserUseCase.create(dto);
		}

		UUID puserId = saved == null ? null : saved.id();

		return OrderResponseDto.from(product, requestDto.quantity(), OrderValid.OK, puserId);
	}

	// 2026-04-09 수정 => 상태값 수정
	@Override
	@Transactional
	public OrderValid restoringInventory(UUID productUserId, ReservationStatus orderStatus) {
		ProductUser user = productUserUseCase.innerFindById(productUserId);
		findByIdOrThrow(user.getProductScheduleId());

		OrderValid result = null;
		int count = scheduleRepository.updateStatus(user.getId(), orderStatus);
		if (count == 0)
			result = OrderValid.MODI_FAIL;
		else
			result = OrderValid.OK;
		return result;
	}

	@Override
	@Transactional
	public void confirmReservation(UUID productUserId) {
		ProductUser user = productUserUseCase.innerFindById(productUserId);
		if (user.getStatus() == ReservationStatus.CONFIRMED) {
			return;
		}

		user.changeStatus(ReservationStatus.CONFIRMED);
	}

	@Override
	@Transactional
	public void releaseReservation(UUID productUserId) {
		ProductUser user = productUserUseCase.innerFindById(productUserId);
		if (user.getStatus() == ReservationStatus.RELEASED || user.getStatus() == ReservationStatus.REFUNDED) {
			return;
		}

		Schedule schedule = findByIdOrThrow(user.getProductScheduleId());
		Product product = productUseCase.findByIdOrThrow(schedule.getProductId());

		int restored = scheduleRepository.restoreCapacity(
			schedule.getId(),
			user.getGuestCount(),
			product.getMaxCapacity()
		);
		if (restored == 0) {
			throw new RuntimeException("재고 복구 실패");
		}

		user.changeStatus(ReservationStatus.RELEASED);
		refreshScheduleStatus(schedule);
	}

	@Override
	@Transactional
	public void refundReservation(UUID productUserId) {
		ProductUser user = productUserUseCase.innerFindById(productUserId);
		if (user.getStatus() == ReservationStatus.REFUNDED) {
			return;
		}

		Schedule schedule = findByIdOrThrow(user.getProductScheduleId());
		Product product = productUseCase.findByIdOrThrow(schedule.getProductId());

		int restored = scheduleRepository.restoreCapacity(
			schedule.getId(),
			user.getGuestCount(),
			product.getMaxCapacity()
		);
		if (restored == 0) {
			throw new RuntimeException("재고 복구 실패");
		}

		user.changeStatus(ReservationStatus.REFUNDED);
		refreshScheduleStatus(schedule);
	}

	@Override
	public int claimRestore(UUID productUserId, ReservationStatus restoringStatus) {
		return scheduleRepository.claimRestore(productUserId, restoringStatus);
	}

	@Override
	public int restoreCapacity(UUID scheduleId, int quantity, int capacity) {
		return scheduleRepository.restoreCapacity(scheduleId, quantity, capacity);
	}

	@Override
	public Schedule findByProductUserId(UUID productUserId) {
		return scheduleRepository.findByProductUserId(productUserId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SCHDULES_NOT_FOUND));
	}

	@Override
	public AvailabilityScheduleResponseDto availabilitySchedule(UUID scheduleId) {
		Schedule schedule = findByIdOrThrow(scheduleId);

		// 해당 스케줄 예약자 수 확인
		List<ProductUser> list = productUserUseCase.innserUserList(schedule.getId());
		int reservedCount = list.stream()
			.filter(l -> ReservationStatus.CONFIRMED.equals(l.getStatus())
				|| ReservationStatus.RESERVED.equals(l.getStatus()))
			.mapToInt(p -> p.getGuestCount())
			.sum();

		int remainingCount = schedule.getMaxCapacity() - reservedCount;

		return AvailabilityScheduleResponseDto.from(schedule, reservedCount, remainingCount);
	}

	// 상품 일자 존재 여부/단일 상품 일자 검색
	private Schedule findByIdOrThrow(UUID schedulesId) {
		return scheduleRepository.findByIdAndDeleteDtIsNull(schedulesId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SCHDULES_NOT_FOUND));
	}

	// 로그인 계정 여부
	public UserResponseDto findBySellerIdOrThrow(UUID sellerId) {
		UserResponseDto sellerInfo = sellerRepository.findSeller(sellerId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SELLER_NOT_FOUND));

		return sellerInfo;
	}

	// 이전 날짜 확인
	private void validatePastDate(LocalDate scheduleDt) {
		if (scheduleDt.isBefore(LocalDate.now())) {
			throw new BusinessException(CommonErrorCode.INVALID_SCHEDULE_DATE);
		}
	}

	// 시작 시간, 종료 시간 확인
	private void validateTime(LocalTime start, LocalTime end) {
		if (!start.isBefore(end)) {
			throw new BusinessException(CommonErrorCode.INVALID_TIME_RANGE);
		}
	}

	private void refreshScheduleStatus(Schedule schedule) {
		int remainingCapacity = calculateRemainingCapacity(schedule.getId(), schedule.getMaxCapacity());

		if (remainingCapacity <= 0) {
			schedule.changeStatus(ReservedStatus.FULL);
			return;
		}

		if (schedule.getStatus() == ReservedStatus.FULL) {
			schedule.changeStatus(ReservedStatus.AVAILABLE);
		}
	}

	private int calculateRemainingCapacity(UUID scheduleId, int maxCapacity) {
		int reservedCount = productUserUseCase.innserUserList(scheduleId).stream()
			.filter(productUser -> productUser.getStatus() == ReservationStatus.CONFIRMED
				|| productUser.getStatus() == ReservationStatus.RESERVED)
			.mapToInt(ProductUser::getGuestCount)
			.sum();

		return maxCapacity - reservedCount;
	}

	private UserResponseDto validateAndGetSeller() {
		UUID sellerId = auditorAwareService.getCurrentAuditor()
			.orElseThrow(() -> new BusinessException(CommonErrorCode.EMPTY_USER));
		UserResponseDto seller = findBySellerIdOrThrow(sellerId);
		SellerRole role = SellerRole.from(seller.role());
		if (role != SellerRole.SELLER) {
			throw new BusinessException(CommonErrorCode.NOT_SELLER);
		}
		return seller;
	}

}
