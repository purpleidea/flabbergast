package flabbergast;

public abstract class LoadLibraries implements UriLoader {

    private ResourcePathFinder finder;

    public ResourcePathFinder getFinder() {
        return finder;
    }
    public void setFinder(ResourcePathFinder finder) {
        this.finder = finder;
    }
}
