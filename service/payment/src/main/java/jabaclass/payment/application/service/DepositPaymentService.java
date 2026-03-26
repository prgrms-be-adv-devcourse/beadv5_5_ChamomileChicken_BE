package jabaclass.payment.application.service;

import jabaclass.payment.application.usecase.DepositPaymentUseCase;
import jabaclass.payment.domain.model.DepositPayment;
import jabaclass.payment.domain.repository.DepositPaymentRepository;
import jabaclass.payment.presentation.dto.request.ConfirmDepositPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PrepareDepositPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.ConfirmDepositPaymentResponseDto;
import jabaclass.payment.presentation.dto.response.PrepareDepositPaymentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepositPaymentService implements DepositPaymentUseCase {

	private final DepositPaymentRepository depositPaymentRepository;

	@Override
	@Transactional
	public PrepareDepositPaymentResponseDto prepare(PrepareDepositPaymentRequestDto request) {
		DepositPayment depositPayment = DepositPayment.create(
			request.userId(),
			request.amount(),
			request.paymentMethod()
		);

		DepositPayment saved = depositPaymentRepository.save(depositPayment);

		return PrepareDepositPaymentResponseDto.from(saved);
	}

	@Override
	@Transactional
	public ConfirmDepositPaymentResponseDto confirm(ConfirmDepositPaymentRequestDto request) {
		DepositPayment depositPayment = depositPaymentRepository.findById(request.depositPaymentsId())
			.orElseThrow(() -> new IllegalStateException("예치금 결제 정보를 찾을 수 없습니다."));

		if (depositPayment.isDone()) {
			return new ConfirmDepositPaymentResponseDto(true);
		}

		depositPayment.markDone();

		return new ConfirmDepositPaymentResponseDto(true);
	}
}
