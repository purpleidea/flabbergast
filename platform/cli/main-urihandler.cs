using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using NDesk.Options;

namespace Flabbergast
{
    public class FindUriHandlers
    {
        public static int Main(string[] args)
        {
            var show_help = false;
            var options = new OptionSet
            {
                {"h|help", "show this message and exit", v => show_help = v != null}
            };

            List<string> files;
            try
            {
                files = options.Parse(args);
            }
            catch (OptionException e)
            {
                Console.Error.Write(AppDomain.CurrentDomain.FriendlyName + ": ");
                Console.Error.WriteLine(e.Message);
                Console.Error.WriteLine("Try “" + AppDomain.CurrentDomain.FriendlyName +
                                        " --help” for more information.");
                return 1;
            }

            if (show_help)
            {
                Console.WriteLine("Usage: " + AppDomain.CurrentDomain.FriendlyName + " AssemblyName1 AssemblyName2");
                Console.WriteLine("Produce a .");            if (files.Count > 1)
            {
                Console.Error.WriteLine("No more than one Flabbergast script may be given.");
                return 1;
            }
            Console.ForegroundColor = ConsoleColor.Blue;
 
                Console.WriteLine();
                Console.WriteLine("Options:");
                options.WriteOptionDescriptions(Console.Out);
                return 1;
            }
						Console.WriteLine("# Automatically Generated using flabbergast-urihandler");
						foreach(var file in files) {
                var assembly = Assembly.LoadFile(file);
						    Console.WriteLine("# " + assembly.Location + " " + assembly.FullName);
                foreach (var type_name in assembly.GetTypes().Where(t => typeof(UriService).IsAssignableFrom(t) && !t.IsAbstract && t.IsPublic && t.IsVisible && t.GetConstructor(Type.EmptyTypes) != null).Select(t => t.AssemblyQualifiedName)) {
									Console.WriteLine(type_name);
								}
            }
						return 0;
        }
    }
}
