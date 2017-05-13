package flabbergast.cli;

import picocli.CommandLine;

@CommandLine.Command(
    name = "flabbergast",
    description =
        "Evaluates a Flabbergast script and allows interactive command evaluation and debugging",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    subcommands = {MainPrinter.class, MainDocumenter.class})
public class Main implements Runnable {
  public static void main(String[] args) {
    new CommandLine(new Main()).execute(args);
  }

  @Override
  public void run() {}
}
