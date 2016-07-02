using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Reflection.Emit;
using System.Text.RegularExpressions;
using NDesk.Options;

namespace Flabbergast {
public class Documenter {
    private static bool Discover(string directory, int trim, string github, bool verbose, string output_root, ErrorCollector collector) {
        var success = true;
        foreach (var path in Directory.EnumerateDirectories(directory)) {
            success &= Discover(path, trim, github, verbose, output_root, collector);
        }
        foreach (var file in Directory.EnumerateFiles(directory, "*.o_0")) {
            var file_fragment = file.Substring(trim, file.Length - 4 - trim);
            var uri = file_fragment.Replace(Path.DirectorySeparatorChar, '/');
            var output_filename = Path.Combine(output_root, "doc-" + file_fragment.Replace(Path.DirectorySeparatorChar, '-') + ".xml");
            if (verbose) {
                Console.WriteLine(file);
            }
            var parser = Parser.Open(file);
            var doc = parser.DocumentFile(collector, uri, github);
            if (doc != null) {
                doc.Save(output_filename);
            } else {
                success = false;
            }
        }
        return success;
    }
    public static int Main(string[] args) {
        string github = null;
        string output_root = ".";
        var show_help = false;
        var verbose = false;
        var options = new OptionSet {
            {"g=|github", "The URL to the GitHub version of these files.", v => github = v},
            {"o=|output", "The directory to place the docs.", v => output_root = v},
            {"v=|verbose", "List files as they are being processed.", v => verbose = v != null},
            {"h|help", "show this message and exit", v => show_help = v != null}
        };

        List<string> directories;
        try {
            directories = options.Parse(args);
        } catch (OptionException e) {
            Console.Write(AppDomain.CurrentDomain.FriendlyName + ": ");
            Console.WriteLine(e.Message);
            Console.WriteLine("Try “" + AppDomain.CurrentDomain.FriendlyName + " --help” for more information.");
            return 1;
        }

        if (show_help) {
            Console.WriteLine("Usage: " + AppDomain.CurrentDomain.FriendlyName + " files ...");
            Console.WriteLine("Document a directory containing Flabbergast files.");
            Console.WriteLine();
            Console.WriteLine("Options:");
            options.WriteOptionDescriptions(Console.Out);
            return 0;
        }

        if (directories.Count == 0) {
            Console.WriteLine("I need some directories full of delicious source files to document.");
            return 1;
        }

        var collector = new ConsoleCollector();
        var success = true;
        foreach (var dir in directories) {
            Discover(dir, dir.Length + (dir[dir.Length - 1] == Path.DirectorySeparatorChar ? 0 : 1), github, verbose, output_root, collector);
        }
        return success ? 0 : 1;
    }
}
}
