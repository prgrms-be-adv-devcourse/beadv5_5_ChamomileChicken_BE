package jabaclass.ai.domain.model;

public record UserVector(
	float[] vector
) {
	public boolean isEmpty() {
		if (vector == null || vector.length == 0) {
			return true;
		}

		for (float value : vector) {
			if (value != 0.0f) {
				return false;
			}
		}

		return true;
	}
}
