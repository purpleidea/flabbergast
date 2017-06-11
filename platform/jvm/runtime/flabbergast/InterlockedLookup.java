package flabbergast;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Consumer;

import java.util.concurrent.atomic.AtomicInteger;

public class InterlockedLookup {
    private AtomicInteger interlock = new AtomicInteger(1);
    private boolean isAway;
    private final Future owner;
    private final TaskMaster task_master;
    private final SourceReference source_reference;
    private final Context context;
    public InterlockedLookup(Future owner, TaskMaster task_master, SourceReference source_reference, Context context) {
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
        Future input_lookup = new Lookup(task_master, source_reference, names, context);
        input_lookup.listen(input_result -> {
            boolean isNull = input_result == Unit.NULL;
            if (isNull || clazz.isInstance(input_result)) {
                if (writer.test(isNull ? null : (T) input_result)) {
                    if (interlock.decrementAndGet() == 0) {
                        task_master.slot(owner);
                    }
                }
            } else {
                task_master.reportOtherError(source_reference, String.format("“%s” has type %s but expected %s.", String.join(".", names), SupportFunctions.nameForClass(input_result.getClass()), SupportFunctions.nameForClass(clazz)));
            }
        });
    }

    public <T> void lookup(Class<T> clazz, Consumer<T> writer, String... names) {
        if (clazz.equals(String.class)) {
            lookup(Stringish.class, sish -> writer.accept((T)sish.toString()), names);
        } else {
            lookupHelper(clazz, x -> {
                writer.accept(x);
                return true;
            }, names);
        }
    }
    public void lookupStr(Consumer<String> writer, String... names) {
        lookup(Stringish.class, sish -> writer.accept(sish.toString()), names);
    }
    public <T> void lookupMarshalled(Class<T> clazz, String error, Consumer<T> writer, String... names) {
        if (isAway) {
            throw new UnsupportedOperationException("Cannot lookup after finish.");
        }
        interlock.incrementAndGet();
        Future input_lookup = new Lookup(task_master, source_reference, names, context);
        input_lookup.listen(input_result -> {
            if (!(input_result instanceof Frame)) {
                task_master.reportOtherError(source_reference, String.format("“%s” has type %s but expected Frame.", String.join(".", names), SupportFunctions.nameForClass(input_result.getClass())));
                return;
            }
            if (input_result instanceof ReflectedFrame) {
                Object backing = ((ReflectedFrame) input_result).getBacking();
                if (clazz.isInstance(backing)) {
                    writer.accept((T) backing);
                    if (interlock.decrementAndGet() == 0) {
                        task_master.slot(owner);
                    }
                    return ;
                }
            }
            task_master.reportOtherError(source_reference, String.format(error, String.join(".", names)));
        });
    }

    public boolean away() {
        if (isAway) {
            return true;
        }
        isAway = true;
        return interlock.decrementAndGet() == 0;
    }
}
