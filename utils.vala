namespace Flabbergast.Utils {
	public class DefaultMap<K, V> : Gee.HashMap<K, V> {
		public delegate V MakeValue<K, V> (K key);
		private MakeValue<K, V> make_value;
		public DefaultMap (owned MakeValue<K, V> make_value) {
			base ();
			this.make_value = (owned) make_value;
		}
		public override V get (K key) {
			if (has_key (key)) {
				return base[key];
			} else {
				var @value = make_value (key);
				base[key] = value;
				return @value;
			}
		}
	}
}