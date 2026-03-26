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
import jabaclass.product.application.usecase.SchedulesUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Products;
import jabaclass.product.domain.model.Schedules;
import jabaclass.product.domain.repository.SchedulesRepository;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import jabaclass.product.infrastructure.acl.dto.SellerRole;
import jabaclass.product.presentation.dto.request.CreateSchedulesRequestDto;
import jabaclass.product.presentation.dto.request.UpdateSchedulesRequestDto;
import jabaclass.product.presentation.dto.respose.SchedulesResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class SchedulesService implements SchedulesUseCase {

	private final SchedulesRepository schedulesRepository;
	private final ProductUseCase productUseCase;
	private final SellerRepository sellerRepository;
	private final ApplicationEventPublisher publisher;
	private final AuditorAwareService auditorAwareService;

	@Override
	@Transactional
	public SchedulesResponseDto create(CreateSchedulesRequestDto requestDto, UUID productId) {
		UUID sellerId = auditorAwareService.getCurrentAuditor()
			.orElseThrow(() -> new BusinessException(CommonErrorCode.EMPTY_USER));

		// seller 룰을 확인
		SellerResponseDto seller = findBySellerIdOrThrow(sellerId);
		SellerRole role = SellerRole.from(seller.role());
		if (role != SellerRole.SELLER) {
			throw new BusinessException(CommonErrorCode.NOT_SELLER);
		}

		// 상품 존재하는지 확인
		Products products = productUseCase.findByIdOrThrow(productId);
		// 본인 상품인지 확인
		productUseCase.matchProductAndSellerId(productId, seller.sellerId());

		// 날짜/시간 형태
		Schedules schedules = new Schedules();
		LocalDate date = schedules.fDt(requestDto.scheduleDt());
		LocalTime startTime = schedules.fTime(requestDto.startTime());
		LocalTime endTime = schedules.fTime(requestDto.endTime());

		// 날짜 검증 추가
		validatePastDate(date);
		// 시간 검증
		validateTime(startTime, endTime);

		// 중복 검증
		// 동시에 같은 날짜 시간대를 저장할경우를 대비해 lock(비관적 lock)
		List<Schedules> conflicts = schedulesRepository.findConflictSchedules(productId, date, startTime, endTime);

		if (!conflicts.isEmpty()) {
			throw new BusinessException(CommonErrorCode.SCHEDULE_CONFLICT);
		}

		Schedules save = Schedules.builder()
			.productId(productId)
			.scheduleDt(date)
			.startTime(startTime)
			.endTime(endTime)
			.status(requestDto.status())
			.maxCapacity(requestDto.maxCapacity())
			.build();

		Schedules saved = schedulesRepository.save(save);

		return SchedulesResponseDto.from(saved);
	}

	@Override
	public SchedulesResponseDto delete(CreateSchedulesRequestDto createSchedulesRequestDto) {
		return null;
	}

	@Override
	@Transactional
	public SchedulesResponseDto update(UpdateSchedulesRequestDto requestDto, UUID productId, UUID scheduleId) {
		UUID sellerId = auditorAwareService.getCurrentAuditor()
			.orElseThrow(() -> new BusinessException(CommonErrorCode.EMPTY_USER));

		// seller 룰을 확인
		SellerResponseDto seller = findBySellerIdOrThrow(sellerId);
		SellerRole role = SellerRole.from(seller.role());
		if (role != SellerRole.SELLER) {
			throw new BusinessException(CommonErrorCode.NOT_SELLER);
		}

		// 상품 존재하는지 확인
		productUseCase.findByIdOrThrow(productId);
		// 상품 일자 데이터가 존재하는지 확인
		Schedules schedules = findByIdOrThrow(scheduleId);
		// 본인 상품인지 확인
		productUseCase.matchProductAndSellerId(productId, seller.sellerId());
		// 시간 형태
		LocalTime startTime = schedules.fTime(requestDto.startTime());
		LocalTime endTime = schedules.fTime(requestDto.endTime());

		// 시간 검증
		validateTime(startTime, endTime);

		// 중복 검증
		// 동시에 같은 날짜 시간대를 저장할경우를 대비해 lock(비관적 lock)
		List<Schedules> conflicts = schedulesRepository.findConflictSchedulesNoId(productId, schedules.getScheduleDt(),
			startTime, endTime, scheduleId);

		if (!conflicts.isEmpty()) {
			throw new BusinessException(CommonErrorCode.SCHEDULE_CONFLICT);
		}

		// 업데이트
		schedules.changeStartTime(startTime);
		schedules.changeEndTime(endTime);
		schedules.changeStatus(requestDto.status());
		schedules.changeMaxCapacity(requestDto.maxCapacity());

		return SchedulesResponseDto.from(schedules);
	}

	@Override
	public SchedulesResponseDto select(CreateSchedulesRequestDto createSchedulesRequestDto) {
		return null;
	}

	// 상품 일자 존재 여부/단일 상품 일자 검색
	private Schedules findByIdOrThrow(UUID schedulesId) {
		return schedulesRepository.findById(schedulesId)
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

}
