import flabbergast.compiler.Program;
import flabbergast.export.LibraryLoader;

/** Flabbergast compiler, REPL, API document generator, and formatter */
module flabbergast.compiler {
  exports flabbergast.compiler;
  exports flabbergast.compiler.kws;

  provides LibraryLoader with
      Program;

  requires com.github.treesitter;
  requires flabbergast.base;
  requires java.xml;
  requires org.objectweb.asm;
  requires org.objectweb.asm.commons;
}
