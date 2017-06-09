package flabbergast;

import java.util.zip.Inflater;
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayOutputStream;

public class Decompress extends BaseMapFunctionInterop<byte[], byte[]> {
    public Decompress(TaskMaster task_master,
                      SourceReference source_ref, Context context, Frame self,
                      Frame container) {
        super(byte[].class, byte[].class, task_master, source_ref, context, self, container);
    }
    @Override
    protected byte[] computeResult(byte[] input) throws Exception {
        try(GZIPInputStream gunzip = new GZIPInputStream(new ByteArrayInputStream(input));
                    ByteArrayOutputStream output = new ByteArrayOutputStream();) {

            int count;
            byte buffer[] = new byte[1024];
            while ((count = gunzip.read(buffer, 0, buffer.length)) > 0) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }
}
