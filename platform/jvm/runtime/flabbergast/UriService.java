package flabbergast;

import java.util.EnumSet;

public interface UriService {

    UriHandler create(ResourcePathFinder finder, EnumSet<LoadRule> flags);
}
