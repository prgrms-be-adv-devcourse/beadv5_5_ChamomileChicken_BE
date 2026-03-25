package jabaclass.product.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.ProductStatus;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import jabaclass.product.infrastructure.acl.dto.SellerRole;
import jabaclass.product.infrastructure.event.dto.ProductEventResponseDto;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.request.SearchProductRequestDto;
import jabaclass.product.presentation.dto.request.UpdateProductRequestDto;
import jabaclass.product.presentation.dto.respose.DeleteProductResposeDto;
import jabaclass.product.presentation.dto.respose.ProductResponseDto;
import jabaclass.product.presentation.dto.respose.SearchProductResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductService implements ProductUseCase {
	private final ProductRepository productRepository;
	private final SellerRepository sellerRepository;
	private final ApplicationEventPublisher publisher;

	// 테스트용입니다.
	private static final UUID SELLER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

	@Override
	@Transactional
	public ProductResponseDto create(CreateProductRequestDto requestDto) {
		// seller 룰을 확인
		SellerResponseDto seller = findBySellerIdOrThrow(requestDto.sellerId());

		Product product = Product.builder()
			.sellerId(requestDto.sellerId())
			.title(requestDto.title())
			.maxCapacity(requestDto.maxCapacity())
			.description(requestDto.description())
			.descriptionImage(requestDto.descriptionImage())
			.price(requestDto.price())
			.status(requestDto.status())
			.build();

		Product saved = productRepository.save(product);

		publisher.publishEvent(new ProductEventResponseDto(saved.getId()));
		return ProductResponseDto.form(saved, seller.sellerName());
	}

	@Override
	@Transactional
	public ProductResponseDto update(UpdateProductRequestDto requestDto, UUID productId) {
		//	UUID sellerId = auditorAwareService.getCurrentAuditor().get();
		// sellerId를 확인
		SellerResponseDto seller = findBySellerIdOrThrow(SELLER_ID);
		// 상품 존재하는지 확인
		Product product = findByIdOrThrow(productId);
		// 본인 상품인지 확인
		matchProductAndSellerId(productId, seller.sellerId());

		product.changeTitle(requestDto.title());
		product.changeMaxCapacity(requestDto.maxCapacity());
		product.changeDescription(requestDto.description());
		product.changeDescriptionImage(requestDto.descriptionImage());
		product.changePrice(requestDto.price());
		product.changeStatus(requestDto.status());

		return ProductResponseDto.form(product, seller.sellerName());
	}

	@Override
	@Transactional
	public DeleteProductResposeDto delete(UUID productId) {
		//UUID sellerId = auditorAwareService.getCurrentAuditor().get();
		// sellerId를 확인
		SellerResponseDto seller = findBySellerIdOrThrow(SELLER_ID);
		// 상품 존재하는지 확인
		Product product = findByIdOrThrow(productId);
		// 본인 상품인지 확인
		matchProductAndSellerId(productId, seller.sellerId());

		product.changeDelete();

		return DeleteProductResposeDto.form(productId, ProductStatus.DISABLE);
	}

	@Override
	public SearchProductResponseDto searchAll(SearchProductRequestDto requestDto) {
		// 페이징 설정
		Pageable pageable = PageRequest.of(requestDto.thisPage(), requestDto.pageSize());

		Page<Product> page;

		// 페이징 및 키워드를 조건으로 가져온 상품 리스트
		if (requestDto.keyword() == null || requestDto.keyword().isBlank()) {
			page = productRepository.findByStatusAndDeleteDtIsNull(requestDto.status(), pageable);
		} else {
			page = productRepository.findByStatusAndTitleContainingAndDeleteDtIsNull(requestDto.status(),
				requestDto.keyword(),
				pageable);
		}

		// 검색해온 상품의 user uuid를 List에 담는 작업
		List<UUID> uuidList = page.getContent()
			.stream()
			.map(Product::getSellerId)
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
		List<ProductResponseDto> resultPro = page.getContent().stream()
			.map(p -> ProductResponseDto.listForm(
				p,
				sellerMap
			))
			.toList();

		return SearchProductResponseDto.form(page, resultPro);
	}

	@Override
	public ProductResponseDto searchById(UUID productId) {
		Product product = findByIdOrThrow(productId);

		// sellerId를 확인
		SellerResponseDto seller = findBySellerIdOrThrow(product.getSellerId());

		return ProductResponseDto.form(product, seller.sellerName());
	}

	// 상품 존재 여부/단일 상품 검색
	private Product findByIdOrThrow(UUID sellerId) {
		return productRepository.findById(sellerId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.PRODUCT_NOT_FOUND));
	}

	// 로그인 계정 여부 및 판매자 룰 확인
	private SellerResponseDto findBySellerIdOrThrow(UUID sellerId) {
		SellerResponseDto sellerInfo = sellerRepository.findSeller(sellerId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SELLER_NOT_FOUND));

		SellerRole role = SellerRole.from(sellerInfo.role());

		if (role != SellerRole.SELLER) {
			throw new BusinessException(CommonErrorCode.NOT_SELLER);
		}

		return sellerInfo;
	}

	// 해당 상품 보유자인지 확인
	private Product matchProductAndSellerId(UUID productId, UUID sellerId) {
		return productRepository.findByIdAndSellerId(productId, sellerId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.MATCH_FAIL));
	}

}
