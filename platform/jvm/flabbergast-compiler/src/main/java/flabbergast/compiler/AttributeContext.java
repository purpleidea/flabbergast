package flabbergast.compiler;

enum AttributeContext {
  FILE("top-level of file"),
  TEMPLATE_CREATE("createFromValues new template"),
  TEMPLATE_AMEND("amend template"),
  FRAME_CREATE("literal frame"),
  FRAME_INSTANTIATE("instantiate from from template"),
  FUNCTION_ARGUMENT("argument to function-like template"),
  FUNCTION_PARAMETER("parameter of function-like template");
  private final String description;

  public final String description() {
    return description;
  }

  AttributeContext(String description) {
    this.description = description;
  }
}
