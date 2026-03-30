package jabaclass.product.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.infrastructure.acl.dto.SellerRole;
import jabaclass.product.infrastructure.acl.dto.response.UserResponseDto;
import jabaclass.product.infrastructure.event.dto.ProductEventResponseDto;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.request.SearchProductRequestDto;
import jabaclass.product.presentation.dto.request.UpdateProductRequestDto;
import jabaclass.product.presentation.dto.respose.DeleteProductResposeDto;
import jabaclass.product.presentation.dto.respose.ProductResponseDto;
import jabaclass.product.presentation.dto.respose.ProductSettlementItemResponseDto;
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
	private final AuditorAwareService auditorAwareService;

	@Override
	@Transactional
	public ProductResponseDto create(CreateProductRequestDto requestDto) {
		// seller 룰을 확인
		UserResponseDto seller = validateAndGetSeller();

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
		return ProductResponseDto.from(saved, seller.name());
	}

	@Override
	@Transactional
	public ProductResponseDto update(UpdateProductRequestDto requestDto, UUID productId) {
		// seller 룰을 확인
		UserResponseDto seller = validateAndGetSeller();

		// 상품 존재하는지 확인
		Product product = findByIdOrThrow(productId);
		// 본인 상품인지 확인
		matchProductAndSellerId(productId, seller.userId());

		product.changeTitle(requestDto.title());
		product.changeMaxCapacity(requestDto.maxCapacity());
		product.changeDescription(requestDto.description());
		product.changeDescriptionImage(requestDto.descriptionImage());
		product.changePrice(requestDto.price());
		product.changeStatus(requestDto.status());

		return ProductResponseDto.from(product, seller.name());
	}

	@Override
	@Transactional
	public DeleteProductResposeDto delete(UUID productId) {
		// seller 룰을 확인
		UserResponseDto seller = validateAndGetSeller();

		// 상품 존재하는지 확인
		Product product = findByIdOrThrow(productId);
		// 본인 상품인지 확인
		matchProductAndSellerId(productId, seller.userId());

		product.changeStatus(ProductStatus.DISABLE);
		product.changeDelete();

		return DeleteProductResposeDto.from(productId, ProductStatus.DISABLE);
	}

	@Override
	public SearchProductResponseDto searchAll(SearchProductRequestDto requestDto) {

		// 페이징 설정
		Pageable pageable = PageRequest.of(requestDto.thisPage(), requestDto.pageSize());

		List<Product> products = new ArrayList<>();
		// = new PageImpl<>(products)
		Page<Product> page;

		// 페이징 및 키워드를 조건으로 가져온 상품 리스트
		if (requestDto.title() == null || requestDto.title().isBlank()) {
			page = productRepository.findByStatusAndDeleteDtIsNull(requestDto.status(), pageable);
		} else {
			page = productRepository.findByStatusAndTitleContainingAndDeleteDtIsNull(requestDto.status(),
				requestDto.title(),
				pageable);
		}

		// 검색해온 상품의 user uuid를 List에 담는 작업
		List<UUID> uuidList = page.getContent()
			.stream()
			.map(Product::getSellerId)
			.distinct()
			.toList();

		// seller List 가져오기
		List<UserResponseDto> sellerList = sellerRepository.findSellerList(uuidList)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SELLER_NOT_FOUND));

		// seller를 map으로 변환
		Map<UUID, String> sellerMap =
			sellerList.stream()
				.collect(Collectors.toMap(
						UserResponseDto::userId,
						UserResponseDto::name
					)
				);

		// sellerId를 가져온 기준으로 sellerNmae set
		List<ProductResponseDto> resultPro = page.getContent().stream()
			.map(p -> ProductResponseDto.listFrom(
				p,
				sellerMap
			))
			.toList();

		return SearchProductResponseDto.from(page, resultPro);
	}

	@Override
	public ProductResponseDto searchById(UUID productId) {
		Product product = findByIdOrThrow(productId);

		// sellerId를 확인
		UserResponseDto seller = findBySellerIdOrThrow(product.getSellerId());

		return ProductResponseDto.from(product, seller.name());
	}

	@Override
	public Product findByIdOrThrow(UUID productId) {
		return productRepository.findById(productId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.PRODUCT_NOT_FOUND));
	}

	@Override
	// 해당 상품 보유자인지 확인
	public Product matchProductAndSellerId(UUID productId, UUID sellerId) {
		return productRepository.findByIdAndSellerId(productId, sellerId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.MATCH_FAIL));
	}

	@Override
	public List<ProductSettlementItemResponseDto> getProductsByIds(List<UUID> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return List.of();
		}

		List<UUID> distinctProductIds = productIds.stream()
			.distinct()
			.toList();

		Map<UUID, Product> productMap = productRepository.findAllByIds(distinctProductIds).stream()
			.collect(Collectors.toMap(Product::getId, product -> product));

		return distinctProductIds.stream()
			.map(productMap::get)
			.filter(Objects::nonNull)
			.map(ProductSettlementItemResponseDto::from)
			.toList();
	}

	// 로그인 계정 여부
	private UserResponseDto findBySellerIdOrThrow(UUID sellerId) {
		UserResponseDto sellerInfo = sellerRepository.findSeller(sellerId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SELLER_NOT_FOUND));

		return sellerInfo;
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
