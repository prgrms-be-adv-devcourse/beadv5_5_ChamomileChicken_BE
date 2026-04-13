package jabaclass.order.presentation.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jabaclass.order.application.port.internal.OrderUseCase;
import jabaclass.order.presentation.dto.request.OrderBulkReadRequestDto;
import jabaclass.order.presentation.dto.request.UpdateOrderPaymentStatusRequestDto;
import jabaclass.order.presentation.dto.response.OrderSettlementItemResponseDto;
import jabaclass.order.presentation.dto.response.ValidatePaymentAmountResponseDto;
import jabaclass.order.presentation.openapi.OrderInternalOpenApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderInternalController implements OrderInternalOpenApi {

    private final OrderUseCase orderUseCase;

    @Override
    @GetMapping("/{orderId}/payment-amount/validate")
    public ResponseEntity<ValidatePaymentAmountResponseDto> validatePaymentAmount(
        @PathVariable UUID orderId,
        @RequestParam BigDecimal amount
    ) {
        boolean valid = orderUseCase.validatePaymentAmount(orderId, amount);

        return ResponseEntity.ok(new ValidatePaymentAmountResponseDto(valid));
    }

    @Override
    @PutMapping("/{orderId}/payment-status")
    public ResponseEntity<Void> updatePaymentStatus(
        @PathVariable UUID orderId,
        @Valid @RequestBody UpdateOrderPaymentStatusRequestDto requestDto
    ) {
        orderUseCase.updatePaymentStatus(orderId, requestDto);

        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/bulk")
    public ResponseEntity<List<OrderSettlementItemResponseDto>> getOrdersByIds(
        @Valid @RequestBody OrderBulkReadRequestDto requestDto
    ) {
        return ResponseEntity.ok(orderUseCase.getOrdersByIds(requestDto));
    }
}
