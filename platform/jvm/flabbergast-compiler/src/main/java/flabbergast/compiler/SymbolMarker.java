package flabbergast.compiler;

/** Code highlighting format */
public enum SymbolMarker {
  CHARACTER("Character"),
  CLASS_DEF("ClassDef"),
  CLASS("Class"),
  COMMENT_BLOCK("CommentBlock"),
  COMMENT_LINE_DASH("CommentLineDash"),
  COMMENT_LINE_HASH("CommentLineHash"),
  COMMENT_LINE_PERCENT("CommentLinePercent"),
  COMMENT_LINE_SLASH("CommentLineSlash"),
  COMMENT_LINE("CommentLine"),
  COMMENT("Comment"),
  CONSTANT_DEF("ConstantDef"),
  CONSTANT_SYNTAX("ConstantSyntax"),
  CONSTANT("Constant"),
  CONSTRUCTOR_DEF("ConstructorDef"),
  CONSTRUCTOR("Constructor"),
  CONTROL_KEYWORD("ControlKeyword"),
  DEPRECATED("Deprecated"),
  DOCUMENTATION("Documentation"),
  ENUM_DEF("EnumDef"),
  ENUM_MEMBER_DEF("EnumMemberDef"),
  ENUM_MEMBER("EnumMember"),
  ENUM("Enum"),
  ESCAPE("Escape"),
  EVENT_DEF("EventDef"),
  EVENT("Event"),
  FIELD_DEF("FieldDef"),
  FIELD("Field"),
  FUNCTION_DEF("FunctionDef"),
  FUNCTION("Function"),
  INTERFACE_DEF("InterfaceDef"),
  INTERFACE("Interface"),
  KEYWORD("Keyword"),
  MARKUP_BOLD("MarkupBold"),
  MARKUP_BULLETS("MarkupBullets"),
  MARKUP_HEADING("MarkupHeading"),
  MARKUP_ITALIC("MarkupItalic"),
  MARKUP_LINK("MarkupLink"),
  MARKUP_NUMBERED("MarkupNumbered"),
  MARKUP_QUOTE("MarkupQuote"),
  MARKUP_RAW("MarkupRaw"),
  MARKUP_UNDERLINE("MarkupUnderline"),
  METHOD_DEF("MethodDef"),
  METHOD("Method"),
  MODULE_DEF("ModuleDef"),
  MODULE("Module"),
  NAMESPACE_DEF("NamespaceDef"),
  NAMESPACE("Namespace"),
  NUMBER("Number"),
  OPERATOR("Operator"),
  PARAMETER_DEF("ParameterDef"),
  PARAMETER("Parameter"),
  PROPERTY_DEF("PropertyDef"),
  PROPERTY("Property"),
  REGEXP("RegExp"),
  STORAGE_MODIFIER("StorageModifier"),
  STRING_DOUBLE_QUOTED("StringDoubleQuoted"),
  STRING_HERE_DOC("StringHereDoc"),
  STRING_INTERPOLATED("StringInterpolated"),
  STRING_QUOTED("StringQuoted"),
  STRING_SINGLE_QUOTED("StringSingleQuoted"),
  STRING_TRIPLE_QUOTED("StringTripleQuoted"),
  STRING("String"),
  STRUCT_DEF("StructDef"),
  STRUCT("Struct"),
  TYPE_DEF("TypeDef"),
  TYPE_PARAMETER_DEF("TypeParameterDef"),
  TYPE_PARAMETER("TypeParameter"),
  TYPE("Type"),
  VARIABLE_DEF("VariableDef"),
  VARIABLE("Variable");
  private final String definition;

  SymbolMarker(String definition) {

    this.definition = definition;
  }

  /**
   * The PIG keyword for this type
   *
   * @return
   */
  public String definition() {
    return definition;
  }
}
