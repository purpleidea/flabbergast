package flabbergast;

public class BlackholeFuture extends Future {

    public static Future INSTANCE = new BlackholeFuture();

    private BlackholeFuture() {
        super(null);
    }

    @Override
    protected void run() {
    }
}
