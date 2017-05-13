package flabbergast.compiler.kws.text;

import flabbergast.compiler.Streamable;
import flabbergast.compiler.kws.KwsDispatch;
import java.util.Map;
import java.util.TreeMap;

public class TextDispatch implements KwsDispatch<TextBlock, Printable> {

  private final Map<String, Printable> dispatches = new TreeMap<>();

  @Override
  public void dispatchBin(TextBlock target, Streamable<Printable> captures) {
    dispatches.put("b", Printable.block(target, captures));
  }

  @Override
  public void dispatchBool(TextBlock target, Streamable<Printable> captures) {
    dispatches.put("z", Printable.block(target, captures));
  }

  @Override
  public void dispatchFloat(TextBlock target, Streamable<Printable> captures) {
    dispatches.put("f", Printable.block(target, captures));
  }

  @Override
  public void dispatchFrame(TextBlock target, Streamable<Printable> captures) {
    dispatches.put("r", Printable.block(target, captures));
  }

  @Override
  public void dispatchInt(TextBlock target, Streamable<Printable> captures) {
    dispatches.put("i", Printable.block(target, captures));
  }

  @Override
  public void dispatchLookupHandler(TextBlock target, Streamable<Printable> captures) {
    dispatches.put("l", Printable.block(target, captures));
  }

  @Override
  public void dispatchNull(TextBlock target, Streamable<Printable> captures) {
    dispatches.put("", Printable.block(target, captures));
  }

  @Override
  public void dispatchStr(TextBlock target, Streamable<Printable> captures) {
    dispatches.put("s", Printable.block(target, captures));
  }

  @Override
  public void dispatchTemplate(TextBlock target, Streamable<Printable> captures) {
    dispatches.put("t", Printable.block(target, captures));
  }

  public Printable[] toArray() {
    return dispatches.values().toArray(Printable[]::new);
  }
}
