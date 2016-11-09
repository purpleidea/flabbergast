package flabbergast;

import java.net.IDN;

import java.util.concurrent.atomic.AtomicInteger;

public class Punycode extends Computation {
    private AtomicInteger interlock = new AtomicInteger();
    private String input;
    private boolean encode;
    private boolean allow_unassigned;
    private boolean strict_ascii;

    private SourceReference source_reference;
    private Context context;

    public Punycode(TaskMaster task_master,
                    SourceReference source_ref, Context context, Frame self,
                    Frame container) {
        super(task_master);
        this.source_reference = source_ref;
        this.context = context;
    }
    @Override
    protected void run() {
        if (input == null) {
            interlock.set(5);
            new Lookup(task_master,
            source_reference, new String[] {"arg"}, context).listen(new ConsumeResult() {

                @Override
                public void consume(Object result) {
                    if (result instanceof Stringish) {
                        input = result.toString();
                        if (interlock.decrementAndGet() == 0) {
                            task_master.slot(Punycode.this);
                        }
                    } else {
                        task_master.reportOtherError(source_reference,
                                                     "Input argument must be a string.");
                    }
                }
            }

                                                                              );
            new Lookup(task_master,
            source_reference, new String[] {"encode"}, context).listen(new ConsumeResult() {

                @Override
                public void consume(Object result) {
                    if (result instanceof Boolean) {
                        encode = (Boolean) result;
                        if (interlock.decrementAndGet() == 0) {
                            task_master.slot(Punycode.this);
                        }
                    } else {
                        task_master.reportOtherError(source_reference,
                                                     "“encode” argument must be a Boolean.");
                    }
                }
            }

                                                                                 );
            new Lookup(task_master,
            source_reference, new String[] {"allow_unassigned"}, context).listen(new ConsumeResult() {

                @Override
                public void consume(Object result) {
                    if (result instanceof Boolean) {
                        allow_unassigned = (Boolean) result;
                        if (interlock.decrementAndGet() == 0) {
                            task_master.slot(Punycode.this);
                        }
                    } else {
                        task_master.reportOtherError(source_reference,
                                                     "“allow_unassigned” argument must be a Boolean.");
                    }
                }
            }

                                                                                           );
            new Lookup(task_master,
            source_reference, new String[] {"strict_ascii"}, context).listen(new ConsumeResult() {

                @Override
                public void consume(Object result) {
                    if (result instanceof Boolean) {
                        strict_ascii = (Boolean) result;
                        if (interlock.decrementAndGet() == 0) {
                            task_master.slot(Punycode.this);
                        }
                    } else {
                        task_master.reportOtherError(source_reference,
                                                     "“strict_ascii” argument must be a Boolean.");
                    }
                }
            }

                                                                                       );
            if (interlock.decrementAndGet() > 0) {
                return;
            }
        }
        try {
            int flags = (allow_unassigned ? IDN.ALLOW_UNASSIGNED  : 0) | (strict_ascii ? IDN.USE_STD3_ASCII_RULES : 0);
            result = new SimpleStringish(encode ? IDN.toASCII(input, flags) : IDN.toUnicode(input, flags));
            return;
        } catch (IllegalArgumentException e) {
            task_master.reportOtherError(source_reference, "Inavlid puncody: " + e.getMessage());
        }
    }
}
