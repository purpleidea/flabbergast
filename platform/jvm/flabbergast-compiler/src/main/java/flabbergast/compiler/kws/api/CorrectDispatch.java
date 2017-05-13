package flabbergast.compiler.kws.api;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.Streamable;
import flabbergast.compiler.kws.KwsDispatch;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CorrectDispatch implements KwsDispatch<CorrectBlock, CorrectFunction> {
  private final List<String> errors = new ArrayList<>();

  private boolean hasBin;
  private boolean hasBool;
  private boolean hasFloat;
  private boolean hasFrame;
  private boolean hasInt;
  private boolean hasLookupHandler;
  private boolean hasNull;
  private boolean hasStr;
  private boolean hasTemplate;
  private final Predicate<CorrectFunction> isOwner;

  public CorrectDispatch(CorrectFunction owner) {
    isOwner = owner::equals;
  }

  public void check(ErrorCollector collector, SourceLocation location) {
    errors.forEach(message -> collector.emitError(location, message));
    if (!hasBin
        && !hasBool
        && !hasFloat
        && !hasFrame
        && !hasInt
        && !hasNull
        && !hasLookupHandler
        && !hasStr
        && !hasTemplate) {
      collector.emitError(location, "br.a has no branches.");
    }
  }

  private void check(String type, CorrectBlock target, Streamable<CorrectFunction> captures) {
    captures
        .stream()
        .filter(isOwner.negate())
        .map(function -> String.format("Value from function “%s” used out of context.", function))
        .forEach(errors::add);
    if (!isOwner.test(target.owner())) {
      errors.add(String.format("Block for %s is not in the same function.", type));
    }
  }

  @Override
  public void dispatchBin(CorrectBlock target, Streamable<CorrectFunction> captures) {
    if (hasBin) {
      errors.add(
          String.format("Attempted to add %s, but block for Bin already present.", target.name()));
    }
    hasBin = true;
    check("Bin", target, captures);
  }

  @Override
  public void dispatchBool(CorrectBlock target, Streamable<CorrectFunction> captures) {
    if (hasBool) {
      errors.add(
          String.format("Attempted to add %s, but block for Bool already present.", target.name()));
    }
    hasBool = true;
    check("Bool", target, captures);
  }

  @Override
  public void dispatchFloat(CorrectBlock target, Streamable<CorrectFunction> captures) {
    if (hasFloat) {
      errors.add(
          String.format(
              "Attempted to add %s, but block for Float already present.", target.name()));
    }
    hasFloat = true;
    check("Float", target, captures);
  }

  @Override
  public void dispatchFrame(CorrectBlock target, Streamable<CorrectFunction> captures) {
    if (hasFrame) {
      errors.add(
          String.format(
              "Attempted to add %s, but block for Frame already present.", target.name()));
    }
    hasFrame = true;
    check("Frame", target, captures);
  }

  @Override
  public void dispatchInt(CorrectBlock target, Streamable<CorrectFunction> captures) {
    if (hasInt) {
      errors.add(
          String.format("Attempted to add %s, but block for Int already present.", target.name()));
    }
    hasInt = true;
    check("Int", target, captures);
  }

  @Override
  public void dispatchLookupHandler(CorrectBlock target, Streamable<CorrectFunction> captures) {
    if (hasLookupHandler) {
      errors.add(
          String.format(
              "Attempted to add %s, but block for LookupHandler already present.", target.name()));
    }
    hasLookupHandler = true;
    check("LookupHandler", target, captures);
  }

  @Override
  public void dispatchNull(CorrectBlock target, Streamable<CorrectFunction> captures) {
    if (hasNull) {
      errors.add(
          String.format("Attempted to add %s, but block for Null already present.", target.name()));
    }
    hasNull = true;
    check("Null", target, captures);
  }

  @Override
  public void dispatchStr(CorrectBlock target, Streamable<CorrectFunction> captures) {
    if (hasStr) {
      errors.add(
          String.format("Attempted to add %s, but block for Str already present.", target.name()));
    }
    hasStr = true;
    check("Str", target, captures);
  }

  @Override
  public void dispatchTemplate(CorrectBlock target, Streamable<CorrectFunction> captures) {
    if (hasTemplate) {
      errors.add(
          String.format(
              "Attempted to add %s, but block for Template already present.", target.name()));
    }
    hasTemplate = true;
    check("Template", target, captures);
  }

  public Stream<String> errors() {
    return errors.stream();
  }
}
