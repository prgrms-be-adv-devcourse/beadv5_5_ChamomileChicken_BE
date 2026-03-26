package jabaclass.user.auth.application.usecase;

import java.util.UUID;

public interface LogoutUseCase {

    void logout(UUID userId);
}
