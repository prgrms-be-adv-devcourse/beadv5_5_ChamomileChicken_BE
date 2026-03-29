package jabaclass.frontend.controller;

import jabaclass.frontend.client.PaymentServiceClient;
import jabaclass.frontend.client.UserServiceClient;
import jabaclass.frontend.dto.UserInfoDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/deposit")
@RequiredArgsConstructor
@Slf4j
public class DepositChargeController {

    private final UserServiceClient userServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    @Value("${toss.client-key}")
    private String tossClientKey;

    @Value("${toss.deposit-success-url}")
    private String depositSuccessUrl;

    @Value("${toss.deposit-fail-url}")
    private String depositFailUrl;

    @GetMapping("/charge")
    public String chargePage(HttpSession session, Model model) {
        String accessToken = (String) session.getAttribute("accessToken");
        try {
            UserInfoDto userInfo = userServiceClient.getMyInfo(accessToken);
            model.addAttribute("userId", userInfo.getUserId().toString());
            model.addAttribute("currentDeposit", userInfo.getDeposit());
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return "redirect:/mypage";
        }
        model.addAttribute("tossClientKey", tossClientKey);
        model.addAttribute("depositSuccessUrl", depositSuccessUrl);
        model.addAttribute("depositFailUrl", depositFailUrl);
        return "deposit/charge";
    }

    @PostMapping("/prepare")
    @ResponseBody
    public ResponseEntity<Map<String, String>> prepare(@RequestBody Map<String, Object> body, HttpSession session) {
        String accessToken = (String) session.getAttribute("accessToken");
        try {
            UUID userId = UUID.fromString(body.get("userId").toString());
            int amount = Integer.parseInt(body.get("amount").toString());
            UUID depositPaymentsId = paymentServiceClient.prepareDepositPayment(userId, amount, accessToken);
            return ResponseEntity.ok(Map.of("depositPaymentsId", depositPaymentsId.toString()));
        } catch (Exception e) {
            log.error("예치금 결제 준비 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "결제 준비 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/success")
    public String success(
        @RequestParam String paymentKey,
        @RequestParam String orderId,
        @RequestParam int amount,
        HttpSession session,
        Model model
    ) {
        String accessToken = (String) session.getAttribute("accessToken");
        try {
            UUID depositPaymentsId = UUID.fromString(orderId);
            paymentServiceClient.confirmDepositPayment(depositPaymentsId, paymentKey, amount, accessToken);
            model.addAttribute("amount", amount);
            return "deposit/success";
        } catch (Exception e) {
            log.error("예치금 결제 확정 실패: {}", e.getMessage());
            model.addAttribute("errorMessage", "예치금 충전 처리 중 오류가 발생했습니다.");
            return "deposit/fail";
        }
    }

    @GetMapping("/fail")
    public String fail(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String message,
        Model model
    ) {
        model.addAttribute("errorCode", code);
        model.addAttribute("errorMessage", message);
        return "deposit/fail";
    }
}
