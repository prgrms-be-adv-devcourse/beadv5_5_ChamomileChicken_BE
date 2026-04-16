package jabaclass.admin.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
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
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.product.domain.model.Product;
import jabaclass.admin.product.domain.model.ProductStatus;
import jabaclass.admin.product.domain.repository.ProductAdminRepository;
import jabaclass.admin.product.infrastructure.kafka.AdminProductEvent;
import jabaclass.admin.product.presentation.dto.response.ProductAdminResponseDto;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class ProductAdminServiceTest {

	@Mock
	private ProductAdminRepository productAdminRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ProductAdminService productAdminService;

	private UUID productId;
	private Product product;

	@BeforeEach
	void setUp() {
		productId = UUID.randomUUID();
		product = Product.builder()
			.sellerId(UUID.randomUUID())
			.title("테스트상품")
			.maxCapacity(10)
			.price(new BigDecimal("50000"))
			.status(ProductStatus.ENABLE)
			.build();
		ReflectionTestUtils.setField(product, "id", productId);
	}

	@Test
	void 전체_상품_목록을_조회한다() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		given(productAdminRepository.findAll(pageable))
			.willReturn(new PageImpl<>(List.of(product)));

		// when
		Page<ProductAdminResponseDto> result = productAdminService.getProducts(pageable);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).id()).isEqualTo(productId);
		assertThat(result.getContent().get(0).title()).isEqualTo("테스트상품");
		assertThat(result.getContent().get(0).status()).isEqualTo(ProductStatus.ENABLE);
		then(productAdminRepository).should(times(1)).findAll(pageable);
	}

	@Test
	void 상품을_강제로_내리면_상태가_DISABLE로_변경되고_이벤트가_발행된다() {
		// given
		given(productAdminRepository.findById(productId)).willReturn(Optional.of(product));

		// when
		productAdminService.forceDownProduct(productId);

		// then
		assertThat(product.getStatus()).isEqualTo(ProductStatus.DISABLE);
		assertThat(product.getDeleteDt()).isNotNull();
		then(eventPublisher).should(times(1)).publishEvent(
			any(AdminProductEvent.class)
		);
	}

	@Test
	void 강제로_내린_이벤트의_타입은_FORCE_DOWN이다() {
		// given
		given(productAdminRepository.findById(productId)).willReturn(Optional.of(product));

		// when
		productAdminService.forceDownProduct(productId);

		// then
		then(eventPublisher).should(times(1)).publishEvent(
			org.mockito.ArgumentMatchers.<Object>argThat(event ->
				event instanceof AdminProductEvent e
					&& "FORCE_DOWN".equals(e.type())
					&& productId.toString().equals(e.productId())
			)
		);
	}

	@Test
	void 존재하지_않는_상품_강제_내리기시_예외가_발생한다() {
		// given
		given(productAdminRepository.findById(productId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> productAdminService.forceDownProduct(productId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("상품을 찾을 수 없습니다.");
		then(eventPublisher).shouldHaveNoInteractions();
	}
}
