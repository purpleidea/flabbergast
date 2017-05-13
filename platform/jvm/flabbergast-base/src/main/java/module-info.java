import flabbergast.export.Library;
import flabbergast.export.LibraryLoader;
import flabbergast.lang.KwsService;
import flabbergast.lang.Scheduler;
import flabbergast.lang.UriService;
import jdk.dynalink.linker.GuardingDynamicLinkerExporter;

/** Runtime support library for the Flabbergast language */
module flabbergast.base {
  exports flabbergast.export;
  exports flabbergast.lang;
  exports flabbergast.util;

  uses UriService;
  uses LibraryLoader;
  uses Library;
  uses KwsService;

  provides GuardingDynamicLinkerExporter with
      Scheduler;

  requires java.sql;
  requires java.xml;
  requires jdk.dynalink;
}
