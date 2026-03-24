package jabaclass.product.infrastructure.acl.dto;

public enum SellerRole {
	USER("USER", "일반 사용자"),
	SELLER("SELLER", "판매자");

	private final String roleName;
	private final String description;

	SellerRole(String roleName, String description) {
		this.roleName = roleName;
		this.description = description;
	}

	public String getRoleName() {
		return roleName;
	}

	public String getDescription() {
		return description;
	}

	public static String fromRoleName() {
		return SELLER.getRoleName();
	}
}
