package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.service.AuditorAwareService;
import jabaclass.product.application.service.ProductService;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import jabaclass.product.infrastructure.event.dto.ProductEventResponseDto;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.request.UpdateProductRequestDto;
import jabaclass.product.presentation.dto.respose.ProductResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class ProductCUDTest {

	@InjectMocks
	private ProductService productService;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private SellerRepository sellerRepository;

	@Mock
	private ApplicationEventPublisher publisher;

	@Mock
	private AuditorAwareService auditorAwareService;

	private static final UUID SELLER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final BigDecimal PRICE = new BigDecimal("1000.50");

	private Validator validator;
	private UUID productId;
	private Product product;

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
		productId = UUID.randomUUID();

		product = Product.builder()
			.id(productId)
			.sellerId(SELLER_ID)
			.title("상품A")
			.maxCapacity(10)
			.description("테스트 상품")
			.descriptionImage(UUID.randomUUID().toString())
			.price(PRICE)
			.status(ProductStatus.ENABLE)
			.build();
	}

	@Test
	void 상품_생성에_성공한다() {
		CreateProductRequestDto request = new CreateProductRequestDto(
			SELLER_ID,
			"테스트상품",
			5,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "테스트 판매자", "SELLER")));
		given(productRepository.save(any(Product.class)))
			.willAnswer(invocation -> {
				Product saved = invocation.getArgument(0);
				ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
				return saved;
			});

		ProductResponseDto saved = productService.create(request);

		assertThat(saved.title()).isEqualTo("테스트상품");
		assertThat(saved.price()).isEqualByComparingTo(PRICE);
		then(productRepository).should().save(any(Product.class));
		then(publisher).should().publishEvent(any(ProductEventResponseDto.class));
	}

	@Test
	void 최대인원이_0이면_상품_생성_검증에_실패한다() {
		CreateProductRequestDto request = new CreateProductRequestDto(
			SELLER_ID,
			"테스트상품",
			0,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);

		Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(request);

		assertThat(violations).extracting(ConstraintViolation::getMessage)
			.contains("예약 가능 인원수를 입력해주세요.");
	}

	@Test
	void 상품명이_비어있으면_상품_생성_검증에_실패한다() {
		CreateProductRequestDto request = new CreateProductRequestDto(
			SELLER_ID,
			"",
			10,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);

		Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(request);

		assertThat(violations).extracting(ConstraintViolation::getMessage)
			.contains("상품명을 입력해주세요.");
	}

	@Test
	void 판매자ID가_없으면_상품_생성_검증에_실패한다() {
		CreateProductRequestDto request = new CreateProductRequestDto(
			null,
			"테스트상품",
			10,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);

		Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(request);

		assertThat(violations).extracting(ConstraintViolation::getMessage)
			.contains("판매자 Id를 입력해주세요.");
	}

	@Test
	void 가격이_0이면_상품_생성_검증에_실패한다() {
		CreateProductRequestDto request = new CreateProductRequestDto(
			SELLER_ID,
			"테스트상품",
			10,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			BigDecimal.ZERO,
			ProductStatus.ENABLE
		);

		Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(request);

		assertThat(violations).extracting(ConstraintViolation::getMessage)
			.contains("가격은 1원 이상이어야 합니다.");
	}

	@Test
	void 판매자가_존재하지_않으면_상품_생성에_실패한다() {
		CreateProductRequestDto request = new CreateProductRequestDto(
			SELLER_ID,
			"테스트상품",
			5,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(any(UUID.class))).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.create(request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("존재하지 않는 판매자 입니다.");
	}

	@Test
	void 판매자_권한이_없으면_상품_생성에_실패한다() {
		CreateProductRequestDto request = new CreateProductRequestDto(
			SELLER_ID,
			"테스트상품",
			5,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(any(UUID.class)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "일반 사용자", "USER")));

		assertThatThrownBy(() -> productService.create(request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("판매자가 아닙니다.");
	}

	@Test
	void 상품_수정에_성공한다() {
		UpdateProductRequestDto request = new UpdateProductRequestDto(
			"수정상품",
			10,
			"수정 설명",
			UUID.randomUUID().toString(),
			new BigDecimal("1200.00"),
			ProductStatus.ENABLE
		);
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "테스트 판매자", "SELLER")));
		given(productRepository.findById(productId)).willReturn(Optional.of(product));
		given(productRepository.findByIdAndSellerId(productId, SELLER_ID)).willReturn(Optional.of(product));

		ProductResponseDto updated = productService.update(request, productId);

		assertThat(updated.title()).isEqualTo("수정상품");
		assertThat(updated.price()).isEqualByComparingTo(request.price());
	}

	@Test
	void 존재하지_않는_상품은_수정에_실패한다() {
		UUID missingProductId = UUID.randomUUID();
		UpdateProductRequestDto request = new UpdateProductRequestDto(
			"수정상품",
			10,
			"수정 설명",
			UUID.randomUUID().toString(),
			new BigDecimal("1200.00"),
			ProductStatus.ENABLE
		);
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "테스트 판매자", "SELLER")));
		given(productRepository.findById(missingProductId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.update(request, missingProductId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("존재하지 않는 상품 ID 입니다.");
	}

	@Test
	void 상품_삭제에_성공한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "테스트 판매자", "SELLER")));
		given(productRepository.findById(productId)).willReturn(Optional.of(product));
		given(productRepository.findByIdAndSellerId(productId, SELLER_ID)).willReturn(Optional.of(product));

		productService.delete(productId);

		assertThat(product.getDeleteDt()).isNotNull();
		assertThat(product.getStatus()).isEqualTo(ProductStatus.DISABLE);
	}

	@Test
	void 존재하지_않는_상품은_삭제에_실패한다() {
		UUID missingProductId = UUID.randomUUID();
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "테스트 판매자", "SELLER")));
		given(productRepository.findById(missingProductId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.delete(missingProductId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("존재하지 않는 상품 ID 입니다.");
	}

	@Test
	void 다른_판매자의_상품은_수정할_수_없다() {
		UpdateProductRequestDto request = new UpdateProductRequestDto(
			"수정상품",
			10,
			"수정 설명",
			UUID.randomUUID().toString(),
			new BigDecimal("1200.00"),
			ProductStatus.ENABLE
		);
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(any(UUID.class)))
			.willAnswer(invocation -> {
				UUID id = invocation.getArgument(0);
				return Optional.of(new SellerResponseDto(id, "판매자", "SELLER"));
			});
		given(productRepository.findById(any(UUID.class))).willReturn(Optional.of(product));
		given(productRepository.findByIdAndSellerId(any(UUID.class), any(UUID.class))).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.update(request, SELLER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessage("해당 상품은 본인 상품이 아닙니다.");
	}
}
