using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Common;
using System.Threading;

namespace Flabbergast {
	public class DbQuery : Computation {
		private delegate void Retriever (DbDataReader rs, MutableFrame frame);
		private delegate object Unpacker (DbDataReader rs, int position);

		private static Retriever Bind(string name, int position, Unpacker unpacker) {
				return (rs, frame) => frame.Set(name, rs.IsDBNull(position) ? Unit.NULL : unpacker(rs, position));
		}

		static readonly Dictionary<System.Type, Unpacker> unpackers = new Dictionary<System.Type, Unpacker>();
		static DbQuery () {
			AddUnpacker((rs, position) => {
					var str = rs.GetString(position);
					return str == null ? null : new SimpleStringish(str);
			}, typeof(string));
			AddUnpacker((rs, position) => rs.GetInt64(position), typeof(byte), typeof(sbyte), typeof(short), typeof(ushort), typeof(int), typeof(uint), typeof(long), typeof(ulong));
			AddUnpacker((rs, position) => rs.GetDouble(position), typeof(float), typeof(double));
			AddUnpacker((rs, position) => rs.GetBoolean(position), typeof(bool));
		}

		static void AddUnpacker(Unpacker unpacker, params System.Type[] sql_types) {
			foreach (var sql_type in sql_types) {
				unpackers[sql_type] = unpacker;
			}
		}

		private readonly SourceReference source_ref;
		private readonly Context context;
		private readonly Frame self;
		private int interlock = 3;
		private DbConnection connection = null;
		private string query = null;

		public DbQuery(TaskMaster task_master, SourceReference source_ref,
				Context context, Frame self, Frame container) : base(task_master){
			this.source_ref = source_ref;
			this.context = context;
			this.self = self;
		}

		protected override bool Run() {
			if (connection == null) {
				new Lookup(task_master, source_ref, new []{"connection"},
						context).Notify(return_value =>  {
						if (return_value is ReflectedFrame) {
							Object backing = ((ReflectedFrame) return_value).Backing;
							if (backing is DbConnection) {
								connection = (DbConnection) backing;
								if (Interlocked.Decrement(ref interlock) == 0) {
									task_master.Slot(this);
								}
								return;
							}
						}
						task_master
								.ReportOtherError(source_ref,
										"Expected “connection” to come from “sql:” import.");
					});
				new Lookup(task_master, source_ref, new []{"sql_query"},
						context).Notify(return_value=> {
						if (return_value is Stringish) {
							query = return_value.ToString();
							if (Interlocked.Decrement(ref interlock) == 0) {
								task_master.Slot(this);
							}
							return;
						}
						task_master.ReportOtherError(source_ref, string.Format(
								"Expected type Str for “sql_query”, but got {0}.",
								Stringish.NameForType(return_value.GetType())));
				});
				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}
			try {
				var command = connection.CreateCommand(); 
				command.CommandType = System.Data.CommandType.Text;
				command.CommandText = query; 
				var reader = command.ExecuteReader();

				var retrievers = new List<Retriever>();
				for (int col = 0; col < reader.FieldCount; col++) {
					Unpacker unpacker;
					if (!unpackers.TryGetValue(reader.GetFieldType(col), out unpacker)) {
						task_master
								.ReportOtherError(
										source_ref,
										string.Format(
												"Cannot convert SQL type “{0}” for column “{1}” into Flabbergast type.",
												reader.GetFieldType(col),
												reader.GetName(col)));
					}
					if (!task_master.VerifySymbol(source_ref,
							reader.GetName(col))) {
						return false;
					}
					retrievers.Add(Bind(reader.GetName(col), col, unpacker));
				}

				var list = new MutableFrame(task_master, source_ref, context, self);
				for (int it = 1; reader.Read(); it++) {
					var frame = new MutableFrame(task_master, source_ref, list.Context, list);
					foreach (var r in retrievers) {
						r(reader, frame);
					}
					list.Set(it, frame);
				}
				result = list;
				return true;
			} catch (DataException e) {
				task_master.ReportOtherError(source_ref, e.Message);
				return false;
			}
		}
	}

	public class DbUriHandler : UriHandler {

		public static UriHandler INSTANCE = new DbUriHandler();

		private static readonly Dictionary<string, Func<DbConnection, object>> connection_hooks = new Dictionary<string, Func<DbConnection, object>> {
			{"database",  c =>  c.Database},
			{"product_name",  c => c.DataSource},
			{"product_version",  c => c.ServerVersion},
			{"driver_name",  c => c.GetType().Name},
			{"driver_version",  c => c.GetType().Assembly.GetName().Version.ToString()},
			{"platform",  c => "ADO.NET"}
		};

