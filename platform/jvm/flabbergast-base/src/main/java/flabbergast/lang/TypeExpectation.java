package flabbergast.lang;

/**
 * Describe an expected type when performing a conversion from a Flabbergast value into a Java one
 */
abstract class TypeExpectation {
  /** Expect a <tt>Bin</tt> type */
  public static final TypeExpectation BIN =
      new TypeExpectation() {
        @Override
        public String toString() {
          return "Bin";
        }
      };
  /** Expect a <tt>Bool</tt> type */
  public static final TypeExpectation BOOL =
      new TypeExpectation() {
        @Override
        public String toString() {
          return "Bool";
        }
      };
  /** Expect a <tt>Float</tt> type */
  public static final TypeExpectation FLOAT =
      new TypeExpectation() {
        @Override
        public String toString() {
          return "Float";
        }
      };
  /**
   * Expect a <tt>Frame</tt> type
   *
   * <p>This frame has no special restrictions.
   *
   * @see #frame()
   */
  public static final TypeExpectation FRAME =
      new TypeExpectation() {
        @Override
        public String toString() {
          return "Frame";
        }
      };
  /** Expect a <tt>Int</tt> type */
  public static final TypeExpectation INT =
      new TypeExpectation() {
        @Override
        public String toString() {
          return "Int";
        }
      };
  /** Expect a <tt>LookupHandler</tt> type */
  public static final TypeExpectation LOOKUP_HANDLER =
      new TypeExpectation() {
        @Override
        public String toString() {
          return "LookupHandler";
        }
      };
  /** Expect a <tt>Null</tt> type */
  public static final TypeExpectation NULL =
      new TypeExpectation() {
        @Override
        public String toString() {
          return "Null";
        }
      };
  /** Expect a <tt>Str</tt> type */
  public static final TypeExpectation STR =
      new TypeExpectation() {
        @Override
        public String toString() {
          return "Str";
        }
      };
  /** Expect a <tt>Str</tt> type with exactly one character */
  public static final TypeExpectation STR_1CHAR =
      new TypeExpectation() {
        @Override
        public String toString() {
          return "Str (1 character)";
        }
      };
  /** Expect a <tt>Template</tt> type */
  public static final TypeExpectation TEMPLATE =
      new TypeExpectation() {
        @Override
        public String toString() {
          return "Template";
        }
      };
  /**
   * Expect a <tt>Frame</tt> type with custom restrictions
   *
   * <p>The result of the operation will describe a normal <tt>Frame</tt> type, but additional
   * restrictions can be described to the user.
   *
   * @see FrameTypeExpectation
   */
  static FrameTypeExpectation frame() {
    return FrameTypeExpectation.START;
  }

  TypeExpectation() {}
}
