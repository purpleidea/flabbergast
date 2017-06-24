using System.Configuration;
using System.Web;

namespace Flabbergast
{
    public class SettingsHandler : UriHandler
    {
        public static readonly SettingsHandler INSTANCE = new SettingsHandler();

        private SettingsHandler()
        {
        }

        public string UriName => "VM-specific settings";

        public int Priority => 0;

        public Future ResolveUri(TaskMaster master, string uri, out LibraryFailure reason)
        {
            if (!uri.StartsWith("settings:"))
            {
                reason = LibraryFailure.Missing;
                return null;
            }
            var setting = ConfigurationManager.AppSettings[HttpUtility.UrlDecode(uri.Substring(9))];
            reason = LibraryFailure.None;
            return new Precomputation(setting == null ? Unit.NULL : (object)new SimpleStringish(setting));
        }
    }
}