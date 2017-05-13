/** Flabbergast command-line interface */
module flabbergast.cli {
  exports flabbergast.cli;

  requires info.picocli;
  requires lanterna;
  requires flabbergast.base;
  requires flabbergast.compiler;
  requires org.fusesource.jansi;
  requires java.xml;
  requires jline;

  opens flabbergast.cli to
      info.picocli;
}
