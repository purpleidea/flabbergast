using System.IO;
using System.IO.Compression;
using System.Security.Cryptography;
using System.Text;

namespace Flabbergast
{
    public class BinaryFunctions
    {
        public static byte[] ComputeMD5(byte[] input)
        {
            return MD5.Create().ComputeHash(input);
        }

        public static byte[] ComputeSHA1(byte[] input)
        {
            return SHA1.Create().ComputeHash(input);
        }

        public static byte[] ComputeSHA256(byte[] input)
        {
            return SHA256.Create().ComputeHash(input);
        }

        public static string BytesToHex(byte[] input, string delimiter, bool upper)
        {
            var builder = new StringBuilder();
            var first = true;
            foreach (var b in input)
            {
                if (first)
                    first = false;
                else
                    builder.Append(delimiter);
                builder.Append(string.Format(upper ? "{0:X2}" : "{0:x2}", b));
            }
            return builder.ToString();
        }

        public static byte[] Compress(byte[] input)
        {
            using (var memory = new MemoryStream())
            {
                using (var gzip = new GZipStream(memory,
                    CompressionMode.Compress, true))
                {
                    gzip.Write(input, 0, input.Length);
                }
                return memory.ToArray();
            }
        }
    }

    public class Decompress : BaseMapFunctionInterop<byte[], byte[]>
    {
        public Decompress(TaskMaster task_master, SourceReference source_ref,
            Context context, Frame self, Frame container) : base(task_master, source_ref,
            context, self, container)
        {
        }

        protected override byte[] ComputeResult(byte[] input)
        {
            using (var stream = new GZipStream(new MemoryStream(input), CompressionMode.Decompress))
            using (var memory = new MemoryStream())
            {
                var buffer = new byte[4096];
                int count;
                while ((count = stream.Read(buffer, 0, buffer.Length)) > 0)
                    memory.Write(buffer, 0, count);
                return memory.ToArray();
            }
        }
    }
}