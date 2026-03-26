package jabaclass.user.deposit.domain.exception;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.common.error.ErrorCode;

public class DepositException extends BusinessException {

	public DepositException(ErrorCode errorCode) {
		super(errorCode);
	}
}
