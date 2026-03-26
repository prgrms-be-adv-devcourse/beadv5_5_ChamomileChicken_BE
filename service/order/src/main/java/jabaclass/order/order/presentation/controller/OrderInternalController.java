package jabaclass.order.order.presentation.controller;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.order.order.application.usecase.OrderUseCase;
import jabaclass.order.order.presentation.dto.request.UpdateOrderPaymentStatusRequestDto;
import jabaclass.order.order.presentation.dto.response.ValidatePaymentAmountResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderInternalController {

    private final OrderUseCase orderUseCase;

    @GetMapping("/{orderId}/payment-amount/validate")
    public ResponseEntity<ValidatePaymentAmountResponseDto> validatePaymentAmount(
        @PathVariable UUID orderId,
        @RequestParam BigDecimal amount
    ) {
        boolean valid = orderUseCase.validatePaymentAmount(orderId, amount);

        return ResponseEntity.ok(new ValidatePaymentAmountResponseDto(valid));
    }

    @PatchMapping("/{orderId}/payment-status")
    public ResponseEntity<Void> updatePaymentStatus(
        @PathVariable UUID orderId,
        @Valid @RequestBody UpdateOrderPaymentStatusRequestDto requestDto
    ) {
        orderUseCase.updatePaymentStatus(orderId, requestDto);

        return ResponseEntity.noContent().build();
    }
}
