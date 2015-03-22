package flabbergast;

public class BuiltInLibraries implements UriHandler {
	public static final BuiltInLibraries INSTANCE = new BuiltInLibraries();

	private BuiltInLibraries() {
	}

	@Override
	public String getUriName() {
		return "built-in libraries";
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends Computation> resolveUri(String uri, Ptr<Boolean> stop) {
		stop.set(false);
		if (!uri.startsWith("lib:"))
			return null;
		String type_name = "flabbergast.library."
				+ uri.substring(4).replace('/', '.');
		try {
			return (Class<? extends Computation>) Class.forName(type_name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
