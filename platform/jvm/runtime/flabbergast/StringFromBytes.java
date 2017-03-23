package flabbergast;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharacterCodingException;

public class StringFromBytes extends Future {
    private static final String[] ENCODINGS = new String[] {"UTF-32BE", "UTF-32LE", "UTF-16BE", "UTF-16LE", "UTF-8"};
    private InterlockedLookup interlock;
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
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.lookup(byte[].class, x->input = x, "arg");
            interlock.lookup(Long.class,
            index -> {
                if (index >= 0 && index < ENCODINGS.length) {
                    encoding = ENCODINGS[ index.intValue()];
                }
            }, "encoding");
        }
        if (!interlock.away()) return;
        if (encoding == null) {
            task_master.reportOtherError(source_reference,
                                         "Invalid encoding.");
            return;
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
