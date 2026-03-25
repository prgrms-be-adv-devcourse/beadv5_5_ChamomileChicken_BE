package jabaclass.product;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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
import jabaclass.product.domain.model.ProductStatus;
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
public class ProductCUDTest {
	private Validator validator;

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

	// test 상품
	private Product product1;

	UUID productId = UUID.randomUUID();

	@BeforeEach
	void setup() {
		product1 = Product.builder()
			.id(productId)
			.sellerId(SELLER_ID)
			.title("상품A")
			.maxCapacity(10)
			.description("테스트1")
			.descriptionImage(UUID.randomUUID().toString())
			.price(PRICE)
			.status(ProductStatus.ENABLE)
			.build();
	}

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private static final BigDecimal PRICE = new BigDecimal("1000.50");

	private static final UUID SELLER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

	@Test
	void 상품_생성_테스트() {
		// given
		CreateProductRequestDto product = new CreateProductRequestDto(
			SELLER_ID,
			"테스트상품",
			5,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);

		// Stub: seller 조회
		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "테스트 판매자", "SELLER")));

		// Stub: repository.save -> 입력 객체 그대로 반환 + 단위 테스트용 ID 설정
		given(productRepository.save(any(Product.class)))
			.willAnswer(invocation -> {
				Product p = invocation.getArgument(0);
				ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
				return p;
			});

		// when
		ProductResponseDto saved = productService.create(product);

		// then: 값 검증
		assertThat(saved.title()).isEqualTo("테스트상품");
		assertThat(saved.price()).isEqualByComparingTo(PRICE);

		// then: repository 호출 검증
		verify(productRepository, times(1)).save(any(Product.class));

		// then: 이벤트 발행 검증
		verify(publisher, times(1)).publishEvent(any(ProductEventResponseDto.class));

	}

	@Test
	void 인원수_0이면_상품_생성시_예외가_발생() {
		// given
		CreateProductRequestDto product = new CreateProductRequestDto(
			SELLER_ID,
			"테스트상품",
			0,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);

		// 직접 Validator 호출
		Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(product);

		assertThat(violations).extracting("message")
			.contains("예약 가능 인원수를 입력해주세요.");

		then(productRepository).should(never()).save(any());
		then(publisher).should(never()).publishEvent(any());
	}

	@Test
	void 상품명_공백_상품_생성시_예외가_발생() {
		// given
		CreateProductRequestDto product = new CreateProductRequestDto(
			SELLER_ID,
			"",
			10,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);

		// 직접 Validator 호출
		Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(product);

		assertThat(violations).extracting("message")
			.contains("상품명을 입력해주세요.");

		then(productRepository).should(never()).save(any());
		then(publisher).should(never()).publishEvent(any());
	}

	@Test
	void 판매자_미기재_상품_생성시_예외가_발생() {
		// given
		CreateProductRequestDto product = new CreateProductRequestDto(
			null,
			"테스트상품",
			10,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);

		// 직접 Validator 호출
		Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(product);

		assertThat(violations).extracting("message")
			.contains("판매자 Id를 입력해주세요.");

		then(productRepository).should(never()).save(any());
		then(publisher).should(never()).publishEvent(any());
	}

	@Test
	void 상품가격_0원_상품_생성시_예외가_발생() {
		BigDecimal ZERO = new BigDecimal("0");
		// given
		CreateProductRequestDto product = new CreateProductRequestDto(
			SELLER_ID,
			"테스트상품",
			10,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			ZERO,
			ProductStatus.ENABLE
		);

		// 직접 Validator 호출
		Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(product);

		assertThat(violations).extracting("message")
			.contains("가격은 1원 이상이어야 합니다.");

		then(productRepository).should(never()).save(any());
		then(publisher).should(never()).publishEvent(any());
	}

	@Test
	void 판매자가_존재하지_않으면_예외가_발생() {
		CreateProductRequestDto product = new CreateProductRequestDto(
			SELLER_ID,
			"테스트상품",
			5,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);
		// given
		// ... product DTO 생성
		given(sellerRepository.findSeller(any(UUID.class))).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> productService.create(product))
			.isInstanceOf(BusinessException.class)
			.hasMessage("존재하지 않는 판매자 입니다.");
	}

	@Test
	void 판매자_권한이_없으면_예외가_발생() {
		CreateProductRequestDto product = new CreateProductRequestDto(
			SELLER_ID,
			"테스트상품",
			5,
			"테스트 상품 입니다.",
			UUID.randomUUID().toString(),
			PRICE,
			ProductStatus.ENABLE
		);
		// ... product DTO 생성
		given(sellerRepository.findSeller(any(UUID.class)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "일반 사용자", "USER")));

		// when & then
		assertThatThrownBy(() -> productService.create(product))
			.isInstanceOf(BusinessException.class)
			.hasMessage("판매자가 아닙니다.");
	}

	@Test
	void 상품_수정_성공() {
		// given
		UpdateProductRequestDto updateDto = new UpdateProductRequestDto(
			"수정상품",
			10,
			"수정 설명",
			UUID.randomUUID().toString(),
			new BigDecimal("1200.00"),
			ProductStatus.ENABLE
		);

		/// Stub: seller 조회
		when(auditorAwareService.getCurrentAuditor())
			.thenReturn(Optional.of(SELLER_ID));

		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(
				SELLER_ID, "테스트 판매자", "SELLER"
			)));

		// 상품 조회 (1번)
		given(productRepository.findById(productId))
			.willReturn(Optional.of(product1));

		// 권한 체크 (2번)
		given(productRepository.findByIdAndSellerId(productId, SELLER_ID))
			.willReturn(Optional.of(product1));

		// when
		ProductResponseDto updated = productService.update(updateDto, productId);

		// then
		assertThat(updated.title()).isEqualTo("수정상품");
		assertThat(updated.price()).isEqualByComparingTo(updateDto.price());
	}

	@Test
	void 없는_상품_수정_시_예외발생() {
		UUID productId = UUID.randomUUID();
		UpdateProductRequestDto updateDto = new UpdateProductRequestDto(
			"수정상품",
			10,
			"수정 설명",
			UUID.randomUUID().toString(),
			new BigDecimal("1200.00"),
			ProductStatus.ENABLE
		);

		// 존재하는 판매자
		// Stub: seller 조회
		when(auditorAwareService.getCurrentAuditor())
			.thenReturn(Optional.of(SELLER_ID));

		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "테스트 판매자", "SELLER")));

		given(productRepository.findById(productId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.update(updateDto, productId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("존재하지 않는 상품 ID 입니다.");

		then(productRepository).should(never()).save(any(Product.class));
	}

	@Test
	void 상품_삭제_성공() {
		// 존재하는 판매자
		when(auditorAwareService.getCurrentAuditor())
			.thenReturn(Optional.of(SELLER_ID));

		// Stub: seller 조회
		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(
				SELLER_ID, "테스트 판매자", "SELLER"
			)));

		//  상품 조회 (1번)
		given(productRepository.findById(productId))
			.willReturn(Optional.of(product1));

		//  권한 체크 (2번)
		given(productRepository.findByIdAndSellerId(productId, SELLER_ID))
			.willReturn(Optional.of(product1));

		// when
		productService.delete(productId);

		// then
		assertThat(product1.getDeleteDt()).isNotNull();
	}

	@Test
	void 없는_상품_삭제_시_예외발생() {
		UUID productId = UUID.randomUUID();
		given(productRepository.findById(productId)).willReturn(Optional.empty());

		// 존재하는 판매자
		// Stub: seller 조회
		when(auditorAwareService.getCurrentAuditor())
			.thenReturn(Optional.of(SELLER_ID));

		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "테스트 판매자", "SELLER")));

		assertThatThrownBy(() -> productService.delete(productId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("존재하지 않는 상품 ID 입니다.");

		then(productRepository).should(times(1)).findById(productId);
	}

	@Test
	void 다른_판매자가_수정하면_예외() {
		UpdateProductRequestDto updateDto = new UpdateProductRequestDto(
			"수정상품",
			10,
			"수정 설명",
			UUID.randomUUID().toString(),
			new BigDecimal("1200.00"),
			ProductStatus.ENABLE
		);
		when(auditorAwareService.getCurrentAuditor())
			.thenReturn(Optional.of(SELLER_ID));

		given(sellerRepository.findSeller(any(UUID.class)))
			.willAnswer(invocation -> {
				UUID id = invocation.getArgument(0);
				return Optional.of(new SellerResponseDto(id, "판매자", "SELLER"));
			});

		// 같은 productId로 맞춰야 함
		given(productRepository.findById(any(UUID.class)))
			.willReturn(Optional.of(product1));

		assertThatThrownBy(() -> productService.update(updateDto, SELLER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessage("해당 상품은 본인 상품이 아닙니다.");
	}

}
