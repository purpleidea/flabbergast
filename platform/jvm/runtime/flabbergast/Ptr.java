package flabbergast;

public class Ptr<T> {
	private T value;

	public Ptr() {
		this(null);
	}

	Ptr(T initial) {
		value = initial;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}
}
