package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;

public class UriInstantiator implements UriHandler {

	private UriLoader loader;

	public UriInstantiator(UriLoader loader) {
		this.loader = loader;
	}

	public String getUriName() {
		return loader.getUriName();
	}

	public Computation resolveUri(TaskMaster task_master, String uri,
			Ptr<LibraryFailure> reason) {
		Class<? extends Computation> t = loader.resolveUri(uri, reason);
		if (t == null) {
			return null;
		}

		try {
			Computation computation;
			computation = t.getDeclaredConstructor(TaskMaster.class)
					.newInstance(task_master);
			return computation;
		} catch (Exception e) {
			reason.set(LibraryFailure.CORRUPT);
			return null;
		}
	}
}
