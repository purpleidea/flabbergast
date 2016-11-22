package flabbergast;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Consumer;

import java.util.concurrent.atomic.AtomicInteger;

public class InterlockedLookup {
    private AtomicInteger interlock = new AtomicInteger(1);
    private boolean isAway;
    private final Computation owner;
    private final TaskMaster task_master;
    private final SourceReference source_reference;
    private final Context context;
    public InterlockedLookup(Computation owner, TaskMaster task_master, SourceReference source_reference, Context context) {
        this.task_master = task_master;
        this.owner = owner;
        this.source_reference = source_reference;
        this.context = context;
    }

    private <T> void lookupHelper(Class<T> clazz, Predicate<T>writer, String... names) {
        if (isAway) {
            throw new UnsupportedOperationException("Cannot lookup after finish.");
        }
        interlock.incrementAndGet();
        Computation input_lookup = new Lookup(task_master, source_reference, names, context);
        input_lookup.listen(input_result -> {
            if (clazz.isAssignableFrom(input_result.getClass())) {
                if (writer.test((T) input_result) && interlock.decrementAndGet() == 0) {
                    task_master.slot(owner);
                }
            } else {
                task_master.reportOtherError(source_reference, String.format("“%s” has type %s but expected %s.", String.join(".", names), SupportFunctions.nameForClass(input_result.getClass()), SupportFunctions.nameForClass(clazz)));
            }
        });
    }

    public <T> void lookup(Class<T> clazz, Consumer<T> writer, String... names) {
        lookupHelper(clazz, x -> {
            writer.accept(x);
            return true;
        }, names);
    }
    public void lookupStr(Consumer<String> writer, String... names) {
        lookup(Stringish.class, sish -> writer.accept(sish.toString()), names);
    }
    public <T> void lookupMarshalled(Class<T> clazz, Consumer<T> writer, String error, String... names) {
        lookupHelper(Frame.class, return_value -> {
            if (return_value instanceof ReflectedFrame) {
                Object backing = ((ReflectedFrame) return_value).getBacking();
                if (clazz.isAssignableFrom(backing.getClass())) {
                    writer.accept((T) backing);
                    return true;
                }
            }
            task_master.reportOtherError(source_reference, String.format(error, String.join(".", names)));
            return false;
        }, names);
    }

    public boolean away() {
        if (isAway) {
            return true;
        }
        isAway = true;
        return interlock.decrementAndGet() == 0;
    }
}
