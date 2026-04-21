package jabaclass.order.presentation.controller;

import java.util.List;
import java.util.UUID;

import jabaclass.order.application.port.internal.OrderUseCase;
import jabaclass.order.domain.model.OrderStatus;
import jabaclass.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.presentation.dto.response.CreateOrderResponseDto;
import jabaclass.order.presentation.dto.response.OrderRefundInfoResponseDto;
import jabaclass.order.presentation.dto.response.OrderResponseDto;
import jabaclass.order.presentation.openapi.OrderOpenApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jabaclass.order.common.auth.CurrentUser;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController implements OrderOpenApi {

    private final OrderUseCase orderUseCase;

    @Override
    @PostMapping
    public ResponseEntity<CreateOrderResponseDto> create(
        @CurrentUser UUID userId,
        @Valid @RequestBody CreateOrderRequestDto requestDto
    ) {
        CreateOrderResponseDto responseDto = orderUseCase.create(userId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Override
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getById(@CurrentUser UUID userId,
			@PathVariable UUID orderId) {
        OrderResponseDto responseDto = orderUseCase.getById(userId, orderId);

        return ResponseEntity.ok(responseDto);
    }

	@GetMapping("/{orderId}/refund-info")
	public ResponseEntity<OrderRefundInfoResponseDto> getRefundInfo(
		@CurrentUser UUID userId,
		@PathVariable UUID orderId
	) {
		return ResponseEntity.ok(orderUseCase.getRefundInfo(userId, orderId));
	}

    @Override
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(
        @CurrentUser UUID userId,
        @RequestParam(required = false) OrderStatus status
    ) {
        List<OrderResponseDto> responseDto = orderUseCase.getOrders(userId, status);

        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{orderId}/refund")
    public ResponseEntity<Void> refund(
        @CurrentUser UUID userId,
        @PathVariable UUID orderId
    ) {
        orderUseCase.refund(userId, orderId);
        return ResponseEntity.noContent().build();
    }

}
