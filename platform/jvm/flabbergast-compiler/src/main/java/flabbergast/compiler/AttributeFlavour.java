package flabbergast.compiler;

import java.util.EnumSet;
import java.util.Set;

enum AttributeFlavour {
  DEFINITION(
      "attribute declaration",
      "d",
      AttributeContext.TEMPLATE_CREATE,
      AttributeContext.TEMPLATE_AMEND,
      AttributeContext.FRAME_CREATE,
      AttributeContext.FRAME_INSTANTIATE,
      AttributeContext.FILE),
  DROP(
      "drop attribute declaration",
      "x",
      AttributeContext.TEMPLATE_AMEND,
      AttributeContext.FRAME_INSTANTIATE),
  NOW(
      "eagerly evaluated attribute declaration",
      "d",
      AttributeContext.TEMPLATE_CREATE,
      AttributeContext.TEMPLATE_AMEND,
      AttributeContext.FRAME_CREATE,
      AttributeContext.FRAME_INSTANTIATE,
      AttributeContext.FUNCTION_ARGUMENT,
      AttributeContext.FUNCTION_PARAMETER),
  OVERRIDE(
      "attribute override",
      "o",
      AttributeContext.TEMPLATE_AMEND,
      AttributeContext.FRAME_INSTANTIATE),
  REQUIRED(
      "required attribute", "r", AttributeContext.TEMPLATE_AMEND, AttributeContext.TEMPLATE_CREATE),
  USED("used attribute", "u", AttributeContext.TEMPLATE_AMEND, AttributeContext.TEMPLATE_CREATE);
  private final Set<AttributeContext> allowed;
  private final String description;

  private final String symbol;

  AttributeFlavour(
      String description, String symbol, AttributeContext allowed, AttributeContext... allowed1) {
    this.description = description;
    this.symbol = symbol;
    this.allowed = EnumSet.of(allowed, allowed1);
  }

  public boolean allowed(AttributeContext context) {
    return allowed.contains(context);
  }

  public String description() {
    return description;
  }

  public String symbol() {
    return symbol;
  }
}
