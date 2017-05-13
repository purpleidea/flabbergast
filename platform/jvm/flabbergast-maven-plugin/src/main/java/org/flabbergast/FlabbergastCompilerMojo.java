package org.flabbergast;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.LanguageGrammar;
import flabbergast.compiler.Program;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.kws.KwsRenderer;
import flabbergast.util.Pair;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/** Compile Flabbergast files */
@Mojo(
    name = "compile",
    defaultPhase = LifecyclePhase.COMPILE,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true)
public final class FlabbergastCompilerMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
  public String output;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  public MavenProject project;

  @Parameter(property = "maven.compiler.target", defaultValue = "11")
  public String targetBytecode;

  @Override
  public void execute() throws MojoExecutionException {
    if (Double.parseDouble(targetBytecode) < 11) {
      throw new MojoExecutionException("Flabbergast requires JDK 11 or later");
    }
    final var rootDir = project.getBasedir().toPath().resolve("src").resolve("main");
    try (final var paths = Files.walk(rootDir.resolve("o_0"))) {
      paths
          .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".o_0"))
          .forEach(
              file -> {
                final var chunk = rootDir.relativize(file).toString();
                final var errorCollector =
                    new ErrorCollector() {
                      @Override
                      public void emitConflict(
                          String error, Stream<Pair<SourceLocation, String>> locations) {
                        getLog().error(error);
                        locations
                            .map(
                                location ->
                                    String.format(
                                        "%s:%d:%d-%d:%d: %s",
                                        location.first().getFileName(),
                                        location.first().getStartLine(),
                                        location.first().getStartColumn(),
                                        location.first().getEndLine(),
                                        location.first().getEndColumn(),
                                        location.second()))
                            .forEach(getLog()::error);
                      }

                      @Override
                      public void emitError(SourceLocation location, String error) {
                        getLog()
                            .error(
                                String.format(
                                    "%s:%d:%d-%d:%d: %s",
                                    location.getFileName(),
                                    location.getStartLine(),
                                    location.getStartColumn(),
                                    location.getEndLine(),
                                    location.getEndColumn(),
                                    error));
                      }
                    };
                try {
                  final var libraryModificationTime = Files.getLastModifiedTime(file).toInstant();
                  Program.compile(
                          LanguageGrammar.automatic(),
                          Files.readString(file),
                          chunk,
                          errorCollector)
                      .finish()
                      .ifPresent(
                          renderer ->
                              KwsRenderer.generateDirectory(
                                  renderer,
                                  Path.of(output),
                                  chunk.substring(0, chunk.length() - 4),
                                  libraryModificationTime,
                                  file.toString(),
                                  errorCollector));
                } catch (IOException e) {
                  getLog().error(e);
                }
              });
    } catch (IOException e) {
      getLog().error(e);
      throw new MojoExecutionException("Failed to find sources.");
    }
  }
}
