package jabaclass.product;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.service.ProductService;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.ProductStatus;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import jabaclass.product.presentation.dto.request.SearchProductRequestDto;
import jabaclass.product.presentation.dto.respose.ProductResponseDto;
import jabaclass.product.presentation.dto.respose.SearchProductReposeDto;

@ExtendWith(MockitoExtension.class)
public class ProductSelectTest {

	@InjectMocks
	private ProductService productService;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private SellerRepository sellerRepository;

	@Mock
	private ApplicationEventPublisher publisher;

	private static final BigDecimal PRICE = new BigDecimal("1000.50");

	private static final UUID SELLER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

	// test 상품
	private Product product1;
	private Product product2;

	@BeforeEach
	void setup() {
		product1 = Product.builder()
			.sellerId(SELLER_ID)
			.title("상품A")
			.maxCapacity(10)
			.description("테스트1")
			.descriptionImage(UUID.randomUUID().toString())
			.price(PRICE)
			.status(ProductStatus.ENABLE)
			.build();
		product2 = Product.builder()
			.sellerId(SELLER_ID)
			.title("상품B")
			.maxCapacity(3)
			.description("테스트2")
			.descriptionImage(UUID.randomUUID().toString())
			.price(PRICE)
			.status(ProductStatus.ENABLE)
			.build();
	}

	@Test
	void 전체_상품_조회() {
		SearchProductRequestDto request = new SearchProductRequestDto(
			"",
			0,
			10
		);
		Pageable pageable = PageRequest.of(request.thisPage(), request.pageSize());
		Page<Product> page = new PageImpl<>(List.of(product1, product2));
		// given
		given(productRepository.findByStatusAndDeleteDtIsNull(any(), any(Pageable.class)))
			.willReturn(page);

		// 존재하는 판매자
		given(sellerRepository.findSellerList(anyList()))
			.willReturn(Optional.of(List.of(
				new SellerResponseDto(SELLER_ID, "판매자1", "SELLER")
			)));

		// when
		SearchProductReposeDto result = productService.searchAll(request);

		// then
		assertThat(result.content()).extracting(ProductResponseDto::title)
			.containsExactly("상품A", "상품B");
		then(productRepository).should().findByStatusAndDeleteDtIsNull(any(), any());
	}

	@Test
	void 특정_상품_조회_성공() {
		// given
		UUID id = product1.getId();
		given(productRepository.findById(id)).willReturn(Optional.of(product1));

		// 존재하는 판매자
		// Stub: seller 조회
		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "테스트 판매자", "SELLER")));

		// when
		ProductResponseDto result = productService.searchById(id);

		// then
		assertThat(result.title()).isEqualTo("상품A");
		then(productRepository).should(times(1)).findById(id);
	}

	@Test
	void 특정_상품_조회_실패() {
		// given
		UUID id = UUID.randomUUID();
		given(productRepository.findById(id)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> productService.searchById(id))
			.isInstanceOf(BusinessException.class)
			.hasMessage("존재하지 않는 상품 ID 입니다.");
		then(productRepository).should(times(1)).findById(id);
	}
}
