package flabbergast;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public abstract class LoadLibraries implements UriHandler {
	public static List<String> GenerateDefaultPaths() {
		List<String> paths = new ArrayList<String>();
		String env_var = System.getenv("FLABBERGAST_PATH");
		if (env_var != null) {
			for (String path : env_var.split(File.pathSeparator)) {
				paths.add(new File(path).getAbsolutePath());
			}
		}
		try {
			String path = Frame.class.getProtectionDomain().getCodeSource()
					.getLocation().toURI().getPath();
			paths.add(path + File.separator + ".." + File.separator + "lib"
					+ File.separator + "flabbergast");
		} catch (URISyntaxException e) {
		}
		if (!System.getProperty("os.name").startsWith("Windows")) {
			paths.add("/usr/lib/flabbergast");
			paths.add("/usr/local/lib/flabbergast");
		}
		return paths;
	}

	protected final List<String> paths = GenerateDefaultPaths();

	public void appendPath(String path) {
		paths.add(path);
	}
}
