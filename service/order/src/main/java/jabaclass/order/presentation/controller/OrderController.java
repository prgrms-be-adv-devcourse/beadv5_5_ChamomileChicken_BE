package jabaclass.order.presentation.controller;

import jabaclass.order.application.usecase.OrderUseCase;

import jabaclass.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.presentation.dto.response.OrderResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderUseCase orderUseCase;

    @PostMapping
    public ResponseEntity<OrderResponseDto> create(@Valid @RequestBody CreateOrderRequestDto requestDto) {
        final OrderResponseDto responseDto = orderUseCase.create(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable UUID orderId) {
        final OrderResponseDto responseDto = orderUseCase.getById(orderId);

        return ResponseEntity.ok(responseDto);
    }
}
