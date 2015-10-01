package flabbergast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class MainBuildCache {

    private static void discover(File root, List<File> sources,
                                 Set<String> known_classes) {
        for (File f : root.listFiles()) {
            if (f.isDirectory()) {
                discover(f, sources, known_classes);
            } else if (f.isFile()) {
                if (f.getName().endsWith(".o_0")
                        || f.getName().endsWith(".jo_0")) {
                    sources.add(f);
                } else if (f.getName().endsWith(".class")) {
                    try {
                        known_classes.add(f.getCanonicalPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("P", "preserve", false,
                          "Do not delete old class files.");
        CommandLineParser cl_parser = new GnuParser();
        final CommandLine result;

        try {
            result = cl_parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        if (result.getArgs().length != 0) {
            System.exit(1);
            return;
        }
        ErrorCollector collector = new ConsoleCollector();
        final Set<String> known_classes = new HashSet<String>();
        List<File> sources = new ArrayList<File>();
        File root = new File(".");
        discover(root, sources, known_classes);

        CompilationUnit<Boolean> unit = new WriterCompilationUnit(true) {
            @Override
            protected void fileEvent(File file) {
                try {
                    known_classes.remove(file.getCanonicalPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        boolean success = true;
        for (File source : sources) {
            try {
                String filename = source.getPath();
                Parser parser = Parser.open(source.getCanonicalPath());
                String file_root = ("flabbergast/library/" + removeSuffix(filename))
                                   .replace(File.separatorChar, '/')
                                   .replaceAll("[/.]+", "/").replace('-', '_');
                parser.parseFile(collector, unit, file_root);
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
            }
        }
        if (!result.hasOption('P')) {
            for (String dead : known_classes) {
                new File(dead).delete();
            }
        }
        System.exit(success ? 0 : 1);
    }

    public static String removeSuffix(String filename) {
        for (String suffix : new String[] {".o_0", ".jo_0"}) {
            if (filename.endsWith(suffix)) {
                return filename.substring(0,
                                          filename.length() - suffix.length());
            }
        }
        return filename;
    }
}
