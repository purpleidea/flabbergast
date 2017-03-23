package flabbergast;

import java.util.zip.Inflater;
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayOutputStream;

public class Decompress extends Future {
    private InterlockedLookup interlock;
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
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.lookup(byte[].class, x->input = x, "arg");
        }
        if (!interlock.away()) return;
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
