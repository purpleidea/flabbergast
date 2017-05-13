package flabbergast.compiler.kws;

import flabbergast.lang.*;

/** All the types available to KWS bytecodes */
public enum KwsType {
  /** The boxed any type */
  A("any", Any.class),
  /** The binary blob type (<tt>Bin</tt>) */
  B("bin", byte[].class),
  /** The lookup context type */
  C("context", Context.class),
  /** The standard attribute definition type */
  D("definition", Definition.class),
  /** The fricassée operation type */
  E("fricassée", Fricassee.class),
  /** The fricassée grouper type */
  G("fricassée grouper", FricasseeGrouper.class),
  /** The fricassée zipper type */
  P("fricassée zipper", FricasseeZipper.class),
  /** The floating point number type (<tt>Float</tt>) */
  F("float", double.class),
  /** The integer type (<tt>Int</tt>) */
  I("int", long.class),
  /** The fricassée collector type */
  K("collector", CollectorDefinition.class),
  /** The lookup handler type (<tt>LookupHandler</tt>) */
  L("lookup handler", LookupHandler.class),
  /** The fricassée accumulator type */
  M("accumulator", AccumulatorDefinition.class),
  /** The lookup name type */
  N("name", NameSource.class),
  /** The override attibute type */
  O("override", OverrideDefinition.class),
  /** The frame type (<tt>Frame</tt>) */
  R("frame", Frame.class),
  /** The string type (<tt>Str</tt>) */
  S("str", Str.class),
  /** The template type (<tt>Template</tt>) */
  T("template", Template.class),
  /** The fricassée distributor type */
  U("distributor", DistributorDefinition.class),
  /** The fricassée window type */
  W("window", FricasseeWindow.class),
  /** The attribute definition type */
  X("attribute builder", AttributeSource.class),
  /** The Boolean type (<tt>Bool</tt>) */
  Z("boolean", boolean.class);
  private final Class<?> clazz;
  private final String description;

  KwsType(String description, Class<?> clazz) {

    this.description = description;
    this.clazz = clazz;
  }

  /** A human-friendly name for the type */
  public String description() {
    return description;
  }

  /** The JVM type used to represent this object */
  public Class<?> javaClass() {
    return clazz;
  }
}
