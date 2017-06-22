package flabbergast;

import java.lang.Math;
import java.net.IDN;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import javax.xml.bind.DatatypeConverter;

public class StandardInterop extends Interop {
    public static final StandardInterop INSTANCE = new StandardInterop();
    private StandardInterop() {
        final SourceReference time_src = new NativeSourceReference(
            "<the big bang>");
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
        addMap(Long.class, ZonedDateTime.class, "time/compare", (left, right) -> ChronoUnit.SECONDS.between(right, left), ZonedDateTime.class, "to");
        add("time/days",  new FixedFrame("days", time_src) .add(InterlockedLookup.DAYS));
        add("time/from/parts", CreateTime::new);
        add(ZonedDateTime.class, "time/from/unix", (epoch, is_utc) -> ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch), is_utc ? ZoneId.of("Z") : ZoneId.systemDefault()),  Long.class, "epoch", Boolean.class, "is_utc");
        add("time/modify", ModifyTime::new);
        add("time/months", new FixedFrame("months", time_src)
            .add(InterlockedLookup.MONTHS));
        add("time/now/local", ReflectedFrame.create("now_local", ZonedDateTime.now(), InterlockedLookup.TIME_ACCESSORS));
        add("time/now/utc", ReflectedFrame.create("now_utc", ZonedDateTime.now(ZoneId.of("Z")), InterlockedLookup.TIME_ACCESSORS));
        addMap(ZonedDateTime.class, ZonedDateTime.class, "time/switch_zone", (initial, to_utc) -> initial.withZoneSameInstant(to_utc ? ZoneId.of("Z") : ZoneId.systemDefault()), Boolean.class, "to_utc");
        addMap(byte[].class, byte[].class, "utils/bin/compress/gzip", BinaryFunctions::compress);
        addMap(byte[].class, String.class, "utils/bin/from/base64", DatatypeConverter::parseBase64Binary);
        addMap(byte[].class, byte[].class, "utils/bin/hash/md5", x -> BinaryFunctions.checksum(x, "MD5"));
        addMap(byte[].class, byte[].class, "utils/bin/hash/sha1", x -> BinaryFunctions.checksum(x, "SHA-1"));
        addMap(byte[].class, byte[].class, "utils/bin/hash/sha256", x -> BinaryFunctions.checksum(x, "SHA-256"));
        addMap(String.class, byte[].class, "utils/bin/to/base64", DatatypeConverter::printBase64Binary);
        addMap(String.class, byte[].class, "utils/bin/to/hexstr", BinaryFunctions::bytesToHex, String.class, "delimiter", Boolean.class, "uppercase");
        add("utils/bin/uncompress/gzip", Decompress::new);
        addMap(Stringish.class, Double.class, "utils/float/to/str", Stringish::fromDouble , Boolean.class, "exponential", Long.class,  "digits");
        addMap(Stringish.class, Long.class, "utils/int/to/str", Stringish::fromInt , Boolean.class, "hex", Long.class,  "digits");
        addMap(Stringish.class, Long.class, "utils/ordinal", SupportFunctions::ordinalName);
        addMap(Double.class, String.class, "utils/parse/float", Double::parseDouble);
        addMap(Long.class, String.class, "utils/parse/int", (x, radix) -> Long.parseLong(x, radix.intValue()), Long.class, "radix");
        addMap(String.class, String.class, "utils/str/decode/punycode", (x, allow_unassigned) -> IDN.toUnicode(x, (allow_unassigned ? IDN.ALLOW_UNASSIGNED  : 0)), Boolean.class, "allow_unassigned");
        addMap(String.class, String.class, "utils/str/encode/punycode", (x, allow_unassigned, strict_ascii) -> IDN.toASCII(x, (allow_unassigned ? IDN.ALLOW_UNASSIGNED  : 0) | (strict_ascii ? IDN.USE_STD3_ASCII_RULES : 0)), Boolean.class, "allow_unassigned", Boolean.class, "strict_ascii");
        add("utils/str/escape", EscapeBuilder::new);
        add("utils/str/escape/char", EscapeCharacterBuilder::new);
        add("utils/str/escape/range", EscapeRangeBuilder::new);
        addMap(Long.class, Stringish.class, "utils/str/find", Stringish::find, String.class, "str", Long.class, "start", Boolean.class, "backward");
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
        addMap(String.class, String.class, "utils/str/replace", String::replace, String.class, "str", String.class,  "with");
        addMap(String.class, Stringish.class, "utils/str/slice", Stringish::slice, Long.class, false, "start", Long.class, true, "end", Long.class, true, "length");
        addMap(Boolean.class, String.class, "utils/str/suffixed", String::endsWith, String.class, "str");
        add("utils/str/to/categories", CharacterCategory::new);
        add("utils/str/to/codepoints", StringToCodepoints::new);
        addMap(byte[].class, Stringish.class, "utils/str/to/utf16be", x -> x.toUtf16(true));
        addMap(byte[].class, Stringish.class, "utils/str/to/utf16le", x -> x.toUtf16(false));
        addMap(byte[].class, Stringish.class, "utils/str/to/utf32be", x -> x.toUtf32(true));
        addMap(byte[].class, Stringish.class, "utils/str/to/utf32le", x -> x.toUtf32(false));
        addMap(byte[].class, Stringish.class, "utils/str/to/utf8", Stringish::toUtf8);
        addMap(String.class, String.class, "utils/str/trim", String::trim);
        addMap(String.class, String.class, "utils/str/upper_case", String::toUpperCase);
        Escape.createUnicodeActions(this::add);
    }
}

