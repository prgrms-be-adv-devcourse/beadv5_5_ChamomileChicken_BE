package jabaclass.frontend.controller;

import jabaclass.frontend.client.OrderServiceClient;
import jabaclass.frontend.client.UserServiceClient;
import jabaclass.frontend.dto.DepositHistoryItemDto;
import jabaclass.frontend.dto.OrderSummaryDto;
import jabaclass.frontend.dto.UserInfoDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
@Slf4j
public class MypageController {

    private final UserServiceClient userServiceClient;
    private final OrderServiceClient orderServiceClient;

    @GetMapping
    public String mypage(HttpSession session, Model model) {
        String accessToken = (String) session.getAttribute("accessToken");
        try {
            UserInfoDto userInfo = userServiceClient.getMyInfo(accessToken);
            model.addAttribute("userInfo", userInfo);
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            model.addAttribute("userInfoError", "사용자 정보를 불러올 수 없습니다.");
        }
        try {
            List<OrderSummaryDto> orders = orderServiceClient.getMyOrders(accessToken);
            model.addAttribute("orders", orders);
        } catch (Exception e) {
            log.error("주문 내역 조회 실패: {}", e.getMessage());
            model.addAttribute("ordersError", "주문 내역을 불러올 수 없습니다.");
        }
        try {
            List<DepositHistoryItemDto> depositHistories = userServiceClient.getDepositHistories(accessToken);
            model.addAttribute("depositHistories", depositHistories);
        } catch (Exception e) {
            log.error("예치금 이력 조회 실패: {}", e.getMessage());
            model.addAttribute("depositHistoriesError", "예치금 이력을 불러올 수 없습니다.");
        }
        return "mypage";
    }
}