package jabaclass.frontend.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jabaclass.frontend.client.OrderServiceClient;
import jabaclass.frontend.client.ProductServiceClient;
import jabaclass.frontend.client.UserServiceClient;
import jabaclass.frontend.dto.CreateOrderRequest;
import jabaclass.frontend.dto.CreateOrderResponse;
import jabaclass.frontend.dto.CreateProductRequest;
import jabaclass.frontend.dto.CreateScheduleRequest;
import jabaclass.frontend.dto.ProductDto;
import jabaclass.frontend.dto.ScheduleDto;
import jabaclass.frontend.dto.UserInfoDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PageController {

	private final ProductServiceClient productServiceClient;
	private final OrderServiceClient orderServiceClient;
	private final UserServiceClient userServiceClient;

	@Value("${toss.client-key}")
	private String tossClientKey;

	@Value("${toss.success-url}")
	private String tossSuccessUrl;

	@Value("${toss.fail-url}")
	private String tossFailUrl;

	// 메인 → 상품 목록으로 리다이렉트
	@GetMapping("/")
	public String index() {
		return "redirect:/products";
	}

	// 상품 목록
	@GetMapping("/products")
	public String productList(HttpSession session, Model model) {
		List<ProductDto> products = productServiceClient.getProducts();
		model.addAttribute("products", products);
		model.addAttribute("role", session.getAttribute("role"));
		return "index";
	}

	// 상품 상세
	@GetMapping("/products/{productId}")
	public String productDetail(@PathVariable UUID productId, HttpSession session, Model model) {
		try {
			ProductDto product = productServiceClient.getProduct(productId);
			List<ScheduleDto> schedules = productServiceClient.getSchedules(productId);
			model.addAttribute("product", product);
			model.addAttribute("schedules", schedules);

			BigDecimal depositBalance = BigDecimal.ZERO;
			String accessToken = (String)session.getAttribute("accessToken");
			if (accessToken != null) {
				try {
					UserInfoDto userInfo = userServiceClient.getMyInfo(accessToken);
					depositBalance = userInfo.getDeposit();
				} catch (Exception e) {
					log.warn("예치금 잔액 조회 실패: {}", e.getMessage());
				}
			}
			model.addAttribute("depositBalance", depositBalance);
			return "product";
		} catch (Exception e) {
			log.error("상품 상세 조회 실패: {}", e.getMessage());
			model.addAttribute("errorMessage", e.getMessage());
			return "error";
		}
	}

	// 주문 생성 → 결제 페이지로 이동
	@PostMapping("/orders")
	public String createOrder(
		@RequestParam UUID productId,
		@RequestParam UUID scheduleId,
		@RequestParam(defaultValue = "1") Integer quantity,
		@RequestParam(defaultValue = "0") BigDecimal depositAmount,
		HttpSession session,
		Model model
	) {
		String accessToken = (String)session.getAttribute("accessToken");

		CreateOrderRequest request = new CreateOrderRequest();
		request.setProductId(productId);
		request.setProductScheduleId(scheduleId);
		request.setQuantity(quantity);
		request.setDepositAmount(depositAmount);

		CreateOrderResponse order = orderServiceClient.createOrder(request, accessToken);

		return "redirect:/payment/checkout?orderId=" + order.getId()
			+ "&buyerId=" + order.getBuyerId()
			+ "&productId=" + order.getProductId()
			+ "&productUserId=" + order.getProductUserId()
			+ "&amount=" + order.getPaymentAmount().intValue()
			+ "&depositAmount=" + order.getDepositAmount().intValue();
	}

	// 상품 등록 폼
	@GetMapping("/seller/products/new")
	public String productForm(HttpSession session, Model model) {
		String role = (String)session.getAttribute("role");
		if (!"SELLER".equals(role)) {
			return "redirect:/products";
		}
		return "seller/product-form";
	}

	// 상품 등록 처리
	@PostMapping("/seller/products/new")
	public String createProduct(
		@ModelAttribute CreateProductRequest request,
		@RequestParam(value = "scheduleDt", required = false) List<String> scheduleDts,
		@RequestParam(value = "startTime", required = false) List<String> startTimes,
		@RequestParam(value = "endTime", required = false) List<String> endTimes,
		@RequestParam(value = "imageIds", required = false) List<String> imageIds,
		HttpSession session,
		Model model
	) {
		String accessToken = (String)session.getAttribute("accessToken");

		try {
			UserInfoDto userInfo = userServiceClient.getMyInfo(accessToken);
			request.setSellerId(userInfo.getUserId());
			request.setStatus("ENABLE");
			request.setImageIds(imageIds);
			UUID productId = productServiceClient.createProduct(request, accessToken);

			if (scheduleDts != null) {
				for (int i = 0; i < scheduleDts.size(); i++) {
					CreateScheduleRequest schedule = new CreateScheduleRequest();
					schedule.setScheduleDt(scheduleDts.get(i));
					schedule.setStartTime(startTimes.get(i));
					schedule.setEndTime(endTimes.get(i));
					productServiceClient.createSchedule(productId, schedule, accessToken);
				}
			}

			return "redirect:/products";
		} catch (Exception e) {
			log.error("상품 등록 실패: {}", e.getMessage());
			model.addAttribute("errorMessage", "상품 등록에 실패했습니다.");
			return "seller/product-form";
		}
	}

	// 결제 페이지 (Toss 위젯)
	@GetMapping("/payment/checkout")
	public String checkoutPage(
		@RequestParam UUID orderId,
		@RequestParam UUID buyerId,
		@RequestParam UUID productId,
		@RequestParam UUID productUserId,
		@RequestParam int amount,
		@RequestParam int depositAmount,
		Model model
	) {
		model.addAttribute("orderId", orderId.toString());
		model.addAttribute("buyerId", buyerId.toString());
		model.addAttribute("productId", productId.toString());
		model.addAttribute("productUserId", productUserId.toString());
		model.addAttribute("amount", amount);
		model.addAttribute("depositAmount", depositAmount);
		model.addAttribute("tossClientKey", tossClientKey);
		model.addAttribute("tossSuccessUrl", tossSuccessUrl);
		model.addAttribute("tossFailUrl", tossFailUrl);
		return "payment/checkout";
	}
}
