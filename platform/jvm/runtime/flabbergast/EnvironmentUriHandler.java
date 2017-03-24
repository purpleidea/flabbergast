package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;
import java.util.regex.Pattern;

public class EnvironmentUriHandler implements UriHandler {
    public static final EnvironmentUriHandler INSTANCE = new EnvironmentUriHandler();
    private EnvironmentUriHandler() {
    }
    public String getUriName() {
        return "Environment variables";
    }
    public int getPriority() {
        return 0;
    }
    public Future resolveUri(TaskMaster task_master, String uri,
                             Ptr<LibraryFailure> reason) {
        if (!uri.startsWith("env:")) {
            reason.set(LibraryFailure.MISSING);
            return null;
        }
        String name = uri.substring(4);
        if (!Pattern.matches("[A-Z_][A-Z0-9_]*", name)) {
            reason.set(LibraryFailure.BAD_NAME);
            return null;
        }
        String content = System.getenv(name);
        return new Precomputation(content == null
                                  ? (Object) Unit.NULL
                                  : new SimpleStringish(content));
    }
}
