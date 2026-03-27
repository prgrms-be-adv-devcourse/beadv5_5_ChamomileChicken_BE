package jabaclass.product.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
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
import jabaclass.product.domain.model.status.OrderStatus;
import jabaclass.product.domain.model.status.ReservedStatus;
import jabaclass.product.domain.repository.ScheduleRepository;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import jabaclass.product.infrastructure.acl.dto.SellerRole;
import jabaclass.product.presentation.dto.request.CreateProductUserRequestDto;
import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
import jabaclass.product.presentation.dto.respose.DeleteScheduleResposeDto;
import jabaclass.product.presentation.dto.respose.OrderResponseDto;
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
	private final ApplicationEventPublisher publisher;
	private final AuditorAwareService auditorAwareService;
	private final ProductUserUseCase productUserUseCase;

	@Override
	@Transactional
	public SchedulesResponseDto create(CreateScheduleRequestDto requestDto, UUID productId) {
		SellerResponseDto seller = validateAndGetSeller();

		// 상품 존재하는지 확인
		Product product = productUseCase.findByIdOrThrow(productId);
		// 본인 상품인지 확인
		productUseCase.matchProductAndSellerId(productId, seller.sellerId());

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
		SellerResponseDto seller = validateAndGetSeller();

		// 상품 존재하는지 확인
		Product product = productUseCase.findByIdOrThrow(productId);
		// 상품 일자가 존재하는지
		Schedule schedule = findByIdOrThrow(scheduleId);
		// 본인 상품인지 확인
		productUseCase.matchProductAndSellerId(product.getId(), seller.sellerId());

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
		SellerResponseDto seller = validateAndGetSeller();

		// 상품 존재하는지 확인
		productUseCase.findByIdOrThrow(productId);
		// 상품 일자 데이터가 존재하는지 확인
		Schedule schedule = findByIdOrThrow(scheduleId);
		// 본인 상품인지 확인
		productUseCase.matchProductAndSellerId(productId, seller.sellerId());
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
	@Override
	@Transactional
	public OrderResponseDto verification(OrderRequestDto requestDto) {
		Schedule schedule = findByIdOrThrow(requestDto.productScheduleId());
		// 스케줄에 예약 가능 인원과 받아온 인원에 대한 체크
		ProductUserResponseDto saved = null;
		boolean status = true;
		int quantity = requestDto.quantity();

		// 해당 스케줄 예약자 수 확인
		List<ProductUser> list = productUserUseCase.innserUserList(requestDto.productScheduleId());
		int totalCount = list.stream()
			.filter(l -> OrderStatus.PAID.equals(l.getStatus()))
			.mapToInt(p -> p.getGuestCount())
			.sum();

		// 예약 가능 인원수
		int maxCapacity = schedule.getMaxCapacity() - totalCount;

		// DB 값 < 받아온 값
		if (quantity > maxCapacity) { // 예약이 안 되는 경우
			status = false;
		} else {
			CreateProductUserRequestDto dto = new CreateProductUserRequestDto(
				requestDto.productScheduleId(),
				requestDto.userId(),
				quantity,
				requestDto.status()
			);
			saved = productUserUseCase.create(dto);
			log.info(saved.id() + "");

		}

		Product product = productUseCase.findByIdOrThrow(schedule.getProductId());

		UUID puserId = saved == null ? null : saved.id();
		log.info(puserId + "");
		return OrderResponseDto.from(product, requestDto.quantity(), status, puserId);
	}

	// 재고 수정
	@Override
	@Transactional
	public void restoringInventory(OrderRequestDto requestDto) {
		ProductUser user = productUserUseCase.innerFindById(requestDto.productUserId());

		user.changeStatus(requestDto.status());
	}

	// 상품 일자 존재 여부/단일 상품 일자 검색
	private Schedule findByIdOrThrow(UUID schedulesId) {
		return scheduleRepository.findById(schedulesId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SCHDULES_NOT_FOUND));
	}

	// 로그인 계정 여부
	public SellerResponseDto findBySellerIdOrThrow(UUID sellerId) {
		SellerResponseDto sellerInfo = sellerRepository.findSeller(sellerId)
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

	private SellerResponseDto validateAndGetSeller() {
		UUID sellerId = auditorAwareService.getCurrentAuditor()
			.orElseThrow(() -> new BusinessException(CommonErrorCode.EMPTY_USER));
		SellerResponseDto seller = findBySellerIdOrThrow(sellerId);
		SellerRole role = SellerRole.from(seller.role());
		if (role != SellerRole.SELLER) {
			throw new BusinessException(CommonErrorCode.NOT_SELLER);
		}
		return seller;
	}

}
