package jabaclass.product.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.usecase.ProductUserUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.ProductUser;
import jabaclass.product.domain.repository.ProductUserRepository;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import jabaclass.product.infrastructure.acl.dto.SellerRole;
import jabaclass.product.presentation.dto.request.CreateProductUserRequestDto;
import jabaclass.product.presentation.dto.respose.ProductUserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductUserService implements ProductUserUseCase {

	private final ProductUserRepository pUserRepository;
	private final AuditorAwareService auditorAwareService;
	private final SellerRepository sellerRepository;

	@Override
	public List<ProductUserResponseDto> getUser(UUID scheduleId) {
		validateAndGetSeller();
		List<ProductUser> list = pUserRepository.findByProductScheduleId(scheduleId);

		// 검색해온 상품의 user uuid를 List에 담는 작업
		List<UUID> uuidList = list
			.stream()
			.map(ProductUser::getUserId)
			.distinct()
			.toList();

		// seller List 가져오기
		List<SellerResponseDto> sellerList = sellerRepository.findSellerList(uuidList)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SELLER_NOT_FOUND));

		// seller를 map으로 변환
		Map<UUID, String> sellerMap =
			sellerList.stream()
				.collect(Collectors.toMap(
						SellerResponseDto::sellerId,
						SellerResponseDto::sellerName
					)
				);

		// sellerId를 가져온 기준으로 sellerNmae set
		List<ProductUserResponseDto> resultPro = list.stream()
			.map(p -> ProductUserResponseDto.listFrom(
				p,
				sellerMap
			))
			.toList();

		return resultPro;
	}

	@Override
	public ProductUserResponseDto create(CreateProductUserRequestDto requestDto) {
		ProductUser user = ProductUser.builder()
			.productScheduleId(requestDto.productScheduleId())
			.userId(requestDto.userId())
			.guestCount(requestDto.guestCount())
			.status(requestDto.status())
			.build();

		SellerResponseDto seller = findBySellerIdOrThrow(user.getUserId());

		ProductUser saved = pUserRepository.save(user);

		return ProductUserResponseDto.from(saved, seller.sellerName());
	}

	// 외부용
	@Override
	public ProductUserResponseDto findById(UUID id) {
		ProductUser user = pUserRepository.findById(id)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_BUY_USER));

		return ProductUserResponseDto.from(user, null);
	}

	//내부용
	@Override
	public ProductUser innerFindById(UUID id) {

		return pUserRepository.findById(id)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_BUY_USER));
	}

	@Override
	public List<ProductUser> innserUserList(UUID scheduleId) {
		List<ProductUser> list = pUserRepository.findByProductScheduleId(scheduleId);
		return list;
	}

	// 로그인 계정 여부
	public SellerResponseDto findBySellerIdOrThrow(UUID sellerId) {
		SellerResponseDto sellerInfo = sellerRepository.findSeller(sellerId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SELLER_NOT_FOUND));

		return sellerInfo;
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
