package flabbergast.compiler.kws;

import flabbergast.compiler.FlabbergastType;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.Streamable;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Creator for a basic block in the KWS language
 *
 * <p>A basic block is one which has any number of single static assignments instructions followed
 * by a single termination instruction which transfers flow control to other blocks, returns, or
 * raises an error. Block may take parameters. Also though these parameters look like function
 * parameters, control is transferred from one block to another, with the provided parameters.
 * Parameters replace the need for PHI functions in the SSA.
 *
 * <p>All the methods match their KWS versions
 *
 * @param <V> the type of a value
 * @param <B> the type of a block
 * @param <D> the type of a dispatch
 */
public interface KwsBlock<V, B extends KwsBlock<V, B, D>, D extends KwsDispatch<B, V>> {
  V add_f(V left, V right);

  V add_i(V left, V right);

  V add_n(V source, Streamable<String> names);

  V add_n_a(V source, V value);

  V add_n_i(V source, V ordinal);

  V add_n_r(V source, V frame);

  V add_n_s(V source, V name);

  V adjacent_f(V name, V definition);

  V adjacent_i(V name, V definition);

  V adjacent_s(V name, V definition);

  V adjacent_z(V name, V definition);

  V alwaysinclude_f(V name, V key);

  V alwaysinclude_i(V name, V key);

  V alwaysinclude_s(V name, V key);

  V alwaysinclude_z(V name, V key);

  V and_g(Streamable<V> groupers);

  V and_i(V left, V right);

  V atos(V value);

  V atoz(V value, Predicate<FlabbergastType> include);

  V boundary(V definition, V trailing);

  void br(B target, Streamable<V> arguments);

  void br_a(V value, D dispatch, Optional<String> error);

  void br_aa(
      V left,
      V right,
      B intTarget,
      Streamable<V> intArguments,
      B floatTarget,
      Streamable<V> floatArguments);

  void br_fa(V left, V right, B target, Streamable<V> arguments);

  void br_ia(
      V left,
      V right,
      B intTarget,
      Streamable<V> intArguments,
      B floatTarget,
      Streamable<V> floatArguments);

  void br_z(
      V condition,
      B trueTarget,
      Streamable<V> trueArguments,
      B falseTarget,
      Streamable<V> falseArguments);

  V btoa(V value);

  V buckets_f(V definition, V count);

  V buckets_i(V definition, V count);

  V buckets_s(V definition, V count);

  V call_d(V definition, V context);

  V call_o(V override, V context, V original);

  V cat_e(V context, Streamable<V> chains);

  V cat_ke(V definition, V chain);

  V cat_r(V context, V first, V second);

  V cat_rc(V head, V tail);

  V cat_s(V first, V second);

  V chunk_e(V width);

  V cmp_f(V left, V right);

  V cmp_i(V left, V right);

  V cmp_s(V left, V right);

  V cmp_z(V left, V right);

  V contextual();

  V count_w(V count);

  D createDispatch();

  V crosstab_f(V key);

  V crosstab_i(V key);

  V crosstab_s(V key);

  V crosstab_z(V key);

  V ctr_c(V value);

  V ctr_r(V frame);

  V ctxt_r(V context, V frame);

  V debug_d(V definition, V context);

  V disc_g_f(V name, V getter);

  V disc_g_i(V name, V getter);

  V disc_g_s(V name, V getter);

  V disc_g_z(V name, V getter);

  void disperse_i(V frame, V name, V value);

  void disperse_s(V frame, V name, V value);

  V div_f(V left, V right);

  V div_i(V left, V right);

  V drop_ed(V source, V clause);

  V drop_ei(V source, V count);

  V drop_x(V name);

  V dropl_ei(V source, V count);

  V duration_f(V definition, V duration);

  V duration_i(V definition, V duration);

  void error(V message);

  V etoa_ao(V source, V initial, V reducer);

  V etoa_d(V source, V extractor);

  V etoa_dd(V source, V extractor, V alternate);

