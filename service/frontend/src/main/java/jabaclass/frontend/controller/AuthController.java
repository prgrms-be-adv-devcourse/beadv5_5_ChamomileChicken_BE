package jabaclass.frontend.controller;

import jabaclass.frontend.client.UserServiceClient;
import jabaclass.frontend.dto.LoginRequest;
import jabaclass.frontend.dto.LoginResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserServiceClient userServiceClient;

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest request, HttpSession session, Model model) {
        try {
            LoginResponse loginResponse = userServiceClient.login(request);

            // JWT를 세션에 저장 (SessionAuthFilter가 매 요청마다 인증 복원)
            session.setAttribute("accessToken", loginResponse.getAccessToken());
            session.setAttribute("refreshToken", loginResponse.getRefreshToken());
            session.setAttribute("email", request.getEmail());

            return "redirect:/products";
        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage());
            model.addAttribute("loginRequest", request);
            model.addAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}