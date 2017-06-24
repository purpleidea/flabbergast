package flabbergast;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

public class BinaryFunctions {

  public static String bytesToHex(byte[] input, String delimiter, boolean upper) {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (byte b : input) {
      if (first) {
        first = false;
      } else {
        builder.append(delimiter);
      }
      builder.append(String.format(upper ? "%02X" : "%02x", b));
    }
    return builder.toString();
  }

  public static byte[] checksum(byte[] input, String algorithm) {
    try {
      MessageDigest complete = MessageDigest.getInstance(algorithm);
      complete.update(input);
      return complete.digest();
    } catch (NoSuchAlgorithmException e) {
      return new byte[0];
    }
  }

  public static byte[] compress(byte[] input) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try {
        GZIPOutputStream gzip = new GZIPOutputStream(output);
        try {
          gzip.write(input);
        } finally {
          gzip.close();
        }

      } finally {
        output.close();
      }
      return output.toByteArray();
    } catch (Exception e) {
      return new byte[0];
    }
  }
}
