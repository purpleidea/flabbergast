package flabbergast;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharacterCodingException;

import java.util.concurrent.atomic.AtomicInteger;

public class StringFromBytes extends Computation {
    private static final String[] ENCODINGS = new String[] {"UTF-32BE", "UTF-32LE", "UTF-16BE", "UTF-16LE", "UTF-8"};
    private AtomicInteger interlock = new AtomicInteger();
    private byte[] input;
    private String encoding;

    private SourceReference source_reference;
    private Context context;

    public StringFromBytes(TaskMaster task_master,
                           SourceReference source_ref, Context context, Frame self,
                           Frame container) {
        super(task_master);
        this.source_reference = source_ref;
        this.context = context;
    }
    @Override
    protected void run() {
        if (input == null) {
            interlock.set(3);
            Computation input_lookup = new Lookup(task_master,
                                                  source_reference, new String[] {"arg"}, context);
            input_lookup.listen(new ConsumeResult() {

                @Override
                public void consume(Object result) {
                    if (result instanceof byte[]) {
                        input = (byte[])result;
                        if (interlock.decrementAndGet() == 0) {
                            task_master.slot(StringFromBytes.this);
                        }
                    } else {
                        task_master.reportOtherError(source_reference,
                                                     "Input argument must be a Bin.");
                    }
                }
            }

                               );
            Computation encoding_lookup = new Lookup(task_master,
                    source_reference, new String[] {"encoding"}, context);
            encoding_lookup.listen(new ConsumeResult() {

                @Override
                public void consume(Object result) {
                    if (result instanceof Long) {
                        long index = (Long)result;
                        if (index >= 0 && index < ENCODINGS.length) {
                            encoding = ENCODINGS[(int) index];
                            if (interlock.decrementAndGet() == 0) {
                                task_master.slot(StringFromBytes.this);
                                return;
                            }
                        }
                    }
                    task_master.reportOtherError(source_reference,
                                                 "Invalid encoding.");
                }
            }

                                  );
            if (interlock.decrementAndGet() > 0) {
                return;
            }
        }
        try {
            CharsetDecoder decode = Charset.forName(encoding).newDecoder();
            ByteBuffer bytes = ByteBuffer.wrap(input);
            bytes.mark();
            result = new SimpleStringish(decode.decode(bytes).toString());
            bytes.reset();
        } catch (CharacterCodingException e) {
            task_master.reportOtherError(source_reference, "Cannot decode: " + e.getMessage());
        }
    }
}
