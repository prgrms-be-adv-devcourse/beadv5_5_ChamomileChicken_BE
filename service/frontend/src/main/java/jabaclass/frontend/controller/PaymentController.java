package jabaclass.frontend.controller;

import jabaclass.frontend.client.PaymentServiceClient;
import jabaclass.frontend.dto.PreparePaymentRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/payment")
@Slf4j
public class PaymentController {

    private final PaymentServiceClient paymentServiceClient;

    // Toss 결제 전 prepare 호출 (JS에서 fetch로 호출)
    @PostMapping("/prepare")
    @ResponseBody
    public ResponseEntity<Map<String, String>> prepare(@RequestBody PreparePaymentRequest request, HttpSession session) {
        String accessToken = (String) session.getAttribute("accessToken");
        try {
            paymentServiceClient.preparePayment(request, accessToken);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("결제 prepare 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Toss 결제 성공 콜백
    @GetMapping("/success")
    public String success(
        @RequestParam String paymentKey,
        @RequestParam UUID orderId,
        @RequestParam int amount,
        HttpSession session,
        Model model
    ) {
        String accessToken = (String) session.getAttribute("accessToken");
        try {
            Map result = paymentServiceClient.confirmPayment(orderId, paymentKey, amount, accessToken);
            model.addAttribute("orderId", orderId);
            model.addAttribute("amount", amount);
            return "payment/success";
        } catch (Exception e) {
            log.error("결제 confirm 실패: {}", e.getMessage());
            model.addAttribute("errorMessage", "결제 처리 중 오류가 발생했습니다.");
            return "payment/fail";
        }
    }

    // Toss 결제 실패 콜백
    @GetMapping("/fail")
    public String fail(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String message,
        @RequestParam(required = false) UUID orderId,
        Model model
    ) {
        model.addAttribute("errorCode", code);
        model.addAttribute("errorMessage", message);
        model.addAttribute("orderId", orderId);
        return "payment/fail";
    }

    @PostMapping("/deposit-complete")
    public String depositComplete(
        @RequestParam UUID orderId,
        Model model
    ) {
        // prepare 단계(PaymentService.create)에서 이미 PAID 처리 + 예치금 차감 완료
        // 여기서 updatePaymentStatus를 다시 호출하면 예치금이 이중 차감됨
        model.addAttribute("orderId", orderId);
        model.addAttribute("amount", 0);
        return "payment/success";
    }

    @PostMapping("/refund")
    public String refund(
        @RequestParam UUID orderId,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String accessToken = (String) session.getAttribute("accessToken");
        try {
            paymentServiceClient.refundPayment(orderId, accessToken);
            redirectAttributes.addFlashAttribute("paymentMessage", "환불이 완료되었습니다.");
        } catch (Exception e) {
            log.error("환불 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("paymentError", extractErrorMessage(e));
        }
        return "redirect:/mypage";
    }

    private String extractErrorMessage(Exception e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return "환불 처리 중 오류가 발생했습니다.";
        }
        return e.getMessage();
    }
}
