package jabaclass.apigateway.exception;

public class RedisCircuitOpenException extends RuntimeException{

	public RedisCircuitOpenException(Throwable cause) {
		super(cause);
	}
}
