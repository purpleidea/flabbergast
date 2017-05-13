package flabbergast.cli;

import flabbergast.lang.Scheduler;
import picocli.CommandLine;

/** Provides version information to picocli */
class VersionProvider implements CommandLine.IVersionProvider {
  /** Get version information to be displayed in help */
  @Override
  public String[] getVersion() {
    return new String[] {Scheduler.VERSION, Scheduler.BUILD};
  }
}
