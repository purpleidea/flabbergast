package flabbergast;

import java.util.concurrent.atomic.AtomicInteger;

public class ParseInt extends Computation implements ConsumeResult {

    private AtomicInteger interlock = new AtomicInteger();
    private String input;
    private int radix;

    private SourceReference source_reference;
    private Context context;
    private Frame container;

    public ParseInt(TaskMaster task_master, SourceReference source_ref,
                    Context context, Frame self, Frame container) {
        super(task_master);
        this.source_reference = source_ref;
        this.context = context;
        this.container = self;
    }
    @Override
    public void consume(Object result) {
        if (result instanceof Stringish) {
            input = result.toString();
            if (interlock.decrementAndGet() == 0) {
                task_master.slot(this);
            }
        } else {
            task_master.reportOtherError(source_reference,
                                         "Input argument must be a string.");
        }
    }

    @Override
    protected void run() {
        if (input == null) {
            interlock.set(3);

            Computation input_lookup = new Lookup(task_master,
                                                  source_reference, new String[] {"arg"}, context);
            input_lookup.listen(this);

            Computation radix_lookup = new Lookup(task_master,
                                                  source_reference, new String[] {"radix"}, context);
            radix_lookup.listen(new ConsumeResult() {
                @Override
                public void consume(Object result) {
                    if (result instanceof Long) {
                        long radix = (Long) result;
                        ParseInt.this.radix = (int) radix;
                        if (radix < Character.MIN_RADIX) {
                            task_master.reportOtherError(source_reference,
                                                         String.format(
                                                             "Radix %s must be at least %s.",
                                                             radix, Character.MIN_RADIX));
                        } else if (radix > Character.MAX_RADIX) {
                            task_master.reportOtherError(source_reference,
                                                         String.format(
                                                             "Radix %s must be at most %s.",
                                                             radix, Character.MAX_RADIX));
                        } else if (interlock.decrementAndGet() == 0) {
                            task_master.slot(ParseInt.this);
                        }
                    } else {
                        task_master.reportOtherError(source_reference,
                                                     "Input argument must be a string.");
                    }
                }
            });

            if (interlock.decrementAndGet() > 0) {
                return;
            }
        }

        try {
            result = Long.parseLong(input, radix);
        } catch (NumberFormatException e) {
            task_master.reportOtherError(source_reference,
                                         String.format("Invalid integer “%s”.", input));
        }
    }
}
