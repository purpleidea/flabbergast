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

    private int interlock = 3;
    private byte[] input;
    private System.Text.Encoding encoding;

    private SourceReference source_reference;
    private Context context;

    public StringFromBytes(TaskMaster task_master, SourceReference source_ref,
                           Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
    }
    protected override bool Run() {
        if (input == null) {
            Computation input_lookup = new Lookup(task_master, source_reference, new [] {"arg"}, context);
            input_lookup.Notify(input_result => {
                if (input_result is byte[]) {
                    input = (byte[]) input_result;
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference, "Input argument must be a Bin.");
                }
            });

            Computation encoding_lookup = new Lookup(task_master, source_reference, new [] {"encoding"}, context);
            encoding_lookup.Notify(encoding_result => {
                if (encoding_result is long) {
                    var index = (long) encoding_result;
                    if (index >= 0 && index < encodings.Length) {
                        encoding = encodings[index];
                        if (Interlocked.Decrement(ref interlock) == 0) {
                            task_master.Slot(this);
                            return;
                        }
                    }
                }
                task_master.ReportOtherError(source_reference, "Invalid encoding.");
            });

            if (Interlocked.Decrement(ref interlock) > 0) {
                return false;
            }
        }
        try {
            result = new SimpleStringish(encoding.GetString(input));
            return true;
        } catch (DecoderFallbackException e) {
            task_master.ReportOtherError(source_reference, String.Format("Cannot decode byte {0}.", e.Index));
            return false;
        }
    }
}
public class FromBase64 : Computation {

    private int interlock = 2;
    private String input;

    private SourceReference source_reference;
    private Context context;

    public FromBase64(TaskMaster task_master, SourceReference source_ref,
                      Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
    }

    protected override bool Run() {
        if (input == null) {
            var input_lookup = new Lookup(task_master, source_reference,
                                          new String[] {"arg"}, context);
            input_lookup.Notify(input_result => {
                if (input_result is Stringish) {
                    input = input_result.ToString();
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference,
                                                 "Input argument must be a string.");
                }
            });

            if (Interlocked.Decrement(ref interlock) > 0) {
                return false;
            }
        }

        try {
            result = Convert.FromBase64String(input);
            return true;
        } catch (Exception e) {
            task_master.ReportOtherError(source_reference, e.Message);
            return false;
        }
    }
}
public class Decompress : Computation {

    private int interlock = 2;
    private byte[] input;

    private SourceReference source_reference;
    private Context context;

    public Decompress(TaskMaster task_master, SourceReference source_ref,
                      Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
    }

    protected override bool Run() {
        if (input == null) {
            var input_lookup = new Lookup(task_master, source_reference,
                                          new String[] {"arg"}, context);
            input_lookup.Notify(input_result => {
                if (input_result is byte[]) {
                    input = (byte[])input_result;
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference,
                                                 "Input argument must be a Bin.");
                }
            });

            if (Interlocked.Decrement(ref interlock) > 0) {
                return false;
            }
        }

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
                return true;
            }
        } catch (Exception e) {
            task_master.ReportOtherError(source_reference, e.Message);
            return false;
        }
    }
}
}
