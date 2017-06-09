package flabbergast;

import java.lang.Math;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.DatatypeConverter;

public class StandardInterop extends Interop {
    public static final StandardInterop INSTANCE = new StandardInterop();
    private StandardInterop() {
        final Charset UTF_32BE = Charset.forName("UTF-32BE");
        final Charset UTF_32LE = Charset.forName("UTF-32LE");
        addMap(Double.class, Double.class, "math/abs", Math::abs);
        addMap(Double.class, Double.class, "math/ceiling", Math::ceil);
        addMap(Double.class, Double.class, "math/circle/arccos", (x, angle_unit) -> Math.acos(x) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/circle/arcsin", (x, angle_unit) -> Math.asin(x) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/circle/arctan", (x, angle_unit) -> Math.atan(x) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/circle/cos", (x, angle_unit) -> Math.cos(x * angle_unit), Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/circle/sin", (x, angle_unit) -> Math.sin(x * angle_unit), Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/circle/tan", (x, angle_unit) -> Math.tan(x * angle_unit), Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/floor", Math::floor);
        addMap(Double.class, Double.class, "math/hyperbola/arccos", (x, angle_unit) -> Math.log(x + Math.sqrt(x * x - 1.0)) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/hyperbola/arcsin", (x, angle_unit) ->	Math.log(x + Math.sqrt(x * x + 1.0)) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/hyperbola/arctan", (x, angle_unit) ->	0.5 * Math.log((1.0 + x) / (1.0 - x)) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/hyperbola/cos", (x, angle_unit) -> Math.cosh(x * angle_unit), Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/hyperbola/sin", (x, angle_unit) -> Math.sinh(x * angle_unit), Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/hyperbola/tan", (x, angle_unit) -> Math.tanh(x * angle_unit), Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/log", (x, base) -> Math.log(x) / Math.log(base), Double.class, "real_base");
        addMap(Double.class, Double.class, "math/power", Math::pow, Double.class, "real_exponent");
        addMap(Double.class, Double.class, "math/round", (x, places) -> { double shift = Math.pow(10, places); return Math.round(x * shift) / shift; }, Long.class, "real_places");
        add("parse/json", JsonParser::new);
        add("sql/query", JdbcQuery::new);
        addMap(byte[].class, byte[].class, "utils/bin/compress/gzip", BinaryFunctions::compress);
        addMap(byte[].class, String.class, "utils/bin/from/base64", DatatypeConverter::parseBase64Binary);
        addMap(byte[].class, byte[].class, "utils/bin/hash/md5", x -> BinaryFunctions.checksum(x, "MD5"));
        addMap(byte[].class, byte[].class, "utils/bin/hash/sha1", x -> BinaryFunctions.checksum(x, "SHA-1"));
        addMap(byte[].class, byte[].class, "utils/bin/hash/sha256", x -> BinaryFunctions.checksum(x, "SHA-256"));
        addMap(String.class, byte[].class, "utils/bin/to/base64", DatatypeConverter::printBase64Binary);
        add("utils/bin/uncompress/gzip", Decompress::new);
        addMap(Stringish.class, Long.class, "utils/ordinal", SupportFunctions::ordinalName);
        add("utils/parse/float", ParseDouble::new);
        add("utils/parse/int", ParseInt::new);
        addMap(Stringish.class, Long.class, "utils/str/from/codepoint", Stringish::fromCodepoint);
        addMap(String.class, byte[].class, "utils/str/from/utf16be", x -> new String(x, StandardCharsets.UTF_16BE));
        addMap(String.class, byte[].class, "utils/str/from/utf16le", x -> new String(x, StandardCharsets.UTF_16LE));
        addMap(String.class, byte[].class, "utils/str/from/utf32be", x -> new String(x, UTF_32BE));
        addMap(String.class, byte[].class, "utils/str/from/utf32le", x -> new String(x, UTF_32LE));
        addMap(String.class, byte[].class, "utils/str/from/utf8", x -> new String(x, StandardCharsets.UTF_8));
        addMap(Boolean.class, Stringish.class, "utils/str/identifier", TaskMaster::verifySymbol);
        addMap(Long.class, Stringish.class, "utils/str/length/utf16", Stringish::getUtf16Length);
        addMap(Long.class, Stringish.class, "utils/str/length/utf8", Stringish::getUtf8Length);
        addMap(String.class, String.class, "utils/str/lower_case", String::toLowerCase);
        addMap(Boolean.class, String.class, "utils/str/prefixed", String::startsWith, String.class, "str");
        addMap(Boolean.class, String.class, "utils/str/suffixed", String::endsWith, String.class, "str");
        addMap(byte[].class, Stringish.class, "utils/str/to/utf16be", x -> x.toUtf16(true));
        addMap(byte[].class, Stringish.class, "utils/str/to/utf16le", x -> x.toUtf16(false));
        addMap(byte[].class, Stringish.class, "utils/str/to/utf32be", x -> x.toUtf32(true));
        addMap(byte[].class, Stringish.class, "utils/str/to/utf32le", x -> x.toUtf32(false));
        addMap(byte[].class, Stringish.class, "utils/str/to/utf8", Stringish::toUtf8);
        addMap(String.class, String.class, "utils/str/trim", String::trim);
        addMap(String.class, String.class, "utils/str/upper_case", String::toUpperCase);
    }
}

