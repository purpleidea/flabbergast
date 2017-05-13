module flabbergast.language.server {
  exports flabbergast.lsp;

  requires flabbergast.compiler;
  requires org.eclipse.lsp4j;
  requires org.eclipse.lsp4j.jsonrpc;
}
