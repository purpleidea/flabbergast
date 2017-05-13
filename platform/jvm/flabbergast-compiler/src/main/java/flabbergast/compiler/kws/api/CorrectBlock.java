package flabbergast.compiler.kws.api;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.FlabbergastType;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.Streamable;
import flabbergast.compiler.kws.KwsBlock;
import flabbergast.compiler.kws.KwsType;
import flabbergast.compiler.kws.ResultType;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CorrectBlock implements KwsBlock<CorrectFunction, CorrectBlock, CorrectDispatch> {
  private final ErrorCollector collector;
  private boolean dead;
  private final String id;
  private SourceLocation location = SourceLocation.EMPTY;
  private final CorrectFunction owner;
  private final int parameters;

  public CorrectBlock(CorrectFunction owner, String id, int parameters, ErrorCollector collector) {
    super();
    this.owner = owner;
    this.id = id;
    this.parameters = parameters;
    this.collector = collector;
  }

  @Override
  public CorrectFunction add_f(CorrectFunction left, CorrectFunction right) {
    checkAlive("add.f");
    check("add.f", left, right);
    return owner;
  }

  @Override
  public CorrectFunction add_i(CorrectFunction left, CorrectFunction right) {
    checkAlive("add.i");
    check("add.i", left, right);
    return owner;
  }

  @Override
  public CorrectFunction add_n(CorrectFunction source, Streamable<String> names) {
    checkAlive("add.n");
    check("add.n", source);
    return owner;
  }

  @Override
  public CorrectFunction add_n_a(CorrectFunction source, CorrectFunction value) {
    checkAlive("add.n.a");
    check("add.n.a", source, value);
    return owner;
  }

  @Override
  public CorrectFunction add_n_i(CorrectFunction source, CorrectFunction ordinal) {
    checkAlive("add.n.i");
    check("add.n.i", source, ordinal);
    return owner;
  }

  @Override
  public CorrectFunction add_n_r(CorrectFunction source, CorrectFunction frame) {
    checkAlive("add.n.r");
    return owner;
  }

  @Override
  public CorrectFunction add_n_s(CorrectFunction source, CorrectFunction name) {
    checkAlive("add.n.s");
    check("add.n.s", source, name);
    return owner;
  }

  @Override
  public CorrectFunction adjacent_f(CorrectFunction name, CorrectFunction definition) {
    checkAlive("adjacent.f");
    check("adjacent.f", name, definition);
    return owner;
  }

  @Override
  public CorrectFunction adjacent_i(CorrectFunction name, CorrectFunction definition) {
    checkAlive("adjacent.i");
    check("adjacent.i", name, definition);
    return owner;
  }

  @Override
  public CorrectFunction adjacent_s(CorrectFunction name, CorrectFunction definition) {
    checkAlive("adjacent.s");
    check("adjacent.s", name, definition);
    return owner;
  }

  @Override
  public CorrectFunction adjacent_z(CorrectFunction name, CorrectFunction definition) {
    checkAlive("adjacent.z");
    check("adjacent.z", name, definition);
    return owner;
  }

  @Override
  public CorrectFunction alwaysinclude_f(CorrectFunction name, CorrectFunction key) {
    checkAlive("alwaysinclude.f");
    check("alwaysinclude.f", name, key);
    return owner;
  }

  @Override
  public CorrectFunction alwaysinclude_i(CorrectFunction name, CorrectFunction key) {
    checkAlive("alwaysinclude.i");
    check("alwaysinclude.i", name, key);
    return owner;
  }

  @Override
  public CorrectFunction alwaysinclude_s(CorrectFunction name, CorrectFunction key) {
    checkAlive("alwaysinclude.s");
    check("alwaysinclude.s", name, key);
    return owner;
  }

  @Override
  public CorrectFunction alwaysinclude_z(CorrectFunction name, CorrectFunction key) {
    checkAlive("alwaysinclude.z");
    check("alwaysinclude.z", name, key);
    return owner;
  }

  @Override
  public CorrectFunction and_g(Streamable<CorrectFunction> groupers) {
    checkAlive("and.eg");
    check("and.eg", groupers.stream());
    return owner;
  }

  @Override
  public CorrectFunction and_i(CorrectFunction left, CorrectFunction right) {
    checkAlive("and.i");
    check("and.i", left, right);
    return owner;
  }

  @Override
  public CorrectFunction atos(CorrectFunction value) {
    checkAlive("atos");
    check("atos", value);
    return owner;
  }

  @Override
  public CorrectFunction atoz(CorrectFunction value, Predicate<FlabbergastType> include) {
    checkAlive("atoz");
    check("atoz", value);
    return owner;
  }

  @Override
  public CorrectFunction boundary(CorrectFunction definition, CorrectFunction trailing) {
    checkAlive("boundary");
    check("boundary", definition, trailing);
    return owner;
  }

  @Override
  public void br(CorrectBlock target, Streamable<CorrectFunction> arguments) {
    checkAlive("br");
    check("br", target);
    check("br", arguments.stream());
    dead = true;
  }

  @Override
  public void br_a(CorrectFunction value, CorrectDispatch dispatch, Optional<String> error) {
    checkAlive("br.a");
    dispatch.errors().forEach(message -> collector.emitError(location, message));
    dead = true;
    dispatch.check(collector, location);
  }

  @Override
  public void br_aa(
      CorrectFunction left,
      CorrectFunction right,
      CorrectBlock intTarget,
      Streamable<CorrectFunction> intArguments,
      CorrectBlock floatTarget,
      Streamable<CorrectFunction> floatArguments) {
    checkAlive("br.aa");
    check("br.aa", left, right);
    check("br.aa", intTarget);
    check("br.aa", intArguments.stream());
    check("br.aa", floatTarget);
    check("br.aa", floatArguments.stream());
    dead = true;
  }

  @Override
  public void br_fa(
      CorrectFunction left,
      CorrectFunction right,
      CorrectBlock target,
      Streamable<CorrectFunction> arguments) {
    checkAlive("br.fa");
    check("br.fa", left, right);
    check("br.fa", target);
    check("br.fa", arguments.stream());
    dead = true;
  }

  @Override
  public void br_ia(
      CorrectFunction left,
      CorrectFunction right,
      CorrectBlock intTarget,
      Streamable<CorrectFunction> intArguments,
      CorrectBlock floatTarget,
      Streamable<CorrectFunction> floatArguments) {
    checkAlive("br.ia");
    check("br.ia", left, right);
    check("br.ia", intTarget);
    check("br.ia", intArguments.stream());
    check("br.ia", floatTarget);
    check("br.ia", floatArguments.stream());
    dead = true;
  }

  @Override
  public void br_z(
      CorrectFunction condition,
      CorrectBlock trueTarget,
      Streamable<CorrectFunction> trueArguments,
      CorrectBlock falseTarget,
      Streamable<CorrectFunction> falseArguments) {
    checkAlive("br.z");
    check("br.z", condition);
    check("br.z", trueTarget);
    check("br.z", trueArguments.stream());
    check("br.z", falseTarget);
    check("br.z", falseArguments.stream());
    dead = true;
  }

  @Override
  public CorrectFunction btoa(CorrectFunction value) {
    checkAlive("btoa");
    check("btoa", value);
    return owner;
  }

  @Override
  public CorrectFunction buckets_f(CorrectFunction definition, CorrectFunction count) {
    checkAlive("buckets.f");
    check("buckets.f", definition, count);
    return owner;
  }

  @Override
  public CorrectFunction buckets_i(CorrectFunction definition, CorrectFunction count) {
    checkAlive("buckets.i");
    check("buckets.i", definition, count);
    return owner;
  }

  @Override
  public CorrectFunction buckets_s(CorrectFunction definition, CorrectFunction count) {
    checkAlive("buckets.s");
    check("buckets.s", definition, count);
    return owner;
  }

  @Override
  public CorrectFunction call_d(CorrectFunction definition, CorrectFunction context) {
    checkAlive("call.d");
    check("call.d", definition, context);
    return owner;
  }

  @Override
  public CorrectFunction call_o(
      CorrectFunction override, CorrectFunction context, CorrectFunction original) {
    checkAlive("call.o");
    check("call.o", override, context, original);
    return owner;
  }

  @Override
  public CorrectFunction cat_e(CorrectFunction context, Streamable<CorrectFunction> chains) {
    checkAlive("cat.e");
    check("cat.e", context);
    check("cat.e", chains.stream());
    return owner;
  }

  @Override
  public CorrectFunction cat_ke(CorrectFunction definition, CorrectFunction chain) {
    checkAlive("cat.ke");
    check("cat.ke", definition);
    check("cat.ke", chain);
    return owner;
  }

  @Override
  public CorrectFunction cat_r(
      CorrectFunction context, CorrectFunction first, CorrectFunction second) {
    checkAlive("cat.r");
    check("cat.r", first, second);
    return owner;
  }

  @Override
  public CorrectFunction cat_rc(CorrectFunction head, CorrectFunction tail) {
    checkAlive("cat.rc");
    check("cat.rc", head, tail);
    return owner;
  }

  @Override
  public CorrectFunction cat_s(CorrectFunction first, CorrectFunction second) {
    checkAlive("cat.s");
    check("cat.s", first, second);
    return owner;
  }

  private void check(String operation, CorrectBlock block) {
    if (block.owner != owner) {
      collector.emitError(
          location, String.format("Target block for “%s” not from this function.", operation));
    }
  }

  private void check(String operation, CorrectFunction... parameters) {
    check(operation, Stream.of(parameters));
  }

  void check(String operation, Stream<CorrectFunction> parameters) {
    if (parameters.filter(Objects::nonNull).anyMatch(parameter -> parameter != owner)) {
      collector.emitError(
          location,
          String.format("“%s” was called with parameter from another function.", operation));
    }
  }

  void checkAlive(String operation) {
    if (dead) {
      collector.emitError(
          location, String.format("“%s” was called after terminal instruction.", operation));
    }
  }

  private void checkReturn(boolean accumulator) {
    if (accumulator != (owner.result() == ResultType.ACCUMULATOR)) {
      collector.emitError(location, "Incorrect return used.");
    }
  }

  @Override
  public CorrectFunction chunk_e(CorrectFunction width) {
    checkAlive("chunk.e");
    check("chunk.e", width);
    return owner;
  }

  @Override
  public CorrectFunction cmp_f(CorrectFunction left, CorrectFunction right) {
    checkAlive("cmp.f");
    check("cmp.f", left, right);
    return owner;
  }

  @Override
  public CorrectFunction cmp_i(CorrectFunction left, CorrectFunction right) {
    checkAlive("cmp.i");
    check("cmp.i", left, right);
    return owner;
  }

  @Override
  public CorrectFunction cmp_s(CorrectFunction left, CorrectFunction right) {
    checkAlive("cmp.s");
    check("cmp.s", left, right);
    return owner;
  }

  @Override
  public CorrectFunction cmp_z(CorrectFunction left, CorrectFunction right) {
    checkAlive("cmp.z");
    check("cmp.z", left, right);
    return owner;
  }

  @Override
  public CorrectFunction contextual() {
    checkAlive("contextual");
    return owner;
  }

  @Override
  public CorrectFunction count_w(CorrectFunction count) {
    checkAlive("count.w");
    check("count.w", count);
    return owner;
  }

  @Override
  public CorrectDispatch createDispatch() {
    return new CorrectDispatch(owner);
  }

  @Override
  public CorrectFunction crosstab_f(CorrectFunction key) {
    checkAlive("crosstab.f");
    check("crosstab.f", key);
    return owner;
  }

  @Override
  public CorrectFunction crosstab_i(CorrectFunction key) {
    checkAlive("crosstab.i");
    check("crosstab.i", key);
    return owner;
  }

  @Override
  public CorrectFunction crosstab_s(CorrectFunction key) {
    checkAlive("crosstab.s");
    check("crosstab.s", key);
    return owner;
  }

  @Override
  public CorrectFunction crosstab_z(CorrectFunction key) {
    checkAlive("crosstab.z");
    check("crosstab.z", key);
    return owner;
  }

  @Override
  public CorrectFunction ctr_c(CorrectFunction value) {
    checkAlive("ctr.c");
    check("ctr.c", value);
    return owner;
  }

  @Override
  public CorrectFunction ctr_r(CorrectFunction frame) {
    checkAlive("ctr.r");
    check("ctr.r", frame);
    return owner;
  }

  @Override
  public CorrectFunction ctxt_r(CorrectFunction context, CorrectFunction frame) {
    checkAlive("ctxt.r");
    check("ctxt.r", context);
    check("ctxt.r", frame);
    return owner;
  }

  @Override
  public CorrectFunction debug_d(CorrectFunction definition, CorrectFunction context) {
    checkAlive("debug.d");
    check("debug.d", definition, context);
    return owner;
  }

  @Override
  public CorrectFunction disc_g_f(CorrectFunction name, CorrectFunction getter) {
    checkAlive("disc.eg.f");
    check("disc.eg.f", name, getter);
    return owner;
  }

  @Override
  public CorrectFunction disc_g_i(CorrectFunction name, CorrectFunction getter) {
    checkAlive("disc.eg.i");
    check("disc.eg.i", name, getter);
    return owner;
  }

  @Override
  public CorrectFunction disc_g_s(CorrectFunction name, CorrectFunction getter) {
    checkAlive("disc.eg.s");
    check("disc.eg.s", name, getter);
    return owner;
  }

  @Override
  public CorrectFunction disc_g_z(CorrectFunction name, CorrectFunction getter) {
    checkAlive("disc.eg.z");
    check("disc.eg.z", name, getter);
    return owner;
  }

  @Override
  public void disperse_i(CorrectFunction frame, CorrectFunction name, CorrectFunction value) {
    checkAlive("disperse.i");
    check("disperse.i", frame, name, value);
  }

  @Override
  public void disperse_s(CorrectFunction frame, CorrectFunction name, CorrectFunction value) {
    checkAlive("disperse.s");
    check("disperse.s", frame, name, value);
  }

  @Override
  public CorrectFunction div_f(CorrectFunction left, CorrectFunction right) {
    checkAlive("div.f");
    check("div.f", left, right);
    return owner;
  }

  @Override
  public CorrectFunction div_i(CorrectFunction left, CorrectFunction right) {
    checkAlive("div.i");
    check("div.i", left, right);
    return owner;
  }

  @Override
  public CorrectFunction drop_ed(CorrectFunction source, CorrectFunction clause) {
    checkAlive("drop.ed");
    check("drop.ed", source, clause);
    return owner;
  }

  @Override
  public CorrectFunction drop_ei(CorrectFunction source, CorrectFunction count) {
    checkAlive("drop.ei");
    check("drop.ei", source, count);
    return owner;
  }

  @Override
  public CorrectFunction drop_x(CorrectFunction name) {
    checkAlive("drop.x");
    check("drop.x", name);
    return owner;
  }

  @Override
  public CorrectFunction dropl_ei(CorrectFunction source, CorrectFunction count) {
    checkAlive("dropl.ei");
    check("dropl.ei", source, count);
    return owner;
  }

  @Override
  public CorrectFunction duration_f(CorrectFunction definition, CorrectFunction duration) {
    checkAlive("duration.f");
    check("duration.f", definition, duration);
    return owner;
  }

  @Override
  public CorrectFunction duration_i(CorrectFunction definition, CorrectFunction duration) {
    checkAlive("duration.i");
    check("duration.i", definition, duration);
    return owner;
  }

  @Override
  public void error(CorrectFunction message) {
    checkAlive("error");
    check("error", message);
    dead = true;
  }

  @Override
  public CorrectFunction etoa_ao(
      CorrectFunction source, CorrectFunction initial, CorrectFunction reducer) {
    checkAlive("etoa.ao");
    check("etoa.ao", source, initial, reducer);
    return owner;
  }

  @Override
  public CorrectFunction etoa_d(CorrectFunction source, CorrectFunction extractor) {
    checkAlive("etoa.d");
    check("etoa.d", source, extractor);
    return owner;
  }

  @Override
  public CorrectFunction etoa_dd(
      CorrectFunction source, CorrectFunction extractor, CorrectFunction alternate) {
    checkAlive("etoa.dd");
    check("etoa.dd", source, extractor, alternate);
    return owner;
  }

  @Override
  public CorrectFunction etod(CorrectFunction source, CorrectFunction computeValue) {
    checkAlive("etod");
    check("etod", source, computeValue);
    return owner;
  }

  @Override
  public CorrectFunction etod_a(
      CorrectFunction source, CorrectFunction computeValue, CorrectFunction empty) {
    checkAlive("etod.a");
    check("etod.a", source, computeValue, empty);
    return owner;
  }

  @Override
  public CorrectFunction etoe_g(CorrectFunction source, Streamable<CorrectFunction> groupers) {
    checkAlive("etoe.eg");
    check("etoe.eg", source);
    check("etoe.eg", groupers.stream());
    return owner;
  }

  @Override
  public CorrectFunction etoe_m(
      CorrectFunction source, CorrectFunction initial, CorrectFunction reducer) {
    checkAlive("etoe.m");
    return owner;
  }

  @Override
  public CorrectFunction etoe_u(CorrectFunction source, CorrectFunction flattener) {
    checkAlive("etoe.u");
    check("etoe.u", source);
    check("etoe.u", flattener);
    return owner;
  }

  @Override
  public CorrectFunction etoi(CorrectFunction source) {
    checkAlive("etoi");
    check("etoi", source);
    return owner;
  }

  @Override
  public CorrectFunction etor_ao(
      CorrectFunction source, CorrectFunction initial, CorrectFunction reducer) {
    checkAlive("etor.ao");
    check("etor.ao", source, initial, reducer);
    return owner;
  }

  @Override
  public CorrectFunction etor_i(CorrectFunction source, CorrectFunction computeValue) {
    checkAlive("etor.i");
    check("etor.i", source, computeValue);
    return owner;
  }

  @Override
  public CorrectFunction etor_s(
      CorrectFunction source, CorrectFunction computeName, CorrectFunction computeValue) {
    checkAlive("etor.s");
    check("etor.s", source, computeName, computeValue);
    return owner;
  }

  @Override
  public CorrectFunction ext(String uri) {
    checkAlive("ext");
    return owner;
  }

  @Override
  public CorrectFunction f(double value) {
    checkAlive("f");
    return owner;
  }

  @Override
  public CorrectFunction filt_e(CorrectFunction source, CorrectFunction clause) {
    checkAlive("filt.e");
    check("filt.e", source, clause);
    return owner;
  }

  @Override
  public CorrectFunction ftoa(CorrectFunction value) {
    checkAlive("ftoa");
    check("ftoa", value);
    return owner;
  }

  @Override
  public CorrectFunction ftoi(CorrectFunction value) {
    checkAlive("ftoi");
    check("ftoi", value);
    return owner;
  }

  @Override
  public CorrectFunction ftos(CorrectFunction value) {
    checkAlive("ftos");
    check("ftos", value);
    return owner;
  }

  @Override
  public CorrectFunction gather_i(CorrectFunction frame, CorrectFunction name) {
    checkAlive("gather.i");
    check("gather.i", frame, name);
    return owner;
  }

  @Override
  public CorrectFunction gather_s(CorrectFunction frame, CorrectFunction name) {
    checkAlive("gather.s");
    check("gather.s", frame, name);
    return owner;
  }

  @Override
  public CorrectFunction i(long value) {
    checkAlive("i");
    return owner;
  }

  @Override
  public CorrectFunction id(CorrectFunction frame) {
    checkAlive("id");
    check("id", frame);
    return owner;
  }

  @Override
  public CorrectFunction importFunction(
      String kwsName, KwsType returnType, Streamable<CorrectFunction> arguments) {
    checkAlive("import");
    check("import", arguments.stream());
    return owner;
  }

  @Override
  public CorrectFunction inf_f() {
    checkAlive("int.f");
    return owner;
  }

  public boolean isAlive() {
    return !dead;
  }

  @Override
  public CorrectFunction is_finite(CorrectFunction value) {
    checkAlive("is_finite");
    check("is_finite", value);
    return owner;
  }

  @Override
  public CorrectFunction is_nan(CorrectFunction value) {
    checkAlive("is.nan");
    check("is.nan", value);
    return owner;
  }

  @Override
  public CorrectFunction itoa(CorrectFunction value) {
    checkAlive("itoa");
    check("itoa", value);
    return owner;
  }

  @Override
  public CorrectFunction itof(CorrectFunction value) {
    checkAlive("itof");
    check("itof", value);
    return owner;
  }

  @Override
  public CorrectFunction itos(CorrectFunction value) {
    checkAlive("itos");
    check("itos", value);
    return owner;
  }

  @Override
  public CorrectFunction itoz(long reference, CorrectFunction value) {
    checkAlive("itoz");
    check("itoz", value);
    return owner;
  }

  @Override
  public CorrectFunction ktol(
      CorrectFunction name, CorrectFunction context, CorrectFunction definition) {
    checkAlive("ktol");
    check("ktol", name, context, definition);
    return owner;
  }

  @Override
  public CorrectFunction len_b(CorrectFunction blob) {
    checkAlive("len.b");
    check("len.b", blob);
    return owner;
  }

  @Override
  public CorrectFunction len_s(CorrectFunction str) {
    checkAlive("len.s");
    check("len.s", str);
    return owner;
  }

  @Override
  public CorrectFunction let_e(CorrectFunction source, Streamable<CorrectFunction> builder) {
    checkAlive("let.e");
    check("let.e", source);
    check("let.e", builder.stream());
    return owner;
  }

  @Override
  public CorrectFunction lookup(CorrectFunction context, Streamable<String> names) {
    checkAlive("lookup");
    check("lookup", context);
    return owner;
  }

  @Override
  public CorrectFunction lookup_l(
      CorrectFunction handler, CorrectFunction context, CorrectFunction names) {
    checkAlive("lookup.l");
    check("lookup.l", handler, context, names);
    return owner;
  }

  @Override
  public CorrectFunction ltoa(CorrectFunction value) {
    checkAlive("ltoa");
    check("ltoa", value);
    return owner;
  }

  @Override
  public CorrectFunction max_f() {
    checkAlive("max.f");
    return owner;
  }

  @Override
  public CorrectFunction max_i() {
    checkAlive("max.i");
    return owner;
  }

  @Override
  public CorrectFunction max_z() {
    checkAlive("max.z");
    return owner;
  }

  @Override
  public CorrectFunction min_f() {
    checkAlive("min.f");
    return owner;
  }

  @Override
  public CorrectFunction min_i() {
    checkAlive("min.i");
    return owner;
  }

  @Override
  public CorrectFunction min_z() {
    checkAlive("min.z");
    return owner;
  }

  @Override
  public CorrectFunction mod_f(CorrectFunction left, CorrectFunction right) {
    checkAlive("mod.f");
    check("mod.f", left, right);
    return owner;
  }

  @Override
  public CorrectFunction mod_i(CorrectFunction left, CorrectFunction right) {
    checkAlive("mod.i");
    check("mod.i", left, right);
    return owner;
  }

  @Override
  public CorrectFunction mtoe(
      CorrectFunction context, CorrectFunction initial, CorrectFunction definition) {
    checkAlive("mtoe");
    check("mtoe", context, initial, definition);
    return owner;
  }

  @Override
  public CorrectFunction mul_f(CorrectFunction left, CorrectFunction right) {
    checkAlive("mul.f");
    check("mul.f", left, right);
    return owner;
  }

  @Override
  public CorrectFunction mul_i(CorrectFunction left, CorrectFunction right) {
    checkAlive("mul.i");
    check("mul.i", left, right);
    return owner;
  }

  public String name() {
    return id;
  }

  @Override
  public CorrectFunction nan_f() {
    checkAlive("nan.f");
    return owner;
  }

  @Override
  public CorrectFunction neg_f(CorrectFunction value) {
    checkAlive("neg.f");
    check("neg.f", value);
    return owner;
  }

  @Override
  public CorrectFunction neg_i(CorrectFunction value) {
    checkAlive("neg.i");
    check("neg.i", value);
    return owner;
  }

  @Override
  public CorrectFunction new_g(CorrectFunction name, CorrectFunction collector) {
    checkAlive("new.eg");
    check("new.eg", name, collector);
    return owner;
  }

  @Override
  public CorrectFunction new_g_a(CorrectFunction name, CorrectFunction value) {
    checkAlive("new.eg.a");
    check("new.eg.a", name, value);
    return owner;
  }

  @Override
  public CorrectFunction new_p(
      CorrectFunction source, CorrectFunction intersect, Streamable<CorrectFunction> zippers) {
    checkAlive("new.ez");
    check("new.ez", source);
    check("new.ez", intersect);
    check("new.ez", zippers.stream());
    return owner;
  }

  @Override
  public CorrectFunction new_p_i(CorrectFunction name) {
    checkAlive("new.ez.i");
    check("new.ez.i", name);
    return owner;
  }

  @Override
  public CorrectFunction new_p_r(CorrectFunction name, CorrectFunction frame) {
    checkAlive("new.ez.r");
    check("new.ez.r", name, frame);
    return owner;
  }

  @Override
  public CorrectFunction new_p_s(CorrectFunction name) {
    checkAlive("new.ez.s");
    check("new.ez.s", name);
    return owner;
  }

  @Override
  public CorrectFunction new_r(
      CorrectFunction selfIsThis,
      CorrectFunction context,
      Streamable<CorrectFunction> gatherers,
      Streamable<CorrectFunction> builder) {
    checkAlive("new.r");
    check("new.r", selfIsThis, context);
    check("new.r", gatherers.stream());
    check("new.r", builder.stream());
    return owner;
  }

  @Override
  public CorrectFunction new_r_i(
      CorrectFunction context, CorrectFunction start, CorrectFunction end) {
    checkAlive("new.r.i");
    check("new.r.i", context, start, end);
    return owner;
  }

  @Override
  public CorrectFunction new_t(
      CorrectFunction context,
      Streamable<CorrectFunction> gatherers,
      Streamable<CorrectFunction> builder) {
    checkAlive("new.t");
    check("new.t", context);
    check("new.t", gatherers.stream());
    check("new.t", builder.stream());
    return owner;
  }

  @Override
  public CorrectFunction new_x_ia(CorrectFunction ordinal, CorrectFunction value) {
    checkAlive("new.x.i");
    check("new.x.i", ordinal, value);
    return owner;
  }

  @Override
  public CorrectFunction new_x_sa(CorrectFunction name, CorrectFunction value) {
    checkAlive("new.x.s");
    check("new.x.s", name, value);
    return owner;
  }

  @Override
  public CorrectFunction new_x_sd(CorrectFunction name, CorrectFunction definition) {
    checkAlive("new.x.d");
    check("new.x.d", name, definition);
    return owner;
  }

  @Override
  public CorrectFunction new_x_so(CorrectFunction name, CorrectFunction override) {
    checkAlive("new.x.o");
    check("new.x.o", name, override);
    return owner;
  }

  @Override
  public CorrectFunction nil_a() {
    checkAlive("nil.a");
    return owner;
  }

  @Override
  public CorrectFunction nil_c() {
    checkAlive("nil.c");
    return owner;
  }

  @Override
  public CorrectFunction nil_n() {
    checkAlive("nil.n");
    return owner;
  }

  @Override
  public CorrectFunction nil_r() {
    checkAlive("nil.r");
    return owner;
  }

  @Override
  public CorrectFunction nil_w() {
    checkAlive("nil.w");
    return owner;
  }

  @Override
  public CorrectFunction not_g(CorrectFunction value) {
    checkAlive("not.eg");
    check("not.eg", value);
    return owner;
  }

  @Override
  public CorrectFunction not_i(CorrectFunction value) {
    checkAlive("not.i");
    check("not.i", value);
    return owner;
  }

  @Override
  public CorrectFunction not_z(CorrectFunction value) {
    checkAlive("not.z");
    check("not.z", value);
    return owner;
  }

  @Override
  public CorrectFunction or_g(Streamable<CorrectFunction> groupers) {
    checkAlive("or.eg");
    check("or.eg", groupers.stream());
    return owner;
  }

  @Override
  public CorrectFunction or_i(CorrectFunction left, CorrectFunction right) {
    checkAlive("or.i");
    check("or.i", left, right);
    return owner;
  }

  @Override
  public CorrectFunction ord_e_f(
      CorrectFunction source, CorrectFunction ascending, CorrectFunction clause) {
    checkAlive("ord.e.f");
    check("ord.e.f", source, ascending, clause);
    return owner;
  }

  @Override
  public CorrectFunction ord_e_i(
      CorrectFunction source, CorrectFunction ascending, CorrectFunction clause) {
    checkAlive("ord.e.i");
    check("ord.e.i", source, ascending, clause);
    return owner;
  }

  @Override
  public CorrectFunction ord_e_s(
      CorrectFunction source, CorrectFunction ascending, CorrectFunction clause) {
    checkAlive("ord.e.s");
    check("ord.e.s", source, ascending, clause);
    return owner;
  }

  @Override
  public CorrectFunction ord_e_z(
      CorrectFunction source, CorrectFunction ascending, CorrectFunction clause) {
    checkAlive("ord.e.z");
    check("ord.e.z", source, ascending, clause);
    return owner;
  }

  public CorrectFunction owner() {
    return owner;
  }

  @Override
  public CorrectFunction parameter(int i) {
    if (i < 0 || i >= parameters) {
      collector.emitError(location, String.format("Parameter %d out of bounds for block", i));
    }
    return owner;
  }

  @Override
  public int parameters() {
    return parameters;
  }

  @Override
  public CorrectFunction powerset(Streamable<CorrectFunction> groupers) {
    checkAlive("powerset");
    check("powerset", groupers.stream());
    return owner;
  }

  @Override
  public CorrectFunction priv_cr(CorrectFunction context, CorrectFunction frame) {
    checkAlive("priv.cr");
    check("priv.cr", context);
    check("priv.cr", frame);
    return owner;
  }

  @Override
  public CorrectFunction priv_x(CorrectFunction inner) {
    checkAlive("priv.x");
    check("priv.x", inner);
    return owner;
  }

  @Override
  public CorrectFunction require_x(CorrectFunction name) {
    checkAlive("require.x");
    check("require.x", name);
    return owner;
  }

  @Override
  public void ret(CorrectFunction value) {
    checkReturn(false);
    checkAlive("ret");
    check("ret", value);
    dead = true;
  }

  @Override
  public void ret(CorrectFunction value, Streamable<CorrectFunction> builders) {
    checkReturn(true);
    checkAlive("ret");
    check("ret", value);
    check("ret", builders.stream());
    dead = true;
  }

  @Override
  public CorrectFunction rev_e(CorrectFunction source) {
    checkAlive("rev.e");
    check("rev.e", source);
    return owner;
  }

  @Override
  public CorrectFunction ring_g(CorrectFunction primitive, CorrectFunction size) {
    checkAlive("ring.g");
    check("ring.g", primitive, size);
    return owner;
  }

  @Override
  public CorrectFunction rtoa(CorrectFunction value) {
    checkAlive("rtoa");
    check("rota", value);
    return owner;
  }

  @Override
  public CorrectFunction rtoe(CorrectFunction source, CorrectFunction context) {
    checkAlive("rtoe");
    check("rote", source, context);
    return owner;
  }

  @Override
  public CorrectFunction s(String value) {
    return owner;
  }

  @Override
  public CorrectFunction seal_d(CorrectFunction definition, CorrectFunction context) {
    checkAlive("seal.d");
    check("seal.d", definition, context);
    return owner;
  }

  @Override
  public CorrectFunction seal_o(CorrectFunction definition, CorrectFunction context) {
    checkAlive("seal.o");
    check("seal.o", definition, context);
    return owner;
  }

  @Override
  public CorrectFunction session_f(
      CorrectFunction definition, CorrectFunction adjacent, CorrectFunction maximum) {
    checkAlive("session.f");
    check("session.f", definition, adjacent, maximum);
    return owner;
  }

  @Override
  public CorrectFunction session_i(
      CorrectFunction definition, CorrectFunction adjacent, CorrectFunction maximum) {
    checkAlive("session.i");
    check("session.i", definition, adjacent, maximum);
    return owner;
  }

  @Override
  public CorrectFunction sh_i(CorrectFunction value, CorrectFunction offset) {
    checkAlive("sh.i");
    check("sh.i", value, offset);
    return owner;
  }

  @Override
  public CorrectFunction shuf_e(CorrectFunction source) {
    checkAlive("shuf.e");
    check("shuf.e", source);
    return owner;
  }

  @Override
  public CorrectFunction stoa(CorrectFunction value) {
    checkAlive("stoa");
    check("stoa", value);
    return owner;
  }

  @Override
  public CorrectFunction stripe_e(CorrectFunction width) {
    checkAlive("stripe.e");
    check("stripe.e", width);
    return owner;
  }

  @Override
  public CorrectFunction sub_f(CorrectFunction left, CorrectFunction right) {
    checkAlive("sub.f");
    check("sub.f", left, right);
    return owner;
  }

  @Override
  public CorrectFunction sub_i(CorrectFunction left, CorrectFunction right) {
    checkAlive("sub.i");
    check("sub.i", left, right);
    return owner;
  }

  @Override
  public CorrectFunction take_ed(CorrectFunction source, CorrectFunction clause) {
    checkAlive("take.ed");
    check("take.ed", source, clause);
    return owner;
  }

  @Override
  public CorrectFunction take_ei(CorrectFunction source, CorrectFunction count) {
    checkAlive("take.ei");
    check("take.ei", source, count);
    return owner;
  }

  @Override
  public CorrectFunction takel_ei(CorrectFunction source, CorrectFunction count) {
    checkAlive("takel.ei");
    check("takel.ei", source, count);
    return owner;
  }

  @Override
  public CorrectFunction ttoa(CorrectFunction value) {
    checkAlive("ttoa");
    check("ttoa", value);
    return owner;
  }

  @Override
  public void update(SourceLocation location, String message) {
    this.location = location;
  }

  @Override
  public CorrectFunction window_g(CorrectFunction length, CorrectFunction next) {
    checkAlive("window.g");
    check("window.g", length, next);
    return owner;
  }

  @Override
  public CorrectFunction xor_i(CorrectFunction left, CorrectFunction right) {
    checkAlive("xor.i");
    check("xor.i", left, right);
    return owner;
  }

  @Override
  public CorrectFunction ztoa(CorrectFunction value) {
    checkAlive("ztoa");
    check("ztoa", value);
    return owner;
  }

  @Override
  public CorrectFunction ztos(CorrectFunction value) {
    checkAlive("ztos");
    check("ztos", value);
    return owner;
  }
}
