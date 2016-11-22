using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;

namespace Flabbergast {
public static class SupportFunctions {

    private static readonly char[] symbols = CreateOrdinalSymbols();

    private static char[] CreateOrdinalSymbols() {
        var array = new char[26];
        for (var it = 0; it < 26; it++) {
            array[it] = (char)('A' + it);
        }
        Array.Sort(array);
        return array;
    }
    public static Stringish OrdinalName(long id) {
        return new SimpleStringish(OrdinalNameStr(id));
    }

    public static string OrdinalNameStr(long id) {
        var id_str = new char[(int)(sizeof(long) * 8 * Math.Log(2, symbols.Length)) + 1];
        if (id < 0) {
            id_str[0] = 'e';
            id = long.MaxValue + id;
        } else {
            id_str[0] = 'f';
        }
        for (var it = id_str.Length - 1; it > 0; it--) {
            id_str[it] = symbols[id % symbols.Length];
            id = id / symbols.Length;
        }
        return new string(id_str);
    }

    public static string NameForType(Type t) {
        if (typeof(Frame).IsAssignableFrom(t)) return "Frame";
        if (typeof(Stringish).IsAssignableFrom(t)) return "Str";
        if (typeof(Template) == t) return "Template";
        if (typeof(Unit) == t) return "Null";
        if (typeof(bool) == t) return "Bool";
        if (typeof(double) == t) return "Float";
        if (typeof(long) == t) return "Int";
        if (typeof(byte[]) == t) return "Bin";
        return t.ToString();
    }
}
}
