package flabbergast;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import flabbergast.TaskMaster.LibraryFailure;

public class CurrentInformation implements UriHandler {

    private Map<String, Computation> information = new HashMap<String, Computation>();

    public CurrentInformation(boolean interactive) {
        String currentDirectory = ".";
        try {
            currentDirectory = new File(".").getCanonicalPath().toString();
        } catch (IOException e) {}
        information.put("interactive", new Precomputation(interactive));
        information.put("login", new Precomputation(new SimpleStringish(System.getProperty("user.name"))));
        information.put("directory", new Precomputation(new SimpleStringish(currentDirectory)));
        information.put("version", new Precomputation(Configuration.VERSION));
        information.put("vm/name", new Precomputation(new SimpleStringish("JVM")));
        information.put("vm/vendor", new Precomputation(new SimpleStringish(System.getProperty("java.vendor"))));
        information.put("vm/version", new Precomputation(new SimpleStringish(System.getProperty("java.version"))));
    }

    public String getUriName() {
        return "current information";
    }

    public Computation resolveUri(TaskMaster task_master, String uri,
                                  Ptr<LibraryFailure> reason) {

        if (!uri.startsWith("current:"))
            return null;
        if (information.containsKey(uri.substring(8))) {
            reason.set(null);
            return information.get(uri.substring(8));
        }
        return null;
    }
}
