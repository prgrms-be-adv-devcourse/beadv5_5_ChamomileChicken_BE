package jabaclass.frontend.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jabaclass.frontend.client.OrderServiceClient;
import jabaclass.frontend.client.ProductServiceClient;
import jabaclass.frontend.client.UserServiceClient;
import jabaclass.frontend.dto.DepositHistoryItemDto;
import jabaclass.frontend.dto.OrderSummaryDto;
import jabaclass.frontend.dto.ProductDto;
import jabaclass.frontend.dto.UserInfoDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
@Slf4j
public class MypageController {

	private final UserServiceClient userServiceClient;
	private final OrderServiceClient orderServiceClient;
	private final ProductServiceClient productServiceClient;

	@GetMapping
	public String mypage(HttpSession session, Model model) {
		String accessToken = (String)session.getAttribute("accessToken");
		try {
			UserInfoDto userInfo = userServiceClient.getMyInfo(accessToken);
			model.addAttribute("userInfo", userInfo);
		} catch (Exception e) {
			log.error("사용자 정보 조회 실패: {}", e.getMessage());
			model.addAttribute("userInfoError", "사용자 정보를 불러올 수 없습니다.");
		}
		try {
			List<OrderSummaryDto> orders = orderServiceClient.getMyOrders(accessToken);
			// 세션 상태 오버라이드 (비동기 처리 지연 대응)
			for (OrderSummaryDto order : orders) {
				if (session.getAttribute("refunded_order_" + order.getId()) != null) {
					order.setStatus("REFUNDED");
				}
			}

			try {
				enrichOrdersWithProductInfo(orders);
			} catch (Exception e) {
				log.warn("상품 정보 연동 실패 (Product 서비스 점검 중 가능성): {}", e.getMessage());
			}

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

	private void enrichOrdersWithProductInfo(List<OrderSummaryDto> orders) {
		Set<UUID> scheduleIds = orders.stream()
			.map(OrderSummaryDto::getProductScheduleId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		Map<UUID, ProductDto> productsByScheduleId = productServiceClient.getProductsByScheduleIds(scheduleIds);

		for (OrderSummaryDto order : orders) {
			ProductDto product = productsByScheduleId.get(order.getProductScheduleId());
			if (product == null) {
				continue;
			}
			order.setProductId(product.getId());
			order.setProductTitle(product.getTitle());
			order.setProductDescriptionImage(product.getThumbnailPath());
			order.setProductPrice(product.getPrice());
		}
	}
}
