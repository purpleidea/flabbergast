package flabbergast.compiler.kws.codegen;

import static org.objectweb.asm.Type.*;

import flabbergast.compiler.kws.KwsType;
import flabbergast.export.Library;
import flabbergast.lang.*;
import java.util.function.Consumer;
import org.objectweb.asm.Type;

final class LanguageType {
  public static final Type ANY_CONSUMER_TYPE = Type.getType(AnyConsumer.class);
  public static final Type WHINY_ANY_CONSUMER_TYPE = Type.getType(WhinyAnyConsumer.class);
  public static final Type ACCUMULATOR_DEFINITION_TYPE = Type.getType(AccumulatorDefinition.class);
  public static final Type ACCUMULATOR_TYPE = Type.getType(Accumulator.class);
  public static final Type ANY_FUNCTION_TYPE = Type.getType(AnyFunction.class);
  public static final Type ANY_TYPE = Type.getType(Any.class);
  public static final Type ATTRIBUTE_ARRAY_TYPE = Type.getType(Attribute[].class);
  public static final Type ATTRIBUTE_SOURCE_ARRAY_TYPE = Type.getType(AttributeSource[].class);
  public static final Type ATTRIBUTE_SOURCE_TYPE = Type.getType(AttributeSource.class);
  public static final Type ATTRIBUTE_TYPE = Type.getType(Attribute.class);
  public static final Type BIN_TYPE = Type.getType(byte[].class);
  public static final Type COLLECTOR_DEFINITION_TYPE = Type.getType(CollectorDefinition.class);
  public static final Type CONSUMER_TYPE = Type.getType(Consumer.class);
  public static final Type CONTEXT_TYPE = Type.getType(Context.class);
  public static final Type DEFINITION_TYPE = Type.getType(Definition.class);
  public static final Type DOUBLE_BI_CONSUMER_TYPE = Type.getType(DoubleBiConsumer.class);
  public static final Type FRAME_TYPE = Type.getType(Frame.class);
  public static final Type FRICASSEE_ARRAY_TYPE = Type.getType(Fricassee[].class);
  public static final Type DISTRIBUTOR_DEFINITION_TYPE = Type.getType(DistributorDefinition.class);
  public static final Type FRICASSEE_GROUPER_TYPE = Type.getType(FricasseeGrouper.class);
  public static final Type FRICASSEE_GROUPER_ARRAY_TYPE = Type.getType(FricasseeGrouper[].class);
  public static final Type FRICASSEE_TYPE = Type.getType(Fricassee.class);
  public static final Type FRICASSEE_ZIPPER_TYPE = Type.getType(FricasseeZipper.class);
  public static final Type FRICASSEE_WINDOW_TYPE = Type.getType(FricasseeWindow.class);
  public static final Type FUTURE_TYPE = Type.getType(Future.class);
  public static final Type JDOUBLE_TYPE = Type.getType(Double.class);
  public static final Type JLONG_TYPE = Type.getType(Long.class);
  public static final Type JOBJECT_TYPE = Type.getType(Object.class);
  public static final Type JSTRING_ARRAY_TYPE = Type.getType(String[].class);
  public static final Type JSTRING_TYPE = Type.getType(String.class);
  public static final Type LIBRARY_TYPE = Type.getType(Library.class);
  public static final Type LONG_BI_CONSUMER_TYPE = Type.getType(LongBiConsumer.class);
  public static final Type LOOKUP_HANDLER_TYPE = Type.getType(LookupHandler.class);
  public static final Type NAME_SOURCE_TYPE = Type.getType(NameSource.class);
  public static final Type NAME_TYPE = Type.getType(Name.class);
  public static final Type NAME_ARRAY_TYPE = Type.getType(Name[].class);
  public static final Type OVERRIDE_DEFINITION_TYPE = Type.getType(OverrideDefinition.class);
  public static final Type RUNNABLE_TYPE = Type.getType(Runnable.class);
  public static final Type SOURCE_REFERENCE_TYPE = Type.getType(SourceReference.class);
  public static final Type STR_TYPE = Type.getType(Str.class);
  public static final Type TEMPLATE_TYPE = Type.getType(Template.class);

  public static Type of(KwsType type) {
    switch (type) {
      case A:
        return ANY_TYPE;
      case B:
        return BIN_TYPE;
      case C:
        return CONTEXT_TYPE;
      case D:
        return DEFINITION_TYPE;
      case E:
        return FRICASSEE_TYPE;
      case G:
        return FRICASSEE_GROUPER_TYPE;
      case P:
        return FRICASSEE_ZIPPER_TYPE;
      case F:
        return DOUBLE_TYPE;
      case I:
        return LONG_TYPE;
      case K:
        return COLLECTOR_DEFINITION_TYPE;
      case L:
        return LOOKUP_HANDLER_TYPE;
      case M:
        return ACCUMULATOR_DEFINITION_TYPE;
      case N:
        return NAME_SOURCE_TYPE;
      case O:
        return OVERRIDE_DEFINITION_TYPE;
      case R:
        return FRAME_TYPE;
      case S:
        return STR_TYPE;
      case T:
        return TEMPLATE_TYPE;
      case U:
        return DISTRIBUTOR_DEFINITION_TYPE;
      case W:
        return FRICASSEE_WINDOW_TYPE;
      case X:
        return ATTRIBUTE_TYPE;
      case Z:
        return BOOLEAN_TYPE;
    }
    throw new UnsupportedOperationException();
  }

  private LanguageType() {}
}
