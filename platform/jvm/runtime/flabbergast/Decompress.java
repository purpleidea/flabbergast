package flabbergast;

import java.util.zip.Inflater;
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayOutputStream;

import java.util.concurrent.atomic.AtomicInteger;

public class Decompress extends Computation {
    private AtomicInteger interlock = new AtomicInteger();
    private byte[] input;

    private SourceReference source_reference;
    private Context context;

    public Decompress(TaskMaster task_master,
                      SourceReference source_ref, Context context, Frame self,
                      Frame container) {
        super(task_master);
        this.source_reference = source_ref;
        this.context = context;
    }
    @Override
    protected void run() {
        if (input == null) {
            interlock.set(2);
            Computation input_lookup = new Lookup(task_master,
                                                  source_reference, new String[] {"arg"}, context);
            input_lookup.listen(new ConsumeResult() {

                @Override
                public void consume(Object result) {
                    if (result instanceof byte[]) {
                        input = (byte[])result;
                        if (interlock.decrementAndGet() == 0) {
                            task_master.slot(Decompress.this);
                        }
                    } else {
                        task_master.reportOtherError(source_reference,
                                                     "Input argument must be a Bin.");
                    }
                }
            }

                               );
            if (interlock.decrementAndGet() > 0) {
                return;
            }
        }
        try {
            GZIPInputStream gunzip = new GZIPInputStream(new ByteArrayInputStream(input));
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            int count;
            byte buffer[] = new byte[1024];
            while ((count = gunzip.read(buffer, 0, buffer.length)) > 0) {
                output.write(buffer, 0, count);
            }
            result = output.toByteArray();
            return;
        } catch (Exception e) {
            task_master.reportOtherError(source_reference, "Cannot uncompress: " + e.getMessage());
        }
    }
}
