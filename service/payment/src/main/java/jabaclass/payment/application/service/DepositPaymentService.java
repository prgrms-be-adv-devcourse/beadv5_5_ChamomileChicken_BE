package jabaclass.payment.application.service;

import jabaclass.payment.application.port.external.PaymentGatewayPort;
import jabaclass.payment.application.port.external.UserPort;
import jabaclass.payment.application.usecase.DepositPaymentUseCase;
import jabaclass.payment.domain.model.DepositPayment;
import jabaclass.payment.domain.repository.DepositPaymentRepository;
import jabaclass.payment.presentation.dto.request.ConfirmDepositPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PrepareDepositPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.ConfirmDepositPaymentResponseDto;
import jabaclass.payment.presentation.dto.response.PrepareDepositPaymentResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DepositPaymentService implements DepositPaymentUseCase {

	private final DepositPaymentRepository depositPaymentRepository;
	private final PaymentGatewayPort paymentGatewayPort;
	private final UserPort userPort;

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

		// DepositPayment 조회
		DepositPayment depositPayment = depositPaymentRepository.findById(request.depositPaymentsId())
			.orElseThrow(() -> new IllegalStateException("예치금 결제 정보를 찾을 수 없습니다."));

		// 멱등성 체크
		if (depositPayment.isDone()) {
			return new ConfirmDepositPaymentResponseDto(true);
		}

		// 충전 금액 검증
		if (depositPayment.getAmount().intValue() != request.amount()) {
			throw new IllegalStateException("결제 금액이 일치하지 않습니다.");
		}

		try {
			// PG 승인
			paymentGatewayPort.confirm(
				request.paymentKey(),
				depositPayment.getId().toString(),
				request.amount()
			);

			// 예치금 증가
			userPort.increaseDeposit(
				depositPayment.getUserId(),
				depositPayment.getAmount(),
				depositPayment.getId()
			);

			// paymentKey 저장 + 상태 변경
			depositPayment.markDone(request.paymentKey());

		} catch (Exception e) {

			log.error("예치금 결제 승인 실패. depositPaymentId={}",
				depositPayment.getId(), e);

			// 실패 처리
			depositPayment.markFailed();

			throw new IllegalStateException("예치금 결제 승인에 실패했습니다.", e);
		}

		return new ConfirmDepositPaymentResponseDto(true);
	}
}
