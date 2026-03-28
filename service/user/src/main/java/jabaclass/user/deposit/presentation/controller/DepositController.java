package jabaclass.user.deposit.presentation.controller;

import jabaclass.user.deposit.application.usecase.DepositChargeUseCase;
import jabaclass.user.deposit.application.usecase.DepositQueryUseCase;
import jabaclass.user.deposit.presentation.dto.request.IncreaseDepositRequestDto;
import jabaclass.user.deposit.presentation.dto.response.DepositDetailResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositHistoryResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositMeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposits")
public class DepositController implements DepositApi {

    private final DepositChargeUseCase depositChargeUseCase;
    private final DepositQueryUseCase depositQueryUseCase;

    @Override
    @GetMapping
    public ResponseEntity<DepositHistoryResponseDto> findAllDepositHistories(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(depositQueryUseCase.findAllDepositHistories(userId));
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<DepositMeResponseDto> findMyDeposit(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(depositQueryUseCase.findMyDeposit(userId));
    }

    @Override
    @GetMapping("/{depositHistoryId}")
    public ResponseEntity<DepositDetailResponseDto> findDepositHistory(
            @PathVariable UUID depositHistoryId
    ) {
        return ResponseEntity.ok(depositQueryUseCase.findDepositHistory(depositHistoryId));
    }

    @PutMapping("/internal/users/{userId}/deposit")
    public ResponseEntity<Void> increaseDeposit(
            @PathVariable UUID userId,
            @RequestBody IncreaseDepositRequestDto request
    ) {
        depositChargeUseCase.increase(
                userId,
                request.amount(),
                request.paymentId()
        );
        return ResponseEntity.ok().build();
    }

}