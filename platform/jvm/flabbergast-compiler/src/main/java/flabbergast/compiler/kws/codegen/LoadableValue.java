package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.KwsType.*;
import static flabbergast.compiler.kws.codegen.LanguageType.*;

import flabbergast.compiler.kws.KwsType;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

interface LoadableValue {
  LoadableValue CONTEXTUAL = field(LOOKUP_HANDLER_TYPE, "CONTEXTUAL", L);
  LoadableValue INF_F = field(JDOUBLE_TYPE, "POSITIVE_INFINITY", F);
  LoadableValue MAX_F = field(JDOUBLE_TYPE, "MAX_VALUE", F);
  LoadableValue MAX_I = field(JLONG_TYPE, "MAX_VALUE", I);
  LoadableValue MAX_Z =
      new LoadableValue() {

        @Override
        public void load(GeneratorAdapter method) {
          method.push(true);
        }

        @Override
        public KwsType type() {
          return Z;
        }
      };
  LoadableValue MIN_F = field(JDOUBLE_TYPE, "MIN_VALUE", F);
  LoadableValue MIN_I = field(JLONG_TYPE, "MIN_VALUE", I);
  LoadableValue MIN_Z =
      new LoadableValue() {

        @Override
        public void load(GeneratorAdapter method) {
          method.push(false);
        }

        @Override
        public KwsType type() {
          return Z;
        }
      };
  LoadableValue NAN_F = field(JDOUBLE_TYPE, "NaN", F);
  LoadableValue NIL_A = field(ANY_TYPE, "NULL", A);
  LoadableValue NIL_C = field(CONTEXT_TYPE, "EMPTY", C);
  LoadableValue NIL_N = field(NAME_SOURCE_TYPE, "EMPTY", N);
  LoadableValue NIL_R = field(FRAME_TYPE, "EMPTY", R);
  LoadableValue NIL_W = field(FRICASSEE_WINDOW_TYPE, "NON_OVERLAPPING", W);
  Invokable STR__FROM_STRING = Invokable.staticMethod(STR_TYPE, STR_TYPE, "from", JSTRING_TYPE);

  static LoadableValue field(Type owner, String name, KwsType type) {
    return new LoadableValue() {

      @Override
      public void load(GeneratorAdapter method) {
        method.getStatic(owner, name, LanguageType.of(type));
      }

      @Override
      public KwsType type() {
        return type;
      }
    };
  }

  static LoadableValue of(long value) {
    return new LoadableValue() {

      public void load(GeneratorAdapter method) {
        method.push(value);
      }

      public KwsType type() {
        return I;
      }
    };
  }

  static LoadableValue of(double value) {
    return new LoadableValue() {

      public void load(GeneratorAdapter method) {
        method.push(value);
      }

      public KwsType type() {
        return F;
      }
    };
  }

  static LoadableValue of(String value) {
    return new LoadableValue() {

      public void load(GeneratorAdapter method) {
        method.push(value);
        STR__FROM_STRING.invoke(method);
      }

      public KwsType type() {
        return S;
      }
    };
  }

  void load(GeneratorAdapter method);

  KwsType type();
}
