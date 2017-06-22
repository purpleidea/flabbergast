package flabbergast;

public class SupportFunctions {
    private static char[] symbols = createOrdinalSymbols();

    private static char[] createOrdinalSymbols() {
        char[] array = new char[62];
        for (int it = 0; it < 10; it++) {
            array[it] = (char)('0' + it);
        }
        for (int it = 0; it < 26; it++) {
            array[it + 10] = (char)('A' + it);
            array[it + 36] = (char)('a' + it);
        }
        return array;
    }

    public static Stringish ordinalName(long id) {
        return new SimpleStringish(ordinalNameStr(id));
    }

    public static String ordinalNameStr(long id) {
        char[] id_str = new char[(int)(Long.SIZE * Math.log(2) / Math
                                       .log(symbols.length)) + 1];
        if (id < 0) {
            id_str[0] = 'e';
            id = Long.MAX_VALUE + id;
        } else {
            id_str[0] = 'f';
        }
        for (int it = id_str.length - 1; it > 0; it--) {
            id_str[it] = symbols[(int)(id % symbols.length)];
            id = id / symbols.length;
        }
        return new String(id_str);
    }

    public static String nameForClass(Class<?> t) {
        if (Stringish.class.isAssignableFrom(t)) {
            return "Str";
        }
        if (t == Boolean.class || t == boolean.class) {
            return "Bool";
        }
        if (t == Double.class || t == double.class) {
            return "Float";
        }
        if (Frame.class.isAssignableFrom(t)) {
            return "Frame";
        }
        if (t == Long.class || t == long.class) {
            return "Int";
        }
        if (t == Template.class) {
            return "Template";
        }
        if (t == Unit.class) {
            return "Null";
        }
        if (t == byte[].class) {
            return "Bin";
        }
        return t.getSimpleName();
    }


    private SupportFunctions() {}
}