  V etod(V source, V computeValue);

  V etod_a(V source, V computeValue, V empty);

  V etoe_g(V source, Streamable<V> groupers);

  V etoe_m(V source, V initial, V reducer);

  V etoe_u(V source, V flattener);

  V etoi(V source);

  V etor_ao(V source, V initial, V reducer);

  V etor_i(V source, V computeValue);

  V etor_s(V source, V computeName, V computeValue);

  V ext(String uri);

  V f(double value);

  V filt_e(V source, V clause);

  V ftoa(V value);

  V ftoi(V value);

  V ftos(V value);

  V gather_i(V frame, V ordinal);

  V gather_s(V frame, V name);

  V i(long value);

  V id(V frame);

  V importFunction(String kwsName, KwsType returnType, Streamable<V> arguments);

  V inf_f();

  V is_finite(V value);

  V is_nan(V value);

  V itoa(V value);

  V itof(V value);

  V itos(V value);

  V itoz(long reference, V value);

  V ktol(V name, V context, V definition);

  V len_b(V blob);

  V len_s(V str);

  V let_e(V source, Streamable<V> builder);

  V lookup(V context, Streamable<String> names);

  V lookup_l(V handler, V context, V names);

  V ltoa(V value);

  V max_f();

  V max_i();

  V max_z();

  V min_f();

  V min_i();

  V min_z();

  V mod_f(V left, V right);

  V mod_i(V left, V right);

  V mtoe(V context, V initial, V definition);

  V mul_f(V left, V right);

  V mul_i(V left, V right);

  V nan_f();

  V neg_f(V value);

  V neg_i(V value);

  V new_g(V name, V collector);

  V new_g_a(V name, V value);

  V new_p(V context, V intersect, Streamable<V> zippers);

  V new_p_i(V name);

  V new_p_r(V name, V frame);

  V new_p_s(V name);

  V new_r(V selfIsThis, V context, Streamable<V> gatherers, Streamable<V> builder);

  V new_r_i(V context, V start, V end);

  V new_t(V context, Streamable<V> gatherers, Streamable<V> builder);

  V new_x_ia(V ordinal, V value);

  V new_x_sa(V name, V value);

  V new_x_sd(V name, V definition);

  V new_x_so(V name, V override);

  V nil_a();

  V nil_c();

  V nil_n();

  V nil_r();

  V nil_w();

  V not_g(V value);

  V not_i(V value);

  V not_z(V value);

  V or_g(Streamable<V> groupers);

  V or_i(V left, V right);

  V ord_e_f(V source, V ascending, V clause);

  V ord_e_i(V source, V ascending, V clause);

  V ord_e_s(V source, V ascending, V clause);

  V ord_e_z(V source, V ascending, V clause);

  /**
   * Get the value of a parameter provided to this block
   *
   * @param index the zero-based index of the parameter
   */
  V parameter(int index);

  /** Get the number of parameters provided to this block */
  int parameters();

  V powerset(Streamable<V> groupers);

  V priv_cr(V context, V frame);

  V priv_x(V inner);

  V require_x(V name);

  void ret(V value);

  void ret(V value, Streamable<V> builders);

  V rev_e(V source);

  V ring_g(V primitive, V size);

  V rtoa(V value);

  V rtoe(V source, V context);

  V s(String value);

  V seal_d(V definition, V context);

  V seal_o(V definition, V context);

  V session_f(V definition, V adjacent, V maximum);

  V session_i(V definition, V adjacent, V maximum);

  V sh_i(V value, V offset);

  V shuf_e(V source);

  V stoa(V value);

  V stripe_e(V width);

  V sub_f(V left, V right);

  V sub_i(V left, V right);

  V take_ed(V source, V clause);

  V take_ei(V source, V count);

  V takel_ei(V source, V count);

  V ttoa(V value);

  void update(SourceLocation location, String message);

  V window_g(V length, V next);

  V xor_i(V left, V right);

  V ztoa(V value);

  V ztos(V value);
}
