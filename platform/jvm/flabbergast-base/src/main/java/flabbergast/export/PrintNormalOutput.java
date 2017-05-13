package flabbergast.export;

import flabbergast.lang.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Print a result to standard output or an output file
 *
 * <p>This only opens the output device if valid output has been produced
 *
 * <p>Strings, or values coercible to strings, are printed. Binary blobs are written as binary if
 * the output device is a file. Frames are searched for <tt>value</tt> attributes containing
 * printable values.
 */
public abstract class PrintNormalOutput implements Consumer<Any> {
  /** Print to the user's console. */
  public static final PrintNormalOutput TO_STANDARD_OUTPUT =
      new PrintNormalOutput(System.err::println) {
        @Override
        protected boolean writeToFile(byte[] data) {
          for (final var charset :
              new Charset[] {
                StandardCharsets.UTF_8, StandardCharsets.UTF_16, Charset.defaultCharset()
              }) {
            try {
              return writeToFile(charset.newDecoder().decode(ByteBuffer.wrap(data)).toString());
            } catch (CharacterCodingException e) {
              // Suppress
            }
          }
          errorHandler.accept("Refusing to print raw data to standard output.");
          return false;
        }

        @Override
        protected boolean writeToFile(String data) {
          System.out.println(data);
          return true;
        }
      };

  /**
   * Write output to a file.
   *
   * <p>If there is an error, the file will not be modified.
   *
   * @param outputFilename the output file name to replace
   * @param errorHandler a callback to handle any errors
   */
  public static PrintNormalOutput toFile(Path outputFilename, Consumer<String> errorHandler) {
    return new PrintNormalOutput(errorHandler) {

      @Override
      protected boolean writeToFile(byte[] data) {
        try (var out = Files.newOutputStream(outputFilename)) {
          out.write(data);
          return true;
        } catch (final IOException e) {
          errorHandler.accept(e.getMessage());
          return false;
        }
      }

      @Override
      protected boolean writeToFile(String data) {
        return writeToFile(data.getBytes(StandardCharsets.UTF_8));
      }
    };
  }

  protected final Consumer<String> errorHandler;
  private final Set<Frame> frames = new HashSet<>();
  private boolean success;

  /**
   * Construct a new printer
   *
   * @param errorHandler a callback to receive errors
   */
  protected PrintNormalOutput(Consumer<String> errorHandler) {
    this.errorHandler = errorHandler;
  }

  @Override
  public final void accept(Any any) {
    any.accept(
        new WhinyAnyConsumer() {
          @Override
          public final void accept(long value) {
            success = writeToFile(Long.toString(value));
          }

          @Override
          public final void accept(boolean value) {
            success = writeToFile(value ? "True" : "False");
          }

          @Override
          public final void accept(double value) {
            success = writeToFile(Double.toString(value));
          }

          @Override
          public final void accept(Frame frame) {
            if (frames.contains(frame)) {
              errorHandler.accept("Frame's “value” attribute contains a cycle. Giving up.");
              return;
            }
            frames.add(frame);
            frame
                .get(Name.of("value"))
                .ifPresentOrElse(
                    value ->
                        value.accept(
                            new PromiseConsumer<Any>() {
                              @Override
                              public void accept(Any value) {
                                value.accept(this);
                              }

                              @Override
                              public void unfinished() {
                                errorHandler.accept("Frame's “value” attribute is not evaluated.");
                              }
                            }),
                    () -> errorHandler.accept("Frame has no “value” attribute. Giving up."));
          }

          @Override
          public void accept(byte[] value) {
            success = writeToFile(value);
          }

          @Override
          public final void accept(Str value) {
            success = writeToFile(value.toString());
          }

          @Override
          protected void fail(String type) {
            errorHandler.accept(String.format("Refusing to print result of type %s.", type));
          }
        });
  }

  /** Was the write operation a success? */
  public final boolean successful() {
    return success;
  }

  /** Write some value to the output stream. */
  protected abstract boolean writeToFile(byte[] data);

  /** Write some value to the output stream. */
  protected abstract boolean writeToFile(String data);
}
