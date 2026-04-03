package jabaclass.frontend.exception;

public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException() {
        super("세션이 만료되었습니다. 다시 로그인해주세요.");
    }
}
