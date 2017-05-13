import flabbergast.interop.StandardUriService;
import flabbergast.lang.UriService;

/** Flabbergast standard library bindings */
module flabbergast.stdlib {
  provides UriService with
      StandardUriService;

  requires flabbergast.base;
  requires flabbergast.compiler;
  requires java.sql;
}
