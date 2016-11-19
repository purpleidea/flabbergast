using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.IO.Compression;
using System.Text;
using System.Threading;

namespace Flabbergast {
public class StaticFunctions {
    public static byte[] ComputeMD5(byte[] input) {
        return System.Security.Cryptography.MD5.Create().ComputeHash(input);
    }
    public static byte[] ComputeSHA1(byte[] input) {
        return System.Security.Cryptography.SHA1.Create().ComputeHash(input);
    }
    public static byte[] ComputeSHA256(byte[] input) {
        return System.Security.Cryptography.SHA256.Create().ComputeHash(input);
    }
    public static string BytesToHex(byte[] input, string delimiter, bool upper) {
        StringBuilder builder = new StringBuilder();
        bool first = true;
        foreach (byte b in input) {
            if (first) {
                first = false;
            } else {
                builder.Append(delimiter);
            }
            builder.Append(String.Format(upper ? "{0:X2}" : "{0:x2}", b));
        }
        return builder.ToString();
    }
    public static byte[] Compress(byte[] input) {
        using(var memory = new MemoryStream()) {
            using(var gzip = new GZipStream(memory,
                                            CompressionMode.Compress, true))
            {
                gzip.Write(input, 0, input.Length);
            }
            return memory.ToArray();
        }
    }
}

public class StringFromBytes : Computation {
    private System.Text.Encoding[] encodings = new System.Text.Encoding[] {
        new System.Text.UTF32Encoding(true, false, true),
        new System.Text.UTF32Encoding(false, false, true),
        new System.Text.UnicodeEncoding(true, false, true),
        new System.Text.UnicodeEncoding(false, false, true),
        new System.Text.UTF8Encoding(false, true)
    };

    private InterlockedLookup interlock;
    private byte[] input;
    private System.Text.Encoding encoding;

    private SourceReference source_reference;
    private Context context;

    public StringFromBytes(TaskMaster task_master, SourceReference source_ref,
                           Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
    }
    protected override void Run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.Lookup<byte[]>(x => this.input = x, "arg");
            interlock.Lookup<long>(index => {
                if (index >= 0 && index < encodings.Length) {
                    encoding = encodings[index];
                }
            }, "encoding");
        }
        if (!interlock.Away()) return;
        if (encoding == null) {
            task_master.ReportOtherError(source_reference, "Invalid encoding.");
            return;
        }
        try {
            result = new SimpleStringish(encoding.GetString(input));
        } catch (DecoderFallbackException e) {
            task_master.ReportOtherError(source_reference, String.Format("Cannot decode byte {0}.", e.Index));
        }
    }
}
public class FromBase64 : Computation {

    private InterlockedLookup interlock;
    private String input;

    private SourceReference source_reference;
    private Context context;

    public FromBase64(TaskMaster task_master, SourceReference source_ref,
                      Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
    }

    protected override void Run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.LookupStr(x => input = x, "arg");
        }
        if (!interlock.Away()) return;

        try {
            result = Convert.FromBase64String(input);
        } catch (Exception e) {
            task_master.ReportOtherError(source_reference, e.Message);
        }
    }
}
public class Decompress : Computation {

    private InterlockedLookup interlock;
    private byte[] input;

    private SourceReference source_reference;
    private Context context;

    public Decompress(TaskMaster task_master, SourceReference source_ref,
                      Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
    }

    protected override void Run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.Lookup<byte[]>(x => input = x, "arg");
        }
        if (!interlock.Away()) return;

        try {

            using(var stream = new GZipStream(new MemoryStream(input),  CompressionMode.Decompress)) using(var memory = new MemoryStream())
            {
                byte[] buffer = new byte[4096];
                int count;
                while ((count =  stream.Read(buffer, 0, buffer.Length)) > 0)
                {

                    memory.Write(buffer, 0, count);
                }
                result = memory.ToArray();
            }
        } catch (Exception e) {
            task_master.ReportOtherError(source_reference, e.Message);
        }
    }
}
}
