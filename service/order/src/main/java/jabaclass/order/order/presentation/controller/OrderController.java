package jabaclass.order.order.presentation.controller;

import java.util.List;
import java.util.UUID;

import jabaclass.order.order.application.usecase.OrderUseCase;
import jabaclass.order.order.domain.model.OrderStatus;
import jabaclass.order.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.order.presentation.dto.response.CreateOrderResponseDto;
import jabaclass.order.order.presentation.dto.response.OrderResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderUseCase orderUseCase;

    @PostMapping
    public ResponseEntity<CreateOrderResponseDto> create(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody CreateOrderRequestDto requestDto
    ) {
        CreateOrderResponseDto responseDto = orderUseCase.create(userId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable UUID orderId) {
        OrderResponseDto responseDto = orderUseCase.getById(orderId);

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(
        @AuthenticationPrincipal UUID userId,
        @RequestParam(required = false) OrderStatus status
    ) {
        List<OrderResponseDto> responseDto = orderUseCase.getOrders(userId, status);

        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> cancel(
        @PathVariable UUID orderId,
        @AuthenticationPrincipal UUID userId
    ) {
        OrderResponseDto responseDto = orderUseCase.cancel(orderId, userId);

        return ResponseEntity.ok(responseDto);
    }
}
