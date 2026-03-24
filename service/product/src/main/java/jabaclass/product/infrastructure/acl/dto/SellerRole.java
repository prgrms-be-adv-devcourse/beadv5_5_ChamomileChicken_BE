package jabaclass.product.infrastructure.acl.dto;

public enum SellerRole {
	SELLER_FALSE("USER", "일반 사용자"),
	SELLER_TRUE("SELLER", "판매자");

	private final String roleName;
	private final String description;

	SellerRole(String roleName, String description) {
		this.roleName = roleName;
		this.description = description;
	}

	public String getRoleName() {
		return this.roleName;
	}
}
