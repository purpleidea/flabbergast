package flabbergast.compiler.kws.codegen;

import flabbergast.compiler.FlabbergastType;
import flabbergast.compiler.Streamable;
import flabbergast.compiler.kws.KwsDispatch;
import flabbergast.compiler.kws.KwsType;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

class JvmDispatch implements KwsDispatch<JvmBlock, Value> {
  private final Map<FlabbergastType, MethodBranchInstruction.BranchPath> branches = new HashMap<>();

  public JvmDispatch(JvmBlock block) {}

  @Override
  public void dispatchBin(JvmBlock target, Streamable<Value> captures) {
    branches.put(
        FlabbergastType.BIN, new MethodBranchInstruction.BranchPath(target, captures, KwsType.B));
  }

  @Override
  public void dispatchBool(JvmBlock target, Streamable<Value> captures) {
    branches.put(
        FlabbergastType.BOOL, new MethodBranchInstruction.BranchPath(target, captures, KwsType.Z));
  }

  @Override
  public void dispatchFloat(JvmBlock target, Streamable<Value> captures) {
    branches.put(
        FlabbergastType.FLOAT, new MethodBranchInstruction.BranchPath(target, captures, KwsType.F));
  }

  @Override
  public void dispatchFrame(JvmBlock target, Streamable<Value> captures) {
    branches.put(
        FlabbergastType.FRAME, new MethodBranchInstruction.BranchPath(target, captures, KwsType.R));
  }

  @Override
  public void dispatchInt(JvmBlock target, Streamable<Value> captures) {
    branches.put(
        FlabbergastType.INT, new MethodBranchInstruction.BranchPath(target, captures, KwsType.I));
  }

  @Override
  public void dispatchLookupHandler(JvmBlock target, Streamable<Value> captures) {
    branches.put(
        FlabbergastType.LOOKUP_HANDLER,
        new MethodBranchInstruction.BranchPath(target, captures, KwsType.L));
  }

  @Override
  public void dispatchNull(JvmBlock target, Streamable<Value> captures) {
    branches.put(FlabbergastType.NULL, new MethodBranchInstruction.BranchPath(target, captures));
  }

  @Override
  public void dispatchStr(JvmBlock target, Streamable<Value> captures) {
    branches.put(
        FlabbergastType.STR, new MethodBranchInstruction.BranchPath(target, captures, KwsType.S));
  }

  @Override
  public void dispatchTemplate(JvmBlock target, Streamable<Value> captures) {
    branches.put(
        FlabbergastType.TEMPLATE,
        new MethodBranchInstruction.BranchPath(target, captures, KwsType.T));
  }

  public Stream<FlabbergastType> names() {
    return branches.keySet().stream();
  }

  public Stream<MethodBranchInstruction.BranchPath> paths() {
    return branches.values().stream();
  }
}
