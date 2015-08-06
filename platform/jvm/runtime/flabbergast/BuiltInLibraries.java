package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;

public class BuiltInLibraries implements UriLoader {
	public static final BuiltInLibraries INSTANCE = new BuiltInLibraries();

	private BuiltInLibraries() {
	}

	@Override
	public String getUriName() {
		return "built-in libraries";
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends Computation> resolveUri(String uri,
			Ptr<LibraryFailure> failure) {
		if (!uri.startsWith("lib:"))
			return null;
		String type_name = "flabbergast.library."
				+ uri.substring(4).replace('/', '.');
		try {
			return (Class<? extends Computation>) Class.forName(type_name);
		} catch (ClassNotFoundException e) {
			failure.set(LibraryFailure.MISSING);
			return null;
		}
	}
}
