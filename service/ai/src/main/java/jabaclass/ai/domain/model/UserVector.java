package jabaclass.ai.domain.model;

public record UserVector(
	float[] vector
) {
	public boolean isEmpty() {
		return vector == null || vector.length == 0;
	}
}
