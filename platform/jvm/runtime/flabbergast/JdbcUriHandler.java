package flabbergast;

import flabbergast.ReflectedFrame.Transform;
import flabbergast.TaskMaster.LibraryFailure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JdbcUriHandler implements UriHandler {

    public static UriHandler INSTANCE = new JdbcUriHandler();

    private static Map<String, Transform<Connection>> connection_hooks = new HashMap<String, Transform<Connection>>();
    static {
        connection_hooks.put("database", new Transform<Connection>() {
            public Object invoke(Connection c) {
                try {
                    return c.getCatalog();
                } catch (SQLException e) {
                    return Unit.NULL;
                }
            }
        });
        connection_hooks.put("product_name", new Transform<Connection>() {
            public Object invoke(Connection c) {
                try {
                    return c.getMetaData().getDatabaseProductName();
                } catch (SQLException e) {
                    return Unit.NULL;
                }
            }
        });
        connection_hooks.put("product_version", new Transform<Connection>() {
            public Object invoke(Connection c) {
                try {
                    return c.getMetaData().getDatabaseProductVersion();
                } catch (SQLException e) {
                    return Unit.NULL;
                }
            }
        });
        connection_hooks.put("driver_name", new Transform<Connection>() {
            public Object invoke(Connection c) {
                try {
                    return c.getMetaData().getDriverName();
                } catch (SQLException e) {
                    return Unit.NULL;
                }
            }
        });
        connection_hooks.put("driver_version", new Transform<Connection>() {
            public Object invoke(Connection c) {
                try {
                    return c.getMetaData().getDriverVersion();
                } catch (SQLException e) {
                    return Unit.NULL;
                }
            }
        });
        connection_hooks.put("platform", new Transform<Connection>() {
            public Object invoke(Connection c) {
                return "JDBC";
            }
        });
    }
    public static String parseUri(String uri_fragment, Properties properties,
                                  String user_param, String password_param, String db_param,
                                  Ptr<String> err) {
        int host_start = 0;
        int user_end = 0;
        while (user_end < uri_fragment.length()
                && (Character.isLetterOrDigit(uri_fragment.charAt(user_end)) || uri_fragment
                    .charAt(user_end) == '_')) {
            user_end++;
        }
        if (user_end == uri_fragment.length()) {
            // We know this is malformed.
            err.set("Missing “/” followed by database in SQL URI.");
            return null;
        } else {
            switch (uri_fragment.charAt(user_end)) {
            case '@' :
                // End of user string.
                properties.setProperty(user_param,
                                       uri_fragment.substring(0, user_end));
                host_start = user_end + 1;
                break;
            case ':' :
                // Possible password. Might be port.
                int password_end = user_end + 1;
                while (password_end < uri_fragment.length()
                        && "/@".indexOf(uri_fragment.charAt(password_end)) == -1) {
                    password_end++;
                }
                if (password_end == uri_fragment.length()) {
                    // We know this is malformed.
                    err.set("Missing “/” followed by database in SQL URI.");
                    return null;
                } else if (uri_fragment.charAt(password_end) == '@') {
                    host_start = password_end + 1;
                    properties.setProperty(user_param,
                                           uri_fragment.substring(0, user_end));
                    properties.setProperty(password_param, uri_fragment
                                           .substring(user_end + 1, password_end));
                }
                // Else, this is really the host:port.
                break;
            default :
                // This is really the host.
                break;
            }
        }
        int host_end = host_start;
        while (host_end < uri_fragment.length()
                && "/:".indexOf(uri_fragment.charAt(host_end)) == -1) {
            // IPv6 address?
            if (uri_fragment.charAt(host_end) == '[') {
                while (host_end < uri_fragment.length()
                        && uri_fragment.charAt(host_end) != ']') {
                    host_end++;
                }
            }
            host_end++;
        }
        if (host_end >= uri_fragment.length()) {
            err.set("Missing “/” followed by database in SQL URI.");
            return null;
        }
        if (uri_fragment.charAt(host_end) == ':') {
            host_end++;
            while (host_end < uri_fragment.length()
                    && Character.isDigit(uri_fragment.charAt(host_end))) {
                host_end++;
            }
            if (host_end == uri_fragment.length()) {
                err.set("Missing “/” followed by database in SQL URI.");
                return null;
            }
            if (uri_fragment.charAt(host_end) != '/') {
                err.set("Non-numeric data in port in SQL URI.");
                return null;
            }
        }
        if (uri_fragment.charAt(host_end) != '/') {
            err.set("Junk after host in SQL URI.");
            return null;
        }
        int db_end = host_end + 1;
        while (db_end < uri_fragment.length()
                && uri_fragment.charAt(db_end) != '/') {
            db_end++;
        }
        if (db_end < uri_fragment.length()) {
            err.set("Junk after database in SQL URI.");
            return null;
        }
        if (db_param == null) {
            return uri_fragment.substring(host_start);
        } else {
            properties.setProperty(db_param,
                                   uri_fragment.substring(host_end + 1));
            return uri_fragment.substring(host_start, host_end);
        }
    }

    private JdbcUriHandler() {
    }

    public String getUriName() {
        return "JDBC gateway";
    }
    private Object marshall(String s) {
        if (s == null || s.length() == 0) {
            return Unit.NULL;
        }
        return new SimpleStringish(s);
    }
    public Computation resolveUri(TaskMaster task_master, String uri,
                                  Ptr<LibraryFailure> reason) {
        if (!uri.startsWith("sql:")) {
            reason.set(LibraryFailure.MISSING);
            return null;
        }
        SourceReference src_ref = null;
        try {
            Properties params = new Properties();
            int first_colon = 5;
            while (first_colon < uri.length() && uri.charAt(first_colon) != ':') {
                first_colon++;
            }
            if (first_colon == uri.length()) {
                return new FailureComputation(task_master,
                                              new JavaSourceReference(), "Bad provider in URI “"
                                              + uri + "”.");
            }
            String provider = uri.substring(4, first_colon);
            int question_mark = first_colon;
            while (question_mark < uri.length()
                    && uri.charAt(question_mark) != '?') {
                question_mark++;
            }
            String uri_fragment = uri.substring(first_colon + 1, question_mark);
            if (question_mark < uri.length() - 1) {
                for (String param_str : uri.substring(question_mark + 1).split(
                            "&")) {
                    if (param_str.length() == 0) {
                        continue;
                    }
                    String[] parts = param_str.split("=", 2);
                    if (parts.length != 2) {
                        return new FailureComputation(task_master,
                                                      new JavaSourceReference(), "Bad parameter “"
                                                      + param_str + "”.");
                    }
                    params.setProperty(parts[0], parts[1]);
                }
            }

            Ptr<String> err = new Ptr<String> ("Bad URI.");
            Properties properties = new Properties();
            String jdbc_uri = JdbcParser.parse(provider, uri_fragment, params,
                                               properties, err);
            if (jdbc_uri == null) {
                return new FailureComputation(task_master,
                                              new JavaSourceReference(), err.get());
            }
            Connection connection = DriverManager.getConnection(jdbc_uri,
                                    properties);
            if (connection == null) {
                reason.set(LibraryFailure.CORRUPT);
                return null;
            }
            ReflectedFrame connection_proxy = ReflectedFrame.create(
                                                  task_master, connection, connection_hooks);
            connection_proxy.set("provider", new SimpleStringish(provider));
            return new Precomputation(connection_proxy);
        } catch (SQLException e) {
            if (src_ref == null) {
                src_ref = new JavaSourceReference();
            }
            return new FailureComputation(task_master, src_ref, e.getMessage());
        }
    }
}
