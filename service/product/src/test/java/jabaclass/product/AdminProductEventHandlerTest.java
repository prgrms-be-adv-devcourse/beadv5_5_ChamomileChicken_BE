package jabaclass.product;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.domain.repository.ScheduleRepository;
import jabaclass.product.infrastructure.kafka.admin.AdminProductEventHandler;


@ExtendWith(MockitoExtension.class)
class AdminProductEventHandlerTest {

	@InjectMocks
	private AdminProductEventHandler handler;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ScheduleRepository scheduleRepository;

	@Test
	void processForceDown_상품_DISABLE_및_소프트딜리트_성공() {
		UUID productId = UUID.randomUUID();
		Product product = mock(Product.class);
		given(productRepository.findById(productId)).willReturn(Optional.of(product));
		given(scheduleRepository.softDeleteByProductId(productId)).willReturn(3);

		handler.processForceDown(productId);

		then(product).should().changeStatus(ProductStatus.DISABLE);
		then(product).should().changeDelete();
		then(scheduleRepository).should().softDeleteByProductId(productId);
	}

	@Test
	void processForceDown_상품이_없으면_BusinessException_발생() {
		UUID productId = UUID.randomUUID();
		given(productRepository.findById(productId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.processForceDown(productId))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("존재하지 않는 상품");
	}

}
