using System;
using System.Text.RegularExpressions;

namespace Flabbergast
{
    public class EnvironmentUriHandler : UriHandler
    {
        public static readonly EnvironmentUriHandler INSTANCE = new EnvironmentUriHandler();

        private EnvironmentUriHandler()
        {
        }

        public string UriName => "Environment variables";

        public int Priority => 0;

        public Future ResolveUri(TaskMaster task_master, string uri, out LibraryFailure reason)
        {
            if (!uri.StartsWith("env:"))
            {
                reason = LibraryFailure.Missing;
                return null;
            }
            var name = uri.Substring(4);
            if (!Regex.IsMatch(name, "[A-Z_][A-Z0-9_]*"))
            {
                reason = LibraryFailure.BadName;
                return null;
            }
            reason = LibraryFailure.None;
            var content = Environment.GetEnvironmentVariable(name);
            return new Precomputation(content == null ? (object)Unit.NULL : new SimpleStringish(content));
        }
    }
}