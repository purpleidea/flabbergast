package flabbergast;

public class BlackholeComputation extends Computation {

	public static Computation INSTANCE = new BlackholeComputation();

	private BlackholeComputation() {
		super(null);
	}

	@Override
	protected void run() {
	}
}
