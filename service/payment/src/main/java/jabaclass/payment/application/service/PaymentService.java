package jabaclass.payment.application.service;

import jabaclass.payment.application.usercase.PaymentUseCase;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService implements PaymentUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PaymentResponseDto create(PreparePaymentRequestDto request) {

        // 로그인 사용자
        UUID userId = getCurrentUserId();

        Payment payment = Payment.create(
                userId,
                request.sellerId(),
                request.productId(),
                request.orderId(),
                request.paymentMethod(),
                request.paymentAmount(),
                request.depositAmount()
        );

        Payment savedPayment = paymentRepository.save(payment);

        return PaymentResponseDto.from(savedPayment);
    }

    private UUID getCurrentUserId() {
        // TODO: 로그인 기능 구현 후 SecurityContext로 대체
        return UUID.randomUUID();
    }
}
