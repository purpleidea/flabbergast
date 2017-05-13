package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.KwsType.*;
import static flabbergast.compiler.kws.codegen.InputParameterType.*;
import static flabbergast.compiler.kws.codegen.Invokable.staticInterfaceMethod;
import static flabbergast.compiler.kws.codegen.Invokable.virtualMethod;
import static flabbergast.compiler.kws.codegen.LanguageType.*;
import static flabbergast.compiler.kws.codegen.LoadableValue.CONTEXTUAL;
import static org.objectweb.asm.Type.*;

import flabbergast.compiler.kws.KwsType;
import flabbergast.lang.FricasseeGrouper;
import flabbergast.lang.FricasseeZipper;
import java.util.function.IntConsumer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

enum SimpleOpcode implements GeneralOpcode {
  ADD_F("add.f", false, F, single(F), single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(GeneratorAdapter.ADD, DOUBLE_TYPE);
    }
  },
  ADD_I("add.i", false, I, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(GeneratorAdapter.ADD, LONG_TYPE);
    }
  },

  ADD_N(
      "add.n",
      false,
      N,
      single(N),
      fold(S, virtualMethod(NAME_SOURCE_TYPE, NAME_SOURCE_TYPE, "add", JSTRING_TYPE))) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
    }
  },
  ADD_N_A("add.n.a", false, N, single(N), single(A)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      NAME_SOURCE__ADD_TYPE_OF.invoke(output.generator());
    }
  },
  ADD_N_I("add.n.i", false, N, single(N), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      NAME_SOURCE__ADD_ORDINAL.invoke(output.generator());
    }
  },

  ADD_N_R("add.n.r", false, N, single(N), single(R)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      NAME_SOURCE__ADD_FRAME.invoke(output.generator());
    }
  },

  ADD_N_S("add.n.s", false, N, single(N), single(S)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      NAME_SOURCE__ADD_STR.invoke(output.generator());
    }
  },

  ADJACENT_F("adjacent.f", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__ADJACENT_FLOAT.invoke(output.generator());
    }
  },

  ADJACENT_I("adjacent.i", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__ADJACENT_INT.invoke(output.generator());
    }
  },

  ADJACENT_S("adjacent.s", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__ADJACENT_STR.invoke(output.generator());
    }
  },

  ADJACENT_Z("adjacent.z", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__ADJACENT_BOOL.invoke(output.generator());
    }
  },

  ALWAYSINCLUDE_F("alwaysinclude.f", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__ALWAYS_INCLUDE_FLOAT.invoke(output.generator());
    }
  },

  ALWAYSINCLUDE_I("alwaysinclude.f", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__ALWAYS_INCLUDE_INT.invoke(output.generator());
    }
  },

  ALWAYSINCLUDE_S("alwaysinclude.s", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__ALWAYS_INCLUDE_STR.invoke(output.generator());
    }
  },

  ALWAYSINCLUDE_Z("alwaysinclude.a", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__ALWAYS_INCLUDE_BOOL.invoke(output.generator());
    }
  },

  AND_G("and.g", false, G, arrayOf(G)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_GROUPER__ALL.invoke(output.generator());
    }
  },

  AND_I("and.i", false, I, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(GeneratorAdapter.AND, LONG_TYPE);
    }
  },
  ATOS("atos", true, S, single(A)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadFuture();
      output.loadSource();
      loadCallback.run();
      ANY__TO_STR.invoke(output.generator());
    }
  },

  BOUNDARY("boundary", false, G, single(D), single(Z)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__BOUNDARY.invoke(output.generator());
    }
  },

  BTOA("btoa", false, A, single(B)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      ANY__OF_BIN.invoke(output.generator());
    }
  },
  BUCKETS_F("buckets.f", false, G, single(D), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__BUCKETS_FLOAT.invoke(output.generator());
    }
  },
  BUCKETS_I("buckets.i", false, G, single(D), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__BUCKETS_INT.invoke(output.generator());
    }
  },
  BUCKETS_S("buckets.s", false, G, single(D), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__BUCKETS_STR.invoke(output.generator());
    }
  },
  CALL_D("call.d", true, A, single(D), single(C)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      output.loadFuture();
      values.accept(0);
      output.loadSource();
      values.accept(1);
      loadCallback.run();
      FUTURE__LAUNCH_DEFINITION.invoke(output.generator());
    }
  },
  CALL_O("call.o", true, A, single(O), single(C), single(A)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      output.loadFuture();
      values.accept(0);
      output.loadSource();
      values.accept(1);
      values.accept(2);
      loadCallback.run();
      FUTURE__LAUNCH_OVERRIDE.invoke(output.generator());
    }
  },
  CAT_E("cat.e", false, E, single(C), arrayOf(E)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE__CONCAT.invoke(output.generator());
    }
  },
  CAT_KE("cat.ke", false, D, single(K), single(E)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      COLLECTOR_DEFINITION__BIND.invoke(output.generator());
    }
  },
  CAT_R("cat.r", false, R, single(C), single(R), single(R)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(1);
      output.loadFuture();
      output.loadSource();
      values.accept(0);
      values.accept(2);
      FRAME__APPEND.invoke(output.generator());
    }
  },
  CAT_RC("cat.rc", false, C, single(F), single(C)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      CONTEXT__PREPEND.invoke(output.generator());
    }
  },

  CAT_S("cat.s", false, S, single(S), single(S)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(0);
      STR__CONCAT.invoke(output.generator());
    }
  },

  CHUNK_E("chunk.e", false, G, single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_GROUPER__CHUNK.invoke(output.generator());
    }
  },
  CMP_F("cmp.f", false, I, single(F), single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().visitInsn(Opcodes.DCMPG);

      output.generator().cast(INT_TYPE, LONG_TYPE);
    }
  },
  CMP_I("cmp.i", false, I, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().visitInsn(Opcodes.LCMP);

      output.generator().cast(INT_TYPE, LONG_TYPE);
    }
  },
  CMP_S("cmp.s", false, I, single(S), single(S)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      STR__COMPARE.invoke(output.generator());
    }
  },

  CMP_Z("cmp.z", false, I, single(Z), single(Z)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(GeneratorAdapter.SUB, BOOLEAN_TYPE);

      output.generator().cast(INT_TYPE, LONG_TYPE);
    }
  },
  COUNT_W("count.w", false, W, single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_WINDOW__COUNT.invoke(output.generator());
    }
  },
  CROSSTAB_F("crosstab.f", false, G, single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_GROUPER__CROSSTAB_FLOAT.invoke(output.generator());
    }
  },

  CROSSTAB_I("crosstab.f", false, G, single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_GROUPER__CROSSTAB_INT.invoke(output.generator());
    }
  },
  CROSSTAB_S("crosstab.s", false, G, single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_GROUPER__CROSSTAB_STR.invoke(output.generator());
    }
  },
  CROSSTAB_Z("crosstab.z", false, G, single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_GROUPER__CROSSTAB_BOOL.invoke(output.generator());
    }
  },

  CTR_C("ctr.c", false, R, single(C)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      CONTEXT__SELF.invoke(output.generator());
    }
  },

  CTR_R("ctr.r", false, R, single(R)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRAME__CONTAINER.invoke(output.generator());
    }
  },
  CTXT_R("ctxt.r", false, C, single(C), single(R)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      CONTEXT__FOR_FRAME.invoke(output.generator());
    }
  },
  DEBUG_D("debug.d", true, A, single(D), single(C)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      output.loadFuture();
      values.accept(0);
      output.loadSource();
      values.accept(1);
      loadCallback.run();
      FUTURE__DEBUG_DEFINITION.invoke(output.generator());
    }
  },

  DISC_G_F("disc.g.f", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__DISCRIMINATE_FLOAT.invoke(output.generator());
    }
  },

  DISC_G_I("disc.g.i", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__DISCRIMINATE_INT.invoke(output.generator());
    }
  },
  DISC_G_S("disc.g.s", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__DISCRIMINATE_STR.invoke(output.generator());
    }
  },
  DISC_G_Z("disc.g.z", false, G, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__DISCRIMINATE_BOOL.invoke(output.generator());
    }
  },

  DIV_F("div.f", false, F, single(F), single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(GeneratorAdapter.DIV, DOUBLE_TYPE);
    }
  },

  DIV_I("div.i", false, I, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(GeneratorAdapter.DIV, LONG_TYPE);
    }
  },

  DROPL_EI("dropl.ei", false, E, single(E), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE__DROP_LAST.invoke(output.generator());
    }
  },

  DROP_ED("drop.ed", false, E, single(E), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE__DROP_WHILE.invoke(output.generator());
    }
  },

  DROP_EI("drop.ei", false, E, single(E), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE__DROP.invoke(output.generator());
    }
  },

  DROP_X("drop.x", false, X, single(S)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      ATTRIBUTE__DROP.invoke(output.generator());
    }
  },
  DURATION_F("duration.f", false, W, single(D), single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_WINDOW__DURATION_FLOAT.invoke(output.generator());
    }
  },
  DURATION_I("duration.i", false, W, single(D), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_WINDOW__DURATION_INT.invoke(output.generator());
    }
  },
  ETOA_AO("etoa.ao", true, A, single(E), single(A), single(M)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadFuture();
      output.loadSource();
      values.accept(1);
      values.accept(2);
      loadCallback.run();
      FRICASSEE__REDUCE.invoke(output.generator());
    }
  },

  ETOA_D("etoa.d", true, A, single(E), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadFuture();
      output.loadSource();
      values.accept(1);
      loadCallback.run();
      FRICASSEE__SINGLE_OR_NULL.invoke(output.generator());
    }
  },

  ETOA_DD("etoa.dd", true, A, single(E), single(D), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadFuture();
      output.loadSource();
      values.accept(1);
      values.accept(2);
      loadCallback.run();
      FRICASSEE__SINGLE.invoke(output.generator());
    }
  },

  ETOD("etod", false, D, single(E), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadSource();
      values.accept(1);
      FRICASSEE__YIELD.invoke(output.generator());
    }
  },

  ETOD_A("etod.a", false, D, single(E), single(D), single(A)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadSource();
      values.accept(1);
      values.accept(2);
      FRICASSEE__YIELD_DEFAULT.invoke(output.generator());
    }
  },

  ETOE_G("etoe.g", false, E, single(E), arrayOf(G)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE__GROUP_BY.invoke(output.generator());
    }
  },

  ETOE_M("etoe.m", false, E, single(E), single(A), single(M)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      values.accept(2);
      FRICASSEE__ACCUMULATE.invoke(output.generator());
    }
  },

  ETOE_U("etoe.u", false, E, single(E), single(U)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE__FLATTEN.invoke(output.generator());
    }
  },
  ETOI("etoi", true, I, single(E)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadFuture();
      output.loadSource();
      loadCallback.run();
      FRICASSEE__COUNT.invoke(output.generator());
    }
  },

  ETOR_AO("etor.ao", true, R, single(E), single(A), single(O)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadFuture();
      output.loadSource();
      values.accept(1);
      values.accept(2);
      loadCallback.run();
      FRICASSEE__SCAN.invoke(output.generator());
    }
  },

  ETOR_I("etor.i", true, R, single(E), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadFuture();
      output.loadSource();
      values.accept(1);
      loadCallback.run();
      FRICASSEE__TO_LIST.invoke(output.generator());
    }
  },

  ETOR_S("etor.s", true, R, single(E), single(D), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadFuture();
      output.loadSource();
      values.accept(1);
      values.accept(2);
      loadCallback.run();
      FRICASSEE__TO_FRAME.invoke(output.generator());
    }
  },

  FILT_E("filt.e", false, E, single(E), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE__WHERE.invoke(output.generator());
    }
  },
  FTOA("ftoa", false, A, single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      ANY__OF_FLOAT.invoke(output.generator());
    }
  },
  FTOI("ftoi", false, I, single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      STR__FROM_INT.invoke(output.generator());
    }
  },

  FTOS("ftos", false, S, single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      STR__FROM_FLOAT.invoke(output.generator());
    }
  },

  GATHER_I("gather.i", true, R, single(R), single(S)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadFuture();
      output.loadSource();
      values.accept(1);
      loadCallback.run();
      FRAME__GATHER_I.invoke(output.generator());
    }
  },
  GATHER_S("gather.s", true, R, single(R), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadFuture();
      output.loadSource();
      values.accept(1);
      loadCallback.run();
      FRAME__GATHER_S.invoke(output.generator());
    }
  },
  ID("id", false, S, single(R)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRAME__ID.invoke(output.generator());
    }
  },

  IS_FINITE("isfinite", false, Z, single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      DOUBLE__IS_FINITE.invoke(output.generator());
    }
  },

  IS_NAN("isnan", false, Z, single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      DOUBLE__IS_NAN.invoke(output.generator());
    }
  },
  ITOA("itoa", false, A, single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      ANY__OF_INT.invoke(output.generator());
    }
  },

  ITOF("itof", false, F, single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);

      output.generator().cast(LONG_TYPE, DOUBLE_TYPE);
    }
  },

  ITOS("itos", false, S, single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      STR__FROM_BOOL.invoke(output.generator());
    }
  },

  ITOZ("itoz", false, Z, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      final var end = output.generator().newLabel();

      final var truePath = output.generator().newLabel();

      output.generator().ifCmp(LONG_TYPE, GeneratorAdapter.EQ, truePath);

      output.generator().push(false);

      output.generator().goTo(end);

      output.generator().mark(truePath);

      output.generator().push(true);

      output.generator().mark(end);
    }
  },

  KTOL("ktol", false, L, single(S), single(C), single(K)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      values.accept(2);
      LOOKUP_HANDLER__FRICASSEE.invoke(output.generator());
    }
  },

  LEN_B("len.b", false, I, single(B)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);

      output.generator().arrayLength();

      output.generator().cast(INT_TYPE, LONG_TYPE);
    }
  },

  LEN_S("len.s", false, I, single(S)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      STR__LENGTH.invoke(output.generator());
    }
  },

  LET_E("let.e", false, E, single(E), arrayOf(X)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE__LET.invoke(output.generator());
    }
  },

  LOOKUP(
      "lookup",
      true,
      A,
      single(C),
      fold(S, virtualMethod(NAME_SOURCE_TYPE, NAME_SOURCE_TYPE, "add", JSTRING_TYPE))) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {

      output.generator().getStatic(NAME_SOURCE_TYPE, "EMPTY", NAME_SOURCE_TYPE);
      values.accept(1);
      output.loadFuture();
      output.loadSource();
      values.accept(0);
      CONTEXTUAL.load(output.generator());
      loadCallback.run();
      NAME_SOURCE__COLLECT.invoke(output.generator());
    }
  },

  LOOKUP_L("lookup.l", true, A, single(L), single(C), single(N)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(2);
      output.loadFuture();
      output.loadSource();
      values.accept(1);
      values.accept(0);
      loadCallback.run();
      NAME_SOURCE__COLLECT.invoke(output.generator());
    }
  },

  LTOA("lota", false, A, single(L)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      ANY__OF_LOOKUP_HANDLER.invoke(output.generator());
    }
  },

  MOD_F("mod.f", false, F, single(F), single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(GeneratorAdapter.REM, DOUBLE_TYPE);
    }
  },

  MOD_I("mod.i", false, I, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(GeneratorAdapter.REM, LONG_TYPE);
    }
  },

  MTOE("mtoe", false, E, single(C), single(A), single(M)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      output.loadLocation();
      output.loadSource();
      values.accept(0);
      values.accept(1);
      values.accept(2);
      FRICASSEE__GENERATE.invoke(output.generator());
    }
  },

  MUL_F("mul.f", false, F, single(F), single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(GeneratorAdapter.MUL, DOUBLE_TYPE);
    }
  },

  MUL_I("mul.i", false, I, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(GeneratorAdapter.MUL, LONG_TYPE);
    }
  },

  NEG_F("neg.f", false, F, single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);

      output.generator().math(GeneratorAdapter.NEG, DOUBLE_TYPE);
    }
  },

  NEG_I("neg.i", false, I, single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);

      output.generator().math(GeneratorAdapter.NEG, LONG_TYPE);
    }
  },

  NEW_G("new.g", false, G, single(S), single(K)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__COLLECT.invoke(output.generator());
    }
  },

  NEW_G_A("new.g.a", false, G, single(S), single(A)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__FIXED.invoke(output.generator());
    }
  },

  NEW_P("new.p", false, E, single(C), single(Z), arrayOf(P)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      values.accept(2);
      FRICASSEE__ZIP.invoke(output.generator());
    }
  },

  NEW_P_I("new.p.i", false, P, single(S)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_ZIPPER__ORDINAL.invoke(output.generator());
    }
  },

  NEW_P_R("new.p.r", false, P, single(S), single(R)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_ZIPPER__FRAME.invoke(output.generator());
    }
  },

  NEW_P_S("new.p.s", false, P, single(S)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_ZIPPER__NAME.invoke(output.generator());
    }
  },

  NEW_R("new.r", false, R, single(Z), single(C), NAMES, BUILDERS) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadFuture();
      output.loadSource();
      values.accept(3);
      values.accept(1);
      output.generator().swap();
      values.accept(2);
      output.generator().swap();
      FRAME__CREATE.invoke(output.generator());
    }
  },

  NEW_R_I("new.r.i", false, R, single(C), single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      output.loadFuture();
      output.loadSource();
      values.accept(1);
      values.accept(2);
      values.accept(0);
      FRAME__THROUGH.invoke(output.generator());
    }
  },

  NEW_T("new.t", false, T, single(C), NAMES, BUILDERS) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      output.loadSource();
      values.accept(2);
      values.accept(0);
      output.generator().swap();
      values.accept(1);
      output.generator().swap();
      TEMPLATE__CTOR.invoke(output.generator());
    }
  },

  NEW_X_IA("new.x.ia", false, X, single(I), single(A)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      ATTRIBUTE__OF_ANY_ORDINAL.invoke(output.generator());
    }
  },
  NEW_X_SA("new.x.sa", false, X, single(S), single(A)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      ATTRIBUTE__OF_ANY_NAME.invoke(output.generator());
    }
  },

  NEW_X_SD("new.x.sd", false, X, single(S), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      ATTRIBUTE__OF_DEFINITION.invoke(output.generator());
    }
  },

  NEW_X_SO("new.x.so", false, X, single(S), single(O)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      ATTRIBUTE__OF_OVERRIDE.invoke(output.generator());
    }
  },

  NOT_G("not.g", false, G, single(G)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_GROUPER__INVERT.invoke(output.generator());
    }
  },

  NOT_I("not.i", false, I, single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);

      output.generator().visitInsn(Opcodes.LCONST_1);

      output.generator().visitInsn(Opcodes.LXOR);
    }
  },

  NOT_Z("not.z", false, Z, single(Z)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);

      output.generator().not();
    }
  },

  ORD_E_F("ord.e.f", false, E, single(E), single(Z), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      values.accept(2);
      FRICASSEE_GROUPER__ORDER_BY_FLOAT.invoke(output.generator());
    }
  },

  ORD_E_I("ord.e.i", false, E, single(E), single(Z), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      values.accept(2);
      FRICASSEE_GROUPER__ORDER_BY_INT.invoke(output.generator());
    }
  },

  ORD_E_S("ord.e.s", false, E, single(E), single(Z), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      values.accept(2);
      FRICASSEE_GROUPER__ORDER_BY_STR.invoke(output.generator());
    }
  },

  ORD_E_Z("ord.e.z", false, E, single(E), single(Z), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      values.accept(2);
      FRICASSEE_GROUPER__ORDER_BY_BOOL.invoke(output.generator());
    }
  },

  OR_G("or.g", false, G, arrayOf(G)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_GROUPER__ALTERNATE.invoke(output.generator());
    }
  },

  OR_I("or.i", false, I, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(Opcodes.IOR, LONG_TYPE);
    }
  },

  POWERSET("powerset", false, G, arrayOf(G)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_GROUPER__POWERSET.invoke(output.generator());
    }
  },
  PRIV_CR("priv.cr", false, C, single(C), single(R)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      CONTEXT__OF_FRAME.invoke(output.generator());
    }
  },
  PRIV_X("priv.x", false, C, single(C)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      ATTRIBUTE__REDUCE_VISIBILITY.invoke(output.generator());
    }
  },

  REQUIRE_X("require.x", false, X, single(S)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      ATTRIBUTE__REQUIRE.invoke(output.generator());
    }
  },

  REV_E("rev.e", false, E, single(E)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE__REVERSE.invoke(output.generator());
    }
  },
  RING_G("ring.g", false, G, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__RING_EXPONENT.invoke(output.generator());
    }
  },

  RTOA("rtoa", false, A, single(R)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      ANY__OF_FRAME.invoke(output.generator());
    }
  },

  RTOE("rtoe", false, E, single(R), single(C)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      output.loadLocation();
      output.loadSource();
      FRICASSEE__FOR_EACH.invoke(output.generator());
    }
  },

  RTOX("rtox", false, X, single(R)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRAME__BUILDER.invoke(output.generator());
    }
  },

  SEAL_D("seal.d", false, D, single(D), single(C)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadLocation();
      output.loadSource();
      values.accept(1);
      DEFINITION__SEAL.invoke(output.generator());
    }
  },
  SEAL_O("seal.o", false, O, single(O), single(C)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      output.loadLocation();
      output.loadSource();
      values.accept(1);
      OVERRIDE_DEFINITION__SEAL.invoke(output.generator());
    }
  },
  SESSION_F("session.f", false, W, single(D), single(F), single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      values.accept(2);
      FRICASSEE_WINDOW__SESSION_FLOAT.invoke(output.generator());
    }
  },
  SESSION_I("session.i", false, W, single(D), single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      values.accept(2);
      FRICASSEE_WINDOW__SESSION_INT.invoke(output.generator());
    }
  },
  SHUF_E("shuf.e", false, E, single(E)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE__SHUFFLE.invoke(output.generator());
    }
  },

  SH_I("sh.i", false, I, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      output.generator().dup();

      // Determine if out shift is more than -64
      output.generator().push(-64L);
      output.generator().visitInsn(Opcodes.LCMP);
      final var end = output.generator().newLabel();
      var next = output.generator().newLabel();
      output.generator().ifZCmp(GeneratorAdapter.GE, next);
      // If so, only the sign matters, so drop the shift and compare the main value to 0L
      output.generator().pop();
      output.generator().push(0L);
      output.generator().visitInsn(Opcodes.LCMP);
      // Drop the lowest bit so -1 is preserved and 0 and 1 become 0
      output.generator().math(GeneratorAdapter.SHR, INT_TYPE);
      output.generator().cast(INT_TYPE, LONG_TYPE);
      output.generator().goTo(end);
      output.generator().mark(next);
      next = output.generator().newLabel();
      // Okay, check compared to zero and deal with the -64 through -1 case
      output.generator().push(0L);
      output.generator().visitInsn(Opcodes.LCMP);
      output.generator().ifZCmp(GeneratorAdapter.GE, next);
      // Convert the offset to integer, negate and shift
      output.generator().cast(LONG_TYPE, INT_TYPE);
      output.generator().math(GeneratorAdapter.NEG, INT_TYPE);
      output.generator().math(GeneratorAdapter.SHR, LONG_TYPE);
      output.generator().goTo(end);
      output.generator().mark(next);
      next = output.generator().newLabel();
      // Now see if we are in the 0 through 63
      output.generator().push(64L);
      output.generator().visitInsn(Opcodes.LCMP);
      output.generator().ifZCmp(GeneratorAdapter.GE, next);
      // Just shift left
      output.generator().cast(LONG_TYPE, INT_TYPE);
      output.generator().math(GeneratorAdapter.SHL, LONG_TYPE);
      output.generator().goTo(end);
      output.generator().mark(next);
      // Final case, > 64
      output.generator().pop();
      output.generator().pop();
      output.generator().push(0L);
      output.generator().mark(end);
    }
  },

  STOA("stoa", false, A, single(S)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      ANY__OF_STR.invoke(output.generator());
    }
  },

  STRIPE_E("stripe.e", false, G, single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      FRICASSEE_GROUPER__STRIPE.invoke(output.generator());
    }
  },
  WINDOW_G("window.g", false, G, single(W), single(W)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE_GROUPER__WINDOWED.invoke(output.generator());
    }
  },

  SUB_F("sub.f", false, F, single(F), single(F)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(Opcodes.ISUB, DOUBLE_TYPE);
    }
  },

  SUB_I("sub.i", false, I, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(Opcodes.ISUB, LONG_TYPE);
    }
  },

  TAKEL_EI("takel.ei", false, E, single(E), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE__TAKE_LAST.invoke(output.generator());
    }
  },

  TAKE_ED("take.ed", false, E, single(E), single(D)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE__TAKE_WHILE.invoke(output.generator());
    }
  },

  TAKE_EI("take.ei", false, E, single(E), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);
      FRICASSEE__TAKE.invoke(output.generator());
    }
  },

  TTOA("ttoa", false, A, single(T)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      ANY__OF_TEMPLATE.invoke(output.generator());
    }
  },

  XOR_I("xor.i", false, I, single(I), single(I)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      values.accept(1);

      output.generator().math(GeneratorAdapter.XOR, LONG_TYPE);
    }
  },
  ZTOA("ztoa", false, A, single(Z)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      ANY__OF_BOOL.invoke(output.generator());
    }
  },
  ZTOS("ztos", false, S, single(Z)) {
    @Override
    public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
      values.accept(0);
      STR__FROM_BOOL.invoke(output.generator());
    }
  };

  private static final Invokable ANY__OF_BIN =
      Invokable.staticMethod(ANY_TYPE, ANY_TYPE, "of", BIN_TYPE);
  private static final Invokable ANY__OF_BOOL =
      Invokable.staticMethod(ANY_TYPE, ANY_TYPE, "of", BOOLEAN_TYPE);
  private static final Invokable ANY__OF_FLOAT =
      Invokable.staticMethod(ANY_TYPE, ANY_TYPE, "of", DOUBLE_TYPE);
  private static final Invokable ANY__OF_FRAME =
      Invokable.staticMethod(ANY_TYPE, ANY_TYPE, "of", FRAME_TYPE);
  private static final Invokable ANY__OF_INT =
      Invokable.staticMethod(ANY_TYPE, ANY_TYPE, "of", LONG_TYPE);
  private static final Invokable ANY__OF_LOOKUP_HANDLER =
      Invokable.staticMethod(ANY_TYPE, ANY_TYPE, "of", LOOKUP_HANDLER_TYPE);
  private static final Invokable ANY__OF_STR =
      Invokable.staticMethod(ANY_TYPE, ANY_TYPE, "of", STR_TYPE);
  private static final Invokable ANY__OF_TEMPLATE =
      Invokable.staticMethod(ANY_TYPE, ANY_TYPE, "of", TEMPLATE_TYPE);
  private static final Invokable ANY__TO_STR =
      virtualMethod(
          ANY_TYPE, VOID_TYPE, "toStr", FUTURE_TYPE, SOURCE_REFERENCE_TYPE, CONSUMER_TYPE);

  private static final Invokable ATTRIBUTE__DROP =
      Invokable.staticMethod(ATTRIBUTE_TYPE, ATTRIBUTE_TYPE, "drop", STR_TYPE);
  private static final Invokable ATTRIBUTE__OF_ANY_NAME =
      Invokable.staticMethod(ATTRIBUTE_TYPE, ATTRIBUTE_TYPE, "of", STR_TYPE, ANY_TYPE);
  private static final Invokable ATTRIBUTE__OF_ANY_ORDINAL =
      Invokable.staticMethod(ATTRIBUTE_TYPE, ATTRIBUTE_TYPE, "of", LONG_TYPE, ANY_TYPE);
  private static final Invokable ATTRIBUTE__OF_DEFINITION =
      Invokable.staticMethod(ATTRIBUTE_TYPE, ATTRIBUTE_TYPE, "of", STR_TYPE, DEFINITION_TYPE);
  private static final Invokable ATTRIBUTE__OF_OVERRIDE =
      Invokable.staticMethod(
          ATTRIBUTE_TYPE, ATTRIBUTE_TYPE, "of", STR_TYPE, OVERRIDE_DEFINITION_TYPE);
  private static final Invokable ATTRIBUTE__REDUCE_VISIBILITY =
      Invokable.virtualMethod(ATTRIBUTE_TYPE, ATTRIBUTE_TYPE, "reduceVisibility");
  private static final Invokable ATTRIBUTE__REQUIRE =
      Invokable.staticMethod(ATTRIBUTE_TYPE, ATTRIBUTE_TYPE, "require", STR_TYPE);

  private static final Invokable COLLECTOR_DEFINITION__BIND =
      staticInterfaceMethod(
          COLLECTOR_DEFINITION_TYPE,
          DEFINITION_TYPE,
          "bind",
          COLLECTOR_DEFINITION_TYPE,
          FRICASSEE_TYPE);
  private static final Invokable CONTEXT__FOR_FRAME =
      virtualMethod(CONTEXT_TYPE, CONTEXT_TYPE, "forFrame", FRAME_TYPE);
  private static final Invokable CONTEXT__OF_FRAME =
      virtualMethod(CONTEXT_TYPE, CONTEXT_TYPE, "ofFrame", FRAME_TYPE);
  private static final Invokable CONTEXT__PREPEND =
      virtualMethod(CONTEXT_TYPE, CONTEXT_TYPE, "prepend", FRAME_TYPE);
  private static final Invokable CONTEXT__SELF = virtualMethod(CONTEXT_TYPE, FRAME_TYPE, "self");
  static final Method DEFAULT_CTOR = new Method("<init>", VOID_TYPE, new Type[] {});
  private static final Invokable DEFINITION__SEAL =
      Invokable.staticInterfaceMethod(
          DEFINITION_TYPE,
          DEFINITION_TYPE,
          "seal",
          DEFINITION_TYPE,
          JSTRING_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          SOURCE_REFERENCE_TYPE,
          CONTEXT_TYPE);
  private static final Invokable DOUBLE__IS_FINITE =
      Invokable.staticMethod(JDOUBLE_TYPE, BOOLEAN_TYPE, "isFinite", DOUBLE_TYPE);
  private static final Invokable DOUBLE__IS_NAN =
      Invokable.staticMethod(JDOUBLE_TYPE, BOOLEAN_TYPE, "isNaN", DOUBLE_TYPE);
  private static final Invokable FRAME__APPEND =
      virtualMethod(
          FRAME_TYPE,
          FRAME_TYPE,
          "append",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          CONTEXT_TYPE,
          FRAME_TYPE);
  private static final Invokable FRAME__BUILDER =
      virtualMethod(FRAME_TYPE, ATTRIBUTE_SOURCE_TYPE, "builder");
  private static final Invokable FRAME__CONTAINER =
      virtualMethod(FRAME_TYPE, FRAME_TYPE, "container");
  private static final Invokable FRAME__CREATE =
      Invokable.staticMethod(
          FRAME_TYPE,
          FRAME_TYPE,
          "create",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          CONTEXT_TYPE,
          BOOLEAN_TYPE,
          NAME_ARRAY_TYPE,
          ATTRIBUTE_SOURCE_ARRAY_TYPE);
  private static final Invokable FRAME__GATHER_I =
      virtualMethod(
          FRAME_TYPE,
          VOID_TYPE,
          "gather",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          LONG_TYPE,
          CONSUMER_TYPE);
  private static final Invokable FRAME__GATHER_S =
      virtualMethod(
          FRAME_TYPE,
          VOID_TYPE,
          "gather",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          STR_TYPE,
          CONSUMER_TYPE);
  private static final Invokable FRAME__ID = virtualMethod(FRAME_TYPE, STR_TYPE, "id");
  private static final Invokable FRAME__THROUGH =
      Invokable.staticMethod(
          FRAME_TYPE,
          FRAME_TYPE,
          "through",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          LONG_TYPE,
          LONG_TYPE,
          CONTEXT_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ADJACENT_BOOL =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "adjacentBool",
          STR_TYPE,
          DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ADJACENT_FLOAT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "adjacentFloat",
          STR_TYPE,
          DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ADJACENT_INT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "adjacentInt", STR_TYPE, DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ADJACENT_STR =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "adjacentStr", STR_TYPE, DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ALL =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "all", FRICASSEE_GROUPER_ARRAY_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ALTERNATE =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "alternate",
          FRICASSEE_GROUPER_ARRAY_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ALWAYS_INCLUDE_BOOL =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "alwaysIncludeBool",
          STR_TYPE,
          DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ALWAYS_INCLUDE_FLOAT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "alwaysIncludeFloat",
          STR_TYPE,
          DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ALWAYS_INCLUDE_INT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "alwaysIncludeInt",
          STR_TYPE,
          DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ALWAYS_INCLUDE_STR =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "alwaysIncludeStr",
          STR_TYPE,
          DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__BOUNDARY =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "boundary",
          DEFINITION_TYPE,
          BOOLEAN_TYPE);
  private static final Invokable FRICASSEE_GROUPER__BUCKETS_FLOAT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "bucketsFloat",
          DEFINITION_TYPE,
          LONG_TYPE);
  private static final Invokable FRICASSEE_GROUPER__BUCKETS_INT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "bucketsInt", DEFINITION_TYPE, LONG_TYPE);
  private static final Invokable FRICASSEE_GROUPER__BUCKETS_STR =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "bucketsStr", DEFINITION_TYPE, LONG_TYPE);
  private static final Invokable FRICASSEE_GROUPER__CHUNK =
      Invokable.staticMethod(FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "chunk", LONG_TYPE);
  private static final Invokable FRICASSEE_GROUPER__COLLECT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "collect",
          STR_TYPE,
          COLLECTOR_DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__CROSSTAB_BOOL =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "crosstabBool", DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__CROSSTAB_FLOAT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "crosstabFloat", DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__CROSSTAB_INT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "crosstabInt", DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__CROSSTAB_STR =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "crosstabStr", DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__DISCRIMINATE_BOOL =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "discriminateByBool",
          STR_TYPE,
          DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__DISCRIMINATE_FLOAT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "discriminateByFloat",
          STR_TYPE,
          DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__DISCRIMINATE_INT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "discriminateByInt",
          STR_TYPE,
          DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__DISCRIMINATE_STR =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "discriminateByStr",
          STR_TYPE,
          DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__FIXED =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "fixed", STR_TYPE, ANY_TYPE);
  private static final Invokable FRICASSEE_GROUPER__INVERT =
      virtualMethod(FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "invert");
  private static final Invokable FRICASSEE_GROUPER__ORDER_BY_BOOL =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "orderByBool", BOOLEAN_TYPE, DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ORDER_BY_FLOAT =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "orderByFloat", BOOLEAN_TYPE, DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ORDER_BY_INT =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "orderByInt", BOOLEAN_TYPE, DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__ORDER_BY_STR =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "orderByStr", BOOLEAN_TYPE, DEFINITION_TYPE);
  private static final Invokable FRICASSEE_GROUPER__POWERSET =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "powerset", FRICASSEE_GROUPER_ARRAY_TYPE);
  private static final Invokable FRICASSEE_GROUPER__RING_EXPONENT =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "ringExponent", LONG_TYPE, LONG_TYPE);
  private static final Invokable FRICASSEE_GROUPER__STRIPE =
      Invokable.staticMethod(FRICASSEE_GROUPER_TYPE, FRICASSEE_GROUPER_TYPE, "stripe", LONG_TYPE);
  private static final Invokable FRICASSEE_GROUPER__WINDOWED =
      Invokable.staticMethod(
          FRICASSEE_GROUPER_TYPE,
          FRICASSEE_GROUPER_TYPE,
          "windowed",
          FRICASSEE_WINDOW_TYPE,
          FRICASSEE_WINDOW_TYPE);
  private static final Invokable FRICASSEE_WINDOW__COUNT =
      Invokable.staticMethod(FRICASSEE_WINDOW_TYPE, FRICASSEE_WINDOW_TYPE, "count", LONG_TYPE);
  private static final Invokable FRICASSEE_WINDOW__DURATION_FLOAT =
      Invokable.staticMethod(
          FRICASSEE_WINDOW_TYPE, FRICASSEE_WINDOW_TYPE, "duration", DEFINITION_TYPE, DOUBLE_TYPE);
  private static final Invokable FRICASSEE_WINDOW__DURATION_INT =
      Invokable.staticMethod(
          FRICASSEE_WINDOW_TYPE, FRICASSEE_WINDOW_TYPE, "duration", DEFINITION_TYPE, LONG_TYPE);

  private static final Invokable FRICASSEE_WINDOW__SESSION_FLOAT =
      Invokable.staticMethod(
          FRICASSEE_WINDOW_TYPE,
          FRICASSEE_WINDOW_TYPE,
          "session",
          DEFINITION_TYPE,
          DOUBLE_TYPE,
          DOUBLE_TYPE);
  private static final Invokable FRICASSEE_WINDOW__SESSION_INT =
      Invokable.staticMethod(
          FRICASSEE_WINDOW_TYPE,
          FRICASSEE_WINDOW_TYPE,
          "session",
          DEFINITION_TYPE,
          LONG_TYPE,
          LONG_TYPE);
  private static final Invokable FRICASSEE_ZIPPER__FRAME =
      Invokable.staticMethod(
          FRICASSEE_ZIPPER_TYPE, FRICASSEE_ZIPPER_TYPE, "frame", STR_TYPE, FRAME_TYPE);
  private static final Invokable FRICASSEE_ZIPPER__NAME =
      Invokable.staticMethod(FRICASSEE_ZIPPER_TYPE, FRICASSEE_ZIPPER_TYPE, "name", STR_TYPE);
  private static final Invokable FRICASSEE_ZIPPER__ORDINAL =
      Invokable.staticMethod(FRICASSEE_ZIPPER_TYPE, FRICASSEE_ZIPPER_TYPE, "ordinal", STR_TYPE);
  private static final Invokable FRICASSEE__ACCUMULATE =
      virtualMethod(
          FRICASSEE_TYPE, FRICASSEE_TYPE, "accumulate", ANY_TYPE, ACCUMULATOR_DEFINITION_TYPE);
  private static final Invokable FRICASSEE__CONCAT =
      Invokable.staticMethod(
          FRICASSEE_TYPE, FRICASSEE_TYPE, "concat", CONTEXT_TYPE, FRICASSEE_ARRAY_TYPE);
  private static final Invokable FRICASSEE__COUNT =
      virtualMethod(
          FRICASSEE_TYPE,
          FRICASSEE_TYPE,
          "count",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          CONSUMER_TYPE);
  private static final Invokable FRICASSEE__DROP =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "drop", LONG_TYPE);
  private static final Invokable FRICASSEE__DROP_LAST =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "dropLast", LONG_TYPE);
  private static final Invokable FRICASSEE__DROP_WHILE =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "dropWhile", DEFINITION_TYPE);
  private static final Invokable FRICASSEE__FLATTEN =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "flatten", DISTRIBUTOR_DEFINITION_TYPE);
  private static final Invokable FRICASSEE__FOR_EACH =
      Invokable.staticMethod(
          FRICASSEE_TYPE,
          FRICASSEE_TYPE,
          "forEach",
          FRAME_TYPE,
          CONTEXT_TYPE,
          JSTRING_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          SOURCE_REFERENCE_TYPE);
  private static final Invokable FRICASSEE__GENERATE =
      Invokable.staticMethod(
          FRICASSEE_TYPE,
          FRICASSEE_TYPE,
          "generate",
          JSTRING_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          SOURCE_REFERENCE_TYPE,
          CONTEXT_TYPE,
          ANY_TYPE,
          ACCUMULATOR_DEFINITION_TYPE);
  private static final Invokable FRICASSEE__GROUP_BY =
      virtualMethod(
          FRICASSEE_TYPE,
          FRICASSEE_TYPE,
          "groupBy",
          SOURCE_REFERENCE_TYPE,
          Type.getType(FricasseeGrouper[].class));
  private static final Invokable FRICASSEE__LET =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "let", ATTRIBUTE_ARRAY_TYPE);
  private static final Invokable FRICASSEE__REDUCE =
      virtualMethod(
          FRICASSEE_TYPE,
          VOID_TYPE,
          "reduce",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          ANY_TYPE,
          OVERRIDE_DEFINITION_TYPE,
          CONSUMER_TYPE);
  private static final Invokable FRICASSEE__REVERSE =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "reverse");
  private static final Invokable FRICASSEE__SCAN =
      virtualMethod(
          FRICASSEE_TYPE,
          VOID_TYPE,
          "scan",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          ANY_TYPE,
          ACCUMULATOR_DEFINITION_TYPE,
          CONSUMER_TYPE);
  private static final Invokable FRICASSEE__SHUFFLE =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "shuffle");
  private static final Invokable FRICASSEE__SINGLE =
      virtualMethod(
          FRICASSEE_TYPE,
          VOID_TYPE,
          "single",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          DEFINITION_TYPE,
          DEFINITION_TYPE,
          CONSUMER_TYPE);
  private static final Invokable FRICASSEE__SINGLE_OR_NULL =
      virtualMethod(
          FRICASSEE_TYPE,
          VOID_TYPE,
          "singleOrNull",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          DEFINITION_TYPE,
          CONSUMER_TYPE);
  private static final Invokable FRICASSEE__TAKE =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "take", LONG_TYPE);
  private static final Invokable FRICASSEE__TAKE_LAST =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "takeLast", LONG_TYPE);
  private static final Invokable FRICASSEE__TAKE_WHILE =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "takeWhile", DEFINITION_TYPE);
  private static final Invokable FRICASSEE__TO_FRAME =
      virtualMethod(
          FRICASSEE_TYPE,
          VOID_TYPE,
          "toFrame",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          DEFINITION_TYPE,
          DEFINITION_TYPE,
          CONSUMER_TYPE);
  private static final Invokable FRICASSEE__TO_LIST =
      virtualMethod(
          FRICASSEE_TYPE,
          VOID_TYPE,
          "toList",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          DEFINITION_TYPE,
          CONSUMER_TYPE);
  private static final Invokable FRICASSEE__WHERE =
      virtualMethod(FRICASSEE_TYPE, FRICASSEE_TYPE, "where", DEFINITION_TYPE);
  private static final Invokable FRICASSEE__YIELD =
      virtualMethod(
          FRICASSEE_TYPE, DEFINITION_TYPE, "yield", SOURCE_REFERENCE_TYPE, DEFINITION_TYPE);
  private static final Invokable FRICASSEE__YIELD_DEFAULT =
      virtualMethod(
          FRICASSEE_TYPE,
          DEFINITION_TYPE,
          "yield",
          SOURCE_REFERENCE_TYPE,
          CONTEXT_TYPE,
          DEFINITION_TYPE,
          ANY_TYPE);
  private static final Invokable FRICASSEE__ZIP =
      Invokable.staticMethod(
          FRICASSEE_TYPE,
          FRICASSEE_TYPE,
          "zip",
          CONTEXT_TYPE,
          BOOLEAN_TYPE,
          JSTRING_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          SOURCE_REFERENCE_TYPE,
          Type.getType(FricasseeZipper[].class));
  private static final Invokable FUTURE__DEBUG_DEFINITION =
      virtualMethod(
          FUTURE_TYPE,
          VOID_TYPE,
          "debug",
          DEFINITION_TYPE,
          SOURCE_REFERENCE_TYPE,
          CONTEXT_TYPE,
          CONSUMER_TYPE);
  private static final Invokable FUTURE__LAUNCH_DEFINITION =
      virtualMethod(
          FUTURE_TYPE,
          VOID_TYPE,
          "launch",
          DEFINITION_TYPE,
          SOURCE_REFERENCE_TYPE,
          CONTEXT_TYPE,
          CONSUMER_TYPE);
  private static final Invokable FUTURE__LAUNCH_OVERRIDE =
      virtualMethod(
          FUTURE_TYPE,
          VOID_TYPE,
          "launch",
          OVERRIDE_DEFINITION_TYPE,
          SOURCE_REFERENCE_TYPE,
          CONTEXT_TYPE,
          ANY_TYPE,
          CONSUMER_TYPE);
  private static final Invokable LOOKUP_HANDLER__FRICASSEE =
      Invokable.staticMethod(
          LOOKUP_HANDLER_TYPE,
          LOOKUP_HANDLER_TYPE,
          "fricassee",
          STR_TYPE,
          CONTEXT_TYPE,
          COLLECTOR_DEFINITION_TYPE);
  private static final Invokable NAME_SOURCE__ADD_FRAME =
      virtualMethod(NAME_SOURCE_TYPE, NAME_SOURCE_TYPE, "add", FRAME_TYPE);
  private static final Invokable NAME_SOURCE__ADD_ORDINAL =
      virtualMethod(NAME_SOURCE_TYPE, NAME_SOURCE_TYPE, "add", LONG_TYPE);
  private static final Invokable NAME_SOURCE__ADD_STR =
      virtualMethod(NAME_SOURCE_TYPE, NAME_SOURCE_TYPE, "add", STR_TYPE);
  private static final Invokable NAME_SOURCE__ADD_TYPE_OF =
      virtualMethod(NAME_SOURCE_TYPE, NAME_SOURCE_TYPE, "add", ANY_TYPE);
  private static final Invokable NAME_SOURCE__COLLECT =
      virtualMethod(
          NAME_SOURCE_TYPE,
          VOID_TYPE,
          "lookup",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          CONTEXT_TYPE,
          LOOKUP_HANDLER_TYPE,
          CONSUMER_TYPE);
  private static final Invokable OVERRIDE_DEFINITION__SEAL =
      Invokable.staticInterfaceMethod(
          OVERRIDE_DEFINITION_TYPE,
          OVERRIDE_DEFINITION_TYPE,
          "seal",
          OVERRIDE_DEFINITION_TYPE,
          JSTRING_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          SOURCE_REFERENCE_TYPE,
          CONTEXT_TYPE);
  private static final Invokable STR__COMPARE =
      virtualMethod(STR_TYPE, INT_TYPE, "compareTo", STR_TYPE);
  private static final Invokable STR__CONCAT =
      virtualMethod(STR_TYPE, STR_TYPE, "concat", STR_TYPE);
  private static final Invokable STR__FROM_BOOL =
      Invokable.staticMethod(STR_TYPE, STR_TYPE, "from", BOOLEAN_TYPE);
  private static final Invokable STR__FROM_FLOAT =
      Invokable.staticMethod(STR_TYPE, STR_TYPE, "from", DOUBLE_TYPE);
  private static final Invokable STR__FROM_INT =
      Invokable.staticMethod(STR_TYPE, STR_TYPE, "from", LONG_TYPE);
  private static final Invokable STR__LENGTH =
      Invokable.staticMethod(STR_TYPE, LONG_TYPE, "length");
  private static final Invokable TEMPLATE__CTOR =
      Invokable.constructMethod(
          TEMPLATE_TYPE,
          SOURCE_REFERENCE_TYPE,
          CONTEXT_TYPE,
          NAME_ARRAY_TYPE,
          ATTRIBUTE_ARRAY_TYPE);
  private final boolean callback;
  private final String opcode;
  private final InputParameterType[] parameters;
  private final KwsType returnType;

  SimpleOpcode(
      String opcode, boolean callback, KwsType returnType, InputParameterType... parameters) {
    this.opcode = opcode;

    this.callback = callback;
    this.returnType = returnType;
    this.parameters = parameters;
  }

  @Override
  public final boolean isCallback() {
    return callback;
  }

  @Override
  public String opcode() {
    return opcode;
  }

  @Override
  public final InputParameterType parameter(int i) {
    return parameters[i];
  }

  @Override
  public final int parameters() {
    return parameters.length;
  }

  @Override
  public final KwsType returnType() {
    return returnType;
  }
}
