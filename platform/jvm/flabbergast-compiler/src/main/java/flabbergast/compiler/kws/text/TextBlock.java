package flabbergast.compiler.kws.text;

import flabbergast.compiler.FlabbergastType;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.Streamable;
import flabbergast.compiler.kws.KwsBlock;
import flabbergast.compiler.kws.KwsType;
import flabbergast.util.Numberer;
import flabbergast.util.Pair;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextBlock implements KwsBlock<Printable, TextBlock, TextDispatch> {

  private final StringBuilder builder = new StringBuilder();
  private int counter;
  private final String id;
  private final int parameters;
  private final Consumer<String> writeBack;

  public TextBlock(Consumer<String> writeBack, String id, Stream<KwsType> parameters) {
    this.writeBack = writeBack;
    this.id = id;
    builder.append("block ").append(id).append("(");
    final Numberer<Integer, KwsType> numberer = Pair.number();
    builder.append(
        parameters
            .map(numberer)
            .map(p -> String.format("p%d:%s", p.first(), p.second().name().toLowerCase()))
            .collect(Collectors.joining(", ")));
    builder.append("):\n");
    this.parameters = numberer.size();
  }

  @Override
  public Printable add_f(Printable left, Printable right) {
    return printr("add.f", left, right);
  }

  @Override
  public Printable add_i(Printable left, Printable right) {
    return printr("add.i", left, right);
  }

  @Override
  public Printable add_n(Printable source, Streamable<String> names) {
    return printr(
        "add.n",
        source,
        sb ->
            Printable.print(sb, names.stream().map(name -> "\"" + name + "\"").map(Printable::of)));
  }

  @Override
  public Printable add_n_a(Printable source, Printable value) {
    return printr("add.n.a", value);
  }

  @Override
  public Printable add_n_i(Printable source, Printable ordinal) {
    return printr("add.n.i", source, ordinal);
  }

  @Override
  public Printable add_n_r(Printable source, Printable frame) {
    return printr("add.n.r", source, frame);
  }

  @Override
  public Printable add_n_s(Printable source, Printable name) {
    return printr("add.n.s", source, name);
  }

  @Override
  public Printable adjacent_f(Printable name, Printable definition) {
    return printr("adjacent.f", name, definition);
  }

  @Override
  public Printable adjacent_i(Printable name, Printable definition) {
    return printr("adjacent.i", name, definition);
  }

  @Override
  public Printable adjacent_s(Printable name, Printable definition) {
    return printr("adjacent.s", name, definition);
  }

  @Override
  public Printable adjacent_z(Printable name, Printable definition) {
    return printr("adjacent.z", name, definition);
  }

  @Override
  public Printable alwaysinclude_f(Printable name, Printable key) {
    return printr("alwaysinclude.f", name, key);
  }

  @Override
  public Printable alwaysinclude_i(Printable name, Printable key) {
    return printr("alwaysinclude.i", name, key);
  }

  @Override
  public Printable alwaysinclude_s(Printable name, Printable key) {
    return printr("alwaysinclude.s", name, key);
  }

  @Override
  public Printable alwaysinclude_z(Printable name, Printable key) {
    return printr("alwaysinclude.z", name, key);
  }

  @Override
  public Printable and_g(Streamable<Printable> groupers) {
    return printr("and.g", Printable.all(groupers));
  }

  @Override
  public Printable and_i(Printable left, Printable right) {
    return printr("and.i", left, right);
  }

  @Override
  public Printable atos(Printable value) {
    return printr("atos", value);
  }

  @Override
  public Printable atoz(Printable value, Predicate<FlabbergastType> include) {
    return printr(
        "atoz",
        Stream.concat(
                Stream.of(value),
                Stream.of(FlabbergastType.values())
                    .filter(include)
                    .map(
                        type ->
                            Printable.of(
                                type.kwsType() == null
                                    ? "-"
                                    : type.kwsType().name().toLowerCase())))
            .toArray(Printable[]::new));
  }

  @Override
  public Printable boundary(Printable definition, Printable trailing) {
    return printr("boundary", definition, trailing);
  }

  @Override
  public void br(TextBlock target, Streamable<Printable> arguments) {
    print("br", Printable.block(target, arguments));
  }

  @Override
  public void br_a(Printable value, TextDispatch dispatch, Optional<String> error) {
    print("br.a", dispatch.toArray());
    error.map(e -> " \"" + Pattern.quote(e) + "\"").ifPresent(builder::append);
  }

  @Override
  public void br_aa(
      Printable left,
      Printable right,
      TextBlock intTarget,
      Streamable<Printable> intArguments,
      TextBlock floatTarget,
      Streamable<Printable> floatArguments) {
    print(
        "br.aa",
        left,
        right,
        Printable.block(intTarget, intArguments),
        Printable.block(floatTarget, floatArguments));
    writeBack.accept(builder.toString());
  }

  @Override
  public void br_fa(
      Printable left, Printable right, TextBlock target, Streamable<Printable> arguments) {
    print("br.fa", left, right, Printable.block(target, arguments));
    writeBack.accept(builder.toString());
  }

  @Override
  public void br_ia(
      Printable left,
      Printable right,
      TextBlock intTarget,
      Streamable<Printable> intArguments,
      TextBlock floatTarget,
      Streamable<Printable> floatArguments) {
    print(
        "br.ia",
        left,
        right,
        Printable.block(intTarget, intArguments),
        Printable.block(floatTarget, floatArguments));
    writeBack.accept(builder.toString());
  }

  @Override
  public void br_z(
      Printable condition,
      TextBlock trueTarget,
      Streamable<Printable> trueArguments,
      TextBlock falseTarget,
      Streamable<Printable> falseArguments) {
    print(
        "br.a",
        condition,
        Printable.block(trueTarget, trueArguments),
        Printable.block(falseTarget, falseArguments));
    writeBack.accept(builder.toString());
  }

  @Override
  public Printable btoa(Printable value) {
    return printr("btoa", value);
  }

  @Override
  public Printable buckets_f(Printable definition, Printable count) {
    return printr("buckets.f", definition, count);
  }

  @Override
  public Printable buckets_i(Printable definition, Printable count) {
    return printr("buckets.i", definition, count);
  }

  @Override
  public Printable buckets_s(Printable definition, Printable count) {
    return printr("buckets.s", definition, count);
  }

  @Override
  public Printable call_d(Printable definition, Printable context) {
    return printr("call.d", definition, context);
  }

  @Override
  public Printable call_o(Printable override, Printable context, Printable original) {
    return printr("call.o", override, context, original);
  }

  @Override
  public Printable cat_e(Printable context, Streamable<Printable> chains) {
    return printr("cat.e", context, Printable.all(chains));
  }

  @Override
  public Printable cat_ke(Printable definition, Printable chain) {
    return printr("cat.ke", definition, chain);
  }

  @Override
  public Printable cat_r(Printable context, Printable first, Printable second) {
    return printr("cat.r", context, first, second);
  }

  @Override
  public Printable cat_rc(Printable head, Printable tail) {
    return printr("cat.rc", head, tail);
  }

  @Override
  public Printable cat_s(Printable first, Printable second) {
    return printr("cat.s", first, second);
  }

  @Override
  public Printable chunk_e(Printable width) {
    return printr("chunk.e", width);
  }

  @Override
  public Printable cmp_f(Printable left, Printable right) {
    return printr("cmp.f", left, right);
  }

  @Override
  public Printable cmp_i(Printable left, Printable right) {
    return printr("cmp.i", left, right);
  }

  @Override
  public Printable cmp_s(Printable left, Printable right) {
    return printr("cmp.s", left, right);
  }

  @Override
  public Printable cmp_z(Printable left, Printable right) {
    return printr("cmp.z", left, right);
  }

  @Override
  public Printable contextual() {
    return printr("contextual");
  }

  @Override
  public Printable count_w(Printable count) {
    return printr("count.w", count);
  }

  @Override
  public TextDispatch createDispatch() {
    return new TextDispatch();
  }

  @Override
  public Printable crosstab_f(Printable key) {
    return printr("crosstab.f", key);
  }

  @Override
  public Printable crosstab_i(Printable key) {
    return printr("crosstab.i", key);
  }

  @Override
  public Printable crosstab_s(Printable key) {
    return printr("crosstab.s", key);
  }

  @Override
  public Printable crosstab_z(Printable key) {
    return printr("crosstab.z", key);
  }

  @Override
  public Printable ctr_c(Printable value) {
    return printr("ctr.c", value);
  }

  @Override
  public Printable ctr_r(Printable frame) {
    return printr("ctr.r", frame);
  }

  @Override
  public Printable ctxt_r(Printable context, Printable frame) {
    return printr("ctxt.r", context, frame);
  }

  @Override
  public Printable debug_d(Printable definition, Printable context) {
    return printr("debug.d", definition, context);
  }

  @Override
  public Printable disc_g_f(Printable name, Printable getter) {
    return printr("disc.g.f", name, getter);
  }

  @Override
  public Printable disc_g_i(Printable name, Printable getter) {
    return printr("disc.g.i", name, getter);
  }

  @Override
  public Printable disc_g_s(Printable name, Printable getter) {
    return printr("disc.g.s", name, getter);
  }

  @Override
  public Printable disc_g_z(Printable name, Printable getter) {
    return printr("disc.g.z", name, getter);
  }

  @Override
  public void disperse_i(Printable frame, Printable name, Printable value) {
    print("disperse.i", frame, name, value);
  }

  @Override
  public void disperse_s(Printable frame, Printable name, Printable value) {
    print("disperse.s", frame, name, value);
  }

  @Override
  public Printable div_f(Printable left, Printable right) {
    return printr("div.f", left, right);
  }

  @Override
  public Printable div_i(Printable left, Printable right) {
    return printr("div.i", left, right);
  }

  @Override
  public Printable drop_ed(Printable source, Printable clause) {
    return printr("drop.ed", source, clause);
  }

  @Override
  public Printable drop_ei(Printable source, Printable count) {
    return printr("drop.ei", source, count);
  }

  @Override
  public Printable drop_x(Printable name) {
    return printr("drop.x", name);
  }

  @Override
  public Printable dropl_ei(Printable source, Printable count) {
    return printr("dropl.ei", source, count);
  }

  @Override
  public Printable duration_f(Printable definition, Printable duration) {
    return printr("duration.f", definition, duration);
  }

  @Override
  public Printable duration_i(Printable definition, Printable duration) {
    return printr("duration.i", definition, duration);
  }

  @Override
  public void error(Printable message) {
    print("error", message);
    writeBack.accept(builder.toString());
  }

  @Override
  public Printable etoa_ao(Printable source, Printable initial, Printable reducer) {
    return printr("etoa.ao", source, initial, reducer);
  }

  @Override
  public Printable etoa_d(Printable source, Printable extractor) {
    return printr("etoa.d", source, extractor);
  }

  @Override
  public Printable etoa_dd(Printable source, Printable extractor, Printable alternate) {
    return printr("etoa.dd", source, extractor, alternate);
  }

  @Override
  public Printable etod(Printable source, Printable computeValue) {
    return printr("etod", source, computeValue);
  }

  @Override
  public Printable etod_a(Printable source, Printable computeValue, Printable empty) {
    return printr("etod.a", source, computeValue, empty);
  }

  @Override
  public Printable etoe_g(Printable source, Streamable<Printable> groupers) {
    return printr("etoe.g", source, Printable.all(groupers));
  }

  @Override
  public Printable etoe_m(Printable source, Printable initial, Printable reducer) {
    return printr("etoe.m", initial, reducer);
  }

  @Override
  public Printable etoe_u(Printable source, Printable flattener) {
    return printr("etoe.u", source, flattener);
  }

  @Override
  public Printable etoi(Printable source) {
    return printr("etoi", source);
  }

  @Override
  public Printable etor_ao(Printable source, Printable initial, Printable reducer) {
    return printr("etor.ao", source, initial, reducer);
  }

  @Override
  public Printable etor_i(Printable source, Printable compute_value) {
    return printr("etor.i", source, compute_value);
  }

  @Override
  public Printable etor_s(Printable source, Printable compute_name, Printable compute_value) {
    return printr("etor.s", source, compute_name, compute_value);
  }

  @Override
  public Printable ext(String uri) {
    return printr("ext", Printable.of("\"" + uri + "\""));
  }

  @Override
  public Printable f(double value) {
    return printr("f", sb -> sb.append(value));
  }

  @Override
  public Printable filt_e(Printable source, Printable clause) {
    return printr("filt.e", source, clause);
  }

  @Override
  public Printable ftoa(Printable value) {
    return printr("ftoa", value);
  }

  @Override
  public Printable ftoi(Printable value) {
    return printr("ftoi", value);
  }

  @Override
  public Printable ftos(Printable value) {
    return printr("ftos", value);
  }

  @Override
  public Printable gather_i(Printable frame, Printable name) {
    return printr("gather.i", frame, name);
  }

  @Override
  public Printable gather_s(Printable frame, Printable name) {
    return printr("gather.s", frame, name);
  }

  @Override
  public Printable i(long value) {
    return printr("i", sb -> sb.append(value));
  }

  public String id() {
    return id;
  }

  @Override
  public Printable id(Printable frame) {
    return printr("id", frame);
  }

  @Override
  public Printable importFunction(
      String kwsName, KwsType returnType, Streamable<Printable> arguments) {
    return printr(
        "import." + returnType.name().toLowerCase(),
        Printable.of(kwsName),
        Printable.all(arguments));
  }

  @Override
  public Printable inf_f() {
    return printr("inf.f");
  }

  @Override
  public Printable is_finite(Printable value) {
    return printr("is_finite", value);
  }

  @Override
  public Printable is_nan(Printable value) {
    return printr("is_nan", value);
  }

  @Override
  public Printable itoa(Printable value) {
    return printr("itoa", value);
  }

  @Override
  public Printable itof(Printable value) {
    return printr("itof", value);
  }

  @Override
  public Printable itos(Printable value) {
    return printr("itos", value);
  }

  @Override
  public Printable itoz(long reference, Printable value) {
    return printr("itoz", value);
  }

  @Override
  public Printable ktol(Printable name, Printable context, Printable definition) {
    return printr("ktol", name, context, definition);
  }

  @Override
  public Printable len_b(Printable blob) {
    return printr("len.b", blob);
  }

  @Override
  public Printable len_s(Printable str) {
    return printr("len.s", str);
  }

  @Override
  public Printable let_e(Printable source, Streamable<Printable> builders) {
    return printr("let.e", source, Printable.all(builders));
  }

  @Override
  public Printable lookup(Printable context, Streamable<String> names) {
    return printr(
        "lookup",
        sb ->
            Printable.print(sb, names.stream().map(name -> "\"" + name + "\"").map(Printable::of)));
  }

  @Override
  public Printable lookup_l(Printable handler, Printable context, Printable names) {
    return printr("lookup.l", handler, context, names);
  }

  @Override
  public Printable ltoa(Printable value) {
    return printr("ltoa", value);
  }

  @Override
  public Printable max_f() {
    return printr("max.f");
  }

  @Override
  public Printable max_i() {
    return printr("max.i");
  }

  @Override
  public Printable max_z() {
    return printr("max.z");
  }

  @Override
  public Printable min_f() {
    return printr("min.f");
  }

  @Override
  public Printable min_i() {
    return printr("min.i");
  }

  @Override
  public Printable min_z() {
    return printr("min.z");
  }

  @Override
  public Printable mod_f(Printable left, Printable right) {
    return printr("mod.f", left, right);
  }

  @Override
  public Printable mod_i(Printable left, Printable right) {
    return printr("mod.i", left, right);
  }

  @Override
  public Printable mtoe(Printable context, Printable initial, Printable definition) {
    return printr("mtoe", context, initial, definition);
  }

  @Override
  public Printable mul_f(Printable left, Printable right) {
    return printr("mul.f");
  }

  @Override
  public Printable mul_i(Printable left, Printable right) {
    return printr("mul.i");
  }

  @Override
  public Printable nan_f() {
    return printr("nan.f");
  }

  @Override
  public Printable neg_f(Printable value) {
    return printr("neg.f");
  }

  @Override
  public Printable neg_i(Printable value) {
    return printr("neg.i");
  }

  @Override
  public Printable new_g(Printable name, Printable collector) {
    return printr("new.g", name, collector);
  }

  @Override
  public Printable new_g_a(Printable name, Printable value) {
    return printr("new.g.a", name, value);
  }

  @Override
  public Printable new_p(Printable context, Printable intersect, Streamable<Printable> zippers) {
    return printr("new.p", context, intersect, Printable.all(zippers));
  }

  @Override
  public Printable new_p_i(Printable name) {
    return printr("new.p.i", name);
  }

  @Override
  public Printable new_p_r(Printable name, Printable frame) {
    return printr("new.p.r", name, frame);
  }

  @Override
  public Printable new_p_s(Printable name) {
    return printr("new.p.s", name);
  }

  @Override
  public Printable new_r(
      Printable selfIsThis,
      Printable context,
      Streamable<Printable> gatherers,
      Streamable<Printable> builder) {
    return printr("new.r", selfIsThis, context, Printable.all(gatherers), Printable.all(builder));
  }

  @Override
  public Printable new_r_i(Printable context, Printable start, Printable end) {
    return printr("new.r.i", context, start, end);
  }

  @Override
  public Printable new_t(
      Printable context, Streamable<Printable> gatherers, Streamable<Printable> builder) {
    return printr("new.t", context, Printable.all(gatherers), Printable.all(builder));
  }

  @Override
  public Printable new_x_ia(Printable ordinal, Printable value) {
    return printr("new.x.i", ordinal, value);
  }

  @Override
  public Printable new_x_sa(Printable name, Printable value) {
    return printr("new.x.x", name, value);
  }

  @Override
  public Printable new_x_sd(Printable name, Printable definition) {
    return printr("new.x.d", name, definition);
  }

  @Override
  public Printable new_x_so(Printable name, Printable override) {
    return printr("new.x.o", name, override);
  }

  @Override
  public Printable nil_a() {
    return printr("nil.a");
  }

  @Override
  public Printable nil_c() {
    return printr("nil.c");
  }

  @Override
  public Printable nil_n() {
    return printr("nil.n");
  }

  @Override
  public Printable nil_r() {
    return printr("nil.r");
  }

  @Override
  public Printable nil_w() {
    return printr("nil.w");
  }

  @Override
  public Printable not_g(Printable value) {
    return printr("not.g", value);
  }

  @Override
  public Printable not_i(Printable value) {
    return printr("not.i", value);
  }

  @Override
  public Printable not_z(Printable value) {
    return printr("not.z", value);
  }

  @Override
  public Printable or_g(Streamable<Printable> groupers) {
    return printr("or.g", Printable.all(groupers));
  }

  @Override
  public Printable or_i(Printable left, Printable right) {
    return printr("or.i", left, right);
  }

  @Override
  public Printable ord_e_f(Printable source, Printable ascending, Printable clause) {
    return printr("ord.e.f", source, ascending, clause);
  }

  @Override
  public Printable ord_e_i(Printable source, Printable ascending, Printable clause) {
    return printr("ord.e.i", source, ascending, clause);
  }

  @Override
  public Printable ord_e_s(Printable source, Printable ascending, Printable clause) {
    return printr("ord.e.s", source, ascending, clause);
  }

  @Override
  public Printable ord_e_z(Printable source, Printable ascending, Printable clause) {
    return printr("ord.e.z", source, ascending, clause);
  }

  @Override
  public Printable parameter(int i) {
    return sb -> sb.append("p").append(i);
  }

  @Override
  public int parameters() {
    return parameters;
  }

  @Override
  public Printable powerset(Streamable<Printable> groupers) {
    return printr("powerset", Printable.all(groupers));
  }

  private void print(String name, Printable... arguments) {
    builder.append(name);
    builder.append(" ");
    for (var i = 0; i < arguments.length; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      arguments[i].appendTo(builder);
    }
    builder.append("\n");
  }

  Printable printr(String name, Printable... arguments) {
    final var id = String.format("v%s_%d", this.id, counter++);
    builder.append(id);
    builder.append(" = ");
    print(name, arguments);
    return sb -> sb.append(id);
  }

  @Override
  public Printable priv_cr(Printable context, Printable frame) {
    return printr("priv.cr", context, frame);
  }

  @Override
  public Printable priv_x(Printable inner) {
    return printr("priv.x", inner);
  }

  @Override
  public Printable require_x(Printable name) {
    return printr("require.x", name);
  }

  @Override
  public void ret(Printable value) {
    print("ret", value);
    writeBack.accept(builder.toString());
  }

  @Override
  public void ret(Printable value, Streamable<Printable> builders) {
    print("ret", value, Printable.all(builders));
    writeBack.accept(builder.toString());
  }

  @Override
  public Printable rev_e(Printable source) {
    return printr("rev.e", source);
  }

  @Override
  public Printable ring_g(Printable primitive, Printable size) {
    return printr("ring.g", primitive, size);
  }

  @Override
  public Printable rtoa(Printable value) {
    return printr("rtoa", value);
  }

  @Override
  public Printable rtoe(Printable source, Printable context) {
    return printr("rtoe", source, context);
  }

  @Override
  public Printable s(String value) {
    return printr("s", Printable.of("\"" + value + "\""));
  }

  @Override
  public Printable seal_d(Printable definition, Printable context) {
    return printr("seal.d", definition, context);
  }

  @Override
  public Printable seal_o(Printable definition, Printable context) {
    return printr("seal.o", definition, context);
  }

  @Override
  public Printable session_f(Printable definition, Printable adjacent, Printable maximum) {
    return printr("session.f", definition, adjacent, maximum);
  }

  @Override
  public Printable session_i(Printable definition, Printable adjacent, Printable maximum) {
    return printr("session.i", definition, adjacent, maximum);
  }

  @Override
  public Printable sh_i(Printable value, Printable offset) {
    return printr("sh.i", value, offset);
  }

  @Override
  public Printable shuf_e(Printable source) {
    return printr("shuf.e", source);
  }

  @Override
  public Printable stoa(Printable value) {
    return printr("stoa", value);
  }

  @Override
  public Printable stripe_e(Printable width) {
    return printr("stripe.e", width);
  }

  @Override
  public Printable sub_f(Printable left, Printable right) {
    return printr("sub.f", left, right);
  }

  @Override
  public Printable sub_i(Printable left, Printable right) {
    return printr("sub.r", left, right);
  }

  @Override
  public Printable take_ed(Printable source, Printable clause) {
    return printr("take.ed", source, clause);
  }

  @Override
  public Printable take_ei(Printable source, Printable count) {
    return printr("take.ei", source, count);
  }

  @Override
  public Printable takel_ei(Printable source, Printable count) {
    return printr("takel.ei", source, count);
  }

  @Override
  public Printable ttoa(Printable value) {
    return printr("ttoa", value);
  }

  @Override
  public void update(SourceLocation location, String message) {}

  @Override
  public Printable window_g(Printable length, Printable next) {
    return printr("window.g", length, next);
  }

  @Override
  public Printable xor_i(Printable left, Printable right) {
    return printr("xor.i", left, right);
  }

  @Override
  public Printable ztoa(Printable value) {
    return printr("ztoa", value);
  }

  @Override
  public Printable ztos(Printable value) {
    return printr("ztos", value);
  }
}
