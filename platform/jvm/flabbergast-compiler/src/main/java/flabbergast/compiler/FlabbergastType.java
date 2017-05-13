package flabbergast.compiler;

import flabbergast.compiler.kws.KwsType;

/** The types of values in the Flabbergast language */
public enum FlabbergastType {
  /** The binary blob type */
  BIN("Bin", "bin", KwsType.B),
  /** The Boolean type */
  BOOL("Bool", "bool", KwsType.Z),
  /** The floating-point number type */
  FLOAT("Float", "float", KwsType.F),
  /** The {@link flabbergast.lang.Frame} type */
  FRAME("Frame", "frame", KwsType.R),
  /** The integral number type */
  INT("Int", "int", KwsType.I),
  /** The {@link flabbergast.lang.LookupHandler} type */
  LOOKUP_HANDLER("LookupHandler", "lookup_handler", KwsType.L),
  /** The null type */
  NULL("Null", "null", null),
  /** The {@link flabbergast.lang.Str} type */
  STR("Str", "str", KwsType.S),
  /** The {@link flabbergast.lang.Template} type */
  TEMPLATE("Template", "template", KwsType.T);
  private final String attributeName;
  private final KwsType kwsType;

  private final String prettyName;

  FlabbergastType(String prettyName, String attributeName, KwsType kwsType) {
    this.prettyName = prettyName;
    this.attributeName = attributeName;
    this.kwsType = kwsType;
  }

  /** The attribute name that would be used if this type was used in a <tt>TypeOf</tt> lookup */
  public String attributeName() {
    return attributeName;
  }

  /** The KWS type equivalent to this type (or null for <tt>Null</tt>) */
  public KwsType kwsType() {
    return kwsType;
  }

  /** The name for this type as displayed to the user */
  public String prettyName() {
    return prettyName;
  }
}