		internal static bool ParseUri(String uri_fragment, DbConnectionStringBuilder builder, string host_param, string port_param, string user_param, string password_param, string database_param, out string err) {
			err = null;
			int host_start = 0;
			int user_end = 0;
			while (user_end < uri_fragment.Length && (Char.IsLetterOrDigit(uri_fragment[user_end]) || uri_fragment[user_end] == '_')) user_end++;
			if (user_end == uri_fragment.Length) {
				// We know this is malformed.
				err = "Missing “/” followed by database in SQL URI.";
				return false;
			} else {
				switch(uri_fragment[user_end]) {
					case '@':
						// End of user string.
						builder[user_param] = uri_fragment.Substring(0, user_end);
						host_start = user_end + 1;
						break;
					case ':':
						// Possible password. Might be port.
						int password_end = user_end + 1;
						while (password_end < uri_fragment.Length && "/@".IndexOf(uri_fragment[password_end]) == -1) password_end++;
						if (password_end == uri_fragment.Length) {
							// We know this is malformed.
							err = "Missing “/” followed by database in SQL URI.";
							return false;
						} else if (uri_fragment[password_end] == '@') {
							host_start = password_end + 1;
							builder[user_param] = uri_fragment.Substring(0, user_end);
							builder[password_param] = uri_fragment.Substring(user_end + 1, password_end - user_end - 1);
						}
						// Else, this is really the host:port.
						break;
					default:
						// This is really the host.
						break;
				}
			}
			int host_end = host_start;
			int db_start;
			while (host_end < uri_fragment.Length && "/:".IndexOf(uri_fragment[host_end]) == -1) {
				// IPv6 address?
				if (uri_fragment[host_end] == '[') {
					while (host_end < uri_fragment.Length && uri_fragment[host_end] != ']') host_end++;
				}
				host_end++;
			}
			if (host_end >= uri_fragment.Length) {
				err = "Missing “/” followed by database in SQL URI.";
				return false;
			}
			if (uri_fragment[host_end] == ':') {
				int port_start = host_end + 1;
				int port_end = port_start;
				while (port_end < uri_fragment.Length && Char.IsDigit(uri_fragment[port_end])) port_end++;
				if (port_end == uri_fragment.Length) {
					err = "Missing “/” followed by database in SQL URI.";
					return false;
				}
				if (uri_fragment[port_end] != '/') {
					err = "Non-numeric data in port in SQL URI.";
					return false;
				}
				builder[port_param] = uri_fragment.Substring(port_start, port_end - port_start);
				db_start = port_end + 1;
			} else if (uri_fragment[host_end] == '/') {
				db_start = host_end + 1;
			} else {
				err = "Junk after host in SQL URI.";
				return false;
			}

			builder[host_param] = uri_fragment.Substring(host_start, host_end - host_start);
			int db_end = db_start;
			while (db_end < uri_fragment.Length && uri_fragment[db_end] != '/') db_end++;
			if (db_end < uri_fragment.Length) {
				err = "Junk after database in SQL URI.";
				return false;
			}
			builder[database_param] = uri_fragment.Substring(db_start);
			return true;
		}

		private DbUriHandler() {
		}

		public string UriName {
			get { return "ADO.NET gateway";}
		}
		public Computation ResolveUri(TaskMaster task_master, string uri, out LibraryFailure reason) {
			if (!uri.StartsWith("sql:")) {
				reason = LibraryFailure.Missing;
				return null;
			}
			reason = LibraryFailure.None;
			try {
				var param = new Dictionary<string, string>();

				int first_colon = 5;
				while (first_colon < uri.Length && uri[first_colon] != ':') first_colon++;
				if (first_colon >= uri.Length) {
					return new FailureComputation(task_master, new ClrSourceReference(), "Bad provider in URI “" + uri + "”.");
				}
				var provider = uri.Substring(4, first_colon - 4);
				int question_mark = first_colon;
				while (question_mark < uri.Length && uri[question_mark] != '?') question_mark++;
				var uri_fragment = uri.Substring(first_colon + 1, question_mark - first_colon - 1);
				if (question_mark < uri.Length - 1) {
					foreach (var param_str in uri.Substring(question_mark + 1).Split(new []{'&'})) {
						if (param_str.Length == 0)
							continue;
						var parts = param_str.Split(new []{'='}, 2);
						if (parts.Length != 2) {
							return new FailureComputation(task_master, new ClrSourceReference(), "Bad parameter “" + param_str + "”.");
						}
						param[parts[0]] = parts[1];
					}
				}

				string error;
				var connection = DbParser.Parse(provider, uri_fragment, param, out error);
				if (connection == null) {
					return new FailureComputation(task_master, new ClrSourceReference(), error ?? "Bad URI.");
				}

				connection.Open();

				var connection_proxy = ReflectedFrame.Create(task_master, connection, connection_hooks);
				connection_proxy.Set("provider", new SimpleStringish(provider));
				return new Precomputation(connection_proxy);
			} catch (Exception e) {
				return new FailureComputation(task_master, new ClrSourceReference(e), e.Message);
			}
		}
	}
}
