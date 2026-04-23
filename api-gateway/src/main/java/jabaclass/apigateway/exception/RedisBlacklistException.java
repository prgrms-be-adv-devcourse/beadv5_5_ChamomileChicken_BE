package jabaclass.apigateway.exception;

public class RedisBlacklistException extends RuntimeException {
	public RedisBlacklistException(Throwable cause) {
		super(cause);
	}
}
