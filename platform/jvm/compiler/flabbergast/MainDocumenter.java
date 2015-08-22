package flabbergast;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class MainDocumenter {

	private static boolean discover(File dir, int trim, String github,
			String output_root, ErrorCollector collector) throws Exception {
		boolean success = true;
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				success &= discover(f, trim, github, output_root, collector);
			} else if (f.isFile() && f.getName().endsWith(".o_0")) {
				String file = f.getCanonicalPath();
				String file_fragment = file.substring(trim, file.length() - 4);
				String uri = file_fragment.replace(File.separatorChar, '/');
				String output_filename = output_root + File.separator + "doc-"
						+ file_fragment.replace(File.separatorChar, '-')
						+ ".xml";
				Parser parser = Parser.open(file);
				Document doc = parser.documentFile(collector, uri, github);
				if (doc != null) {
					TransformerFactory transformerFactory = TransformerFactory
							.newInstance();
					Transformer transformer = transformerFactory
							.newTransformer();
					DOMSource source = new DOMSource(doc);
					StreamResult result = new StreamResult(new File(
							output_filename));
					transformer.transform(source, result);
				} else {
					success = false;
				}
			}
		}
		return success;
	}
	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("g", "github", true,
				"The URL to the GitHub version of these files.");
		options.addOption("o", "output", true,
				"The directory to place the docs.");
		options.addOption("h", "help", false, "Show this message and exit");
		CommandLineParser cl_parser = new GnuParser();
		final CommandLine result;

		try {
			result = cl_parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			System.exit(1);
			return;
		}

		if (result.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			System.err
					.println("Document a directory containing Flabbergast files.");
			formatter.printHelp("gnu", options);
			System.exit(1);
		}

		String[] directories = result.getArgs();
		if (directories.length == 0) {
			System.err
					.println("I need some directories full of delicious source files to document.");
			System.exit(1);
			return;
		}
		ErrorCollector collector = new ConsoleCollector();
		boolean success = true;
		try {
			for (String directory : directories) {
				File dir = new File(directory);
				success &= discover(dir, dir.getCanonicalPath().length() + 1,
						result.getOptionValue('g'),
						result.getOptionValue('o', "."), collector);

			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(success ? 0 : 1);
	}
}
