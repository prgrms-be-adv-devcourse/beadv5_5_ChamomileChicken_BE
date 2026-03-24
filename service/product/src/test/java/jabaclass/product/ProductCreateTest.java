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

import jabaclass.product.application.service.ProductService;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.ProductStatus;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.infrastructure.acl.client.SellerClient;
import jabaclass.product.infrastructure.acl.dto.SellerResposeDto;
import jabaclass.product.infrastructure.event.dto.ProductEventResposeDto;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.respose.CreateProductResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProductCreateTest {
	private Validator validator;

	@InjectMocks
	private ProductService productService;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private SellerClient sellerClient;

	@Mock
	private ApplicationEventPublisher publisher;

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
		given(sellerClient.findSeller(any(UUID.class)))
			.willReturn(Optional.of(new SellerResposeDto(SELLER_ID, "테스트 판매자", "SELLER")));

		// Stub: repository.save -> 입력 객체 그대로 반환 + 단위 테스트용 ID 설정
		given(productRepository.save(any(Product.class)))
			.willAnswer(invocation -> {
				Product p = invocation.getArgument(0);
				return p;
			});

		// when
		CreateProductResponseDto saved = productService.create(product);

		// then: 값 검증
		assertThat(saved.title()).isEqualTo("테스트상품");
		assertThat(saved.price()).isEqualByComparingTo(PRICE);

		// then: repository 호출 검증
		verify(productRepository, times(1)).save(any(Product.class));

		// then: 이벤트 발행 검증
		verify(publisher, times(1)).publishEvent(any(ProductEventResposeDto.class));

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
}
