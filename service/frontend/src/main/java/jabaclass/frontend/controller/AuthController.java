package jabaclass.frontend.controller;

import jabaclass.frontend.client.UserServiceClient;
import jabaclass.frontend.dto.LoginRequest;
import jabaclass.frontend.dto.LoginResponse;
import jabaclass.frontend.dto.SignupRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

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

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup/send-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendVerificationCode(@RequestBody Map<String, String> body) {
        try {
            userServiceClient.checkEmailDuplicate(body.get("email"));
            userServiceClient.sendVerificationCode(body.get("email"));
            return ResponseEntity.ok(Map.of());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.badRequest().body(Map.of("error", extractErrorMessage(e, "인증번호 발송에 실패했습니다.")));
        } catch (Exception e) {
            log.error("인증번호 발송 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "인증번호 발송 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/signup/verify-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> verifyEmailCode(@RequestBody Map<String, String> body) {
        try {
            String verifiedToken = userServiceClient.verifyEmailCode(body.get("email"), body.get("code"));
            return ResponseEntity.ok(Map.of("verifiedToken", verifiedToken));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.badRequest().body(Map.of("error", extractErrorMessage(e, "인증번호가 올바르지 않습니다.")));
        } catch (Exception e) {
            log.error("이메일 인증 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "인증 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/signup")
    @ResponseBody
    public ResponseEntity<Map<String, String>> signup(@RequestBody SignupRequest request) {
        try {
            userServiceClient.register(request);
            return ResponseEntity.ok(Map.of());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.badRequest().body(Map.of("error", extractErrorMessage(e, "회원가입에 실패했습니다.")));
        } catch (Exception e) {
            log.error("회원가입 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "회원가입 처리 중 오류가 발생했습니다."));
        }
    }

    private String extractErrorMessage(HttpClientErrorException e, String defaultMsg) {
        try {
            String body = e.getResponseBodyAsString();
            int idx = body.indexOf("\"message\":\"");
            if (idx >= 0) {
                int start = idx + 11;
                int end = body.indexOf("\"", start);
                if (end > start) return body.substring(start, end);
            }
        } catch (Exception ignored) {}
        return defaultMsg;
    }
}