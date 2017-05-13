package flabbergast.interop;

import static flabbergast.export.NativeBinding.*;
import static flabbergast.lang.AnyConverter.*;

import flabbergast.export.LookupAssistant;
import flabbergast.export.NativeBinding;
import flabbergast.lang.*;
import flabbergast.util.WhinyFunction;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.IDN;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

/**
 * A {@link UriService} that can be exported via the plugin architecture to provide standard
 * interconnects for supporting the Flabbergast runtime library's <tt>native:</tt> URIs as well as
 * other common URIs.
 */
public final class StandardUriService implements UriService {

  private static final UriHandler[] INTEROP_HANDLERS =
      new UriHandler[] {
        NativeBinding.create("emit", EmitBaseGenerator.bindings()),
        NativeBinding.create(
            "lookup",
            NativeBinding.of(
                "action.access",
                function(
                    asProxy(),
                    LookupAction::access,
                    AnyConverter.asName(false),
                    "name",
                    AnyConverter.asTemplate(false),
                    "next")),
            NativeBinding.of(
                "action.fail",
                Any.of(
                    Frame.<LookupAction>proxyOf(
                        "lookup action fail", "lookup", LookupAction.FAIL, Stream.empty()))),
            NativeBinding.of("action.finish", LookupAction.FINISH),
            NativeBinding.of("action.fork", LookupAction.FORK),
            NativeBinding.of(
                "action.next",
                Any.of(
                    Frame.<LookupAction>proxyOf(
                        "lookup action next", "lookup", LookupAction.NEXT, Stream.empty()))),
            NativeBinding.of(
                "action.then",
                function(
                    asProxy(),
                    LookupAction::then,
                    AnyConverter.asTemplate(false),
                    "next",
                    LookupAction.CONVERTER,
                    "action")),
            NativeBinding.of(
                "custom",
                function(
                    LOOKUP_HANDLER,
                    (selector, explorer) ->
                        new LookupHandler(
                            explorer == null
                                ? LookupExplorer.EXACT
                                : new CustomLookupExplorer(explorer),
                            selector == null
                                ? LookupSelector.FIRST
                                : new CustomLookupSelector(selector)),
                    AnyConverter.asTemplate(true),
                    "explorer",
                    AnyConverter.asTemplate(true),
                    "selector")),
            NativeBinding.of("all", LookupExplorer.EXACT, LookupSelector.ALL),
            NativeBinding.of("coalescing", LookupExplorer.NULL_COALESCING, LookupSelector.FIRST),
            NativeBinding.of("existence", LookupExplorer.EXACT, LookupSelector.EXISTS),
            NativeBinding.of("flag_divided", LookupExplorer.FLAG_DIVIDED, LookupSelector.FIRST),
            NativeBinding.of("flag_divides", LookupExplorer.FLAG_DIVIDES, LookupSelector.FIRST),
            NativeBinding.of("flag_contained", LookupExplorer.FLAG_CONTAINED, LookupSelector.FIRST),
            NativeBinding.of("flag_contains", LookupExplorer.FLAG_CONTAINS, LookupSelector.FIRST),
            NativeBinding.of(
                "flag_intersect", LookupExplorer.FLAG_INTERSECTS, LookupSelector.FIRST),
            NativeBinding.of("merge", LookupExplorer.EXACT, LookupSelector.MERGING),
            NativeBinding.of(
                "filtered_explorer",
                function(
                    LOOKUP_HANDLER,
                    (handler, filter) ->
                        handler.withExplorer(LookupExplorer.filter(filter, handler.explorer())),
                    AnyConverter.asLookup(false),
                    "base",
                    AnyConverter.asTemplate(false),
                    "filter")),
            NativeBinding.of(
                "filtered_selector",
                function(
                    LOOKUP_HANDLER,
                    (handler, filter) ->
                        handler.withSelector(LookupSelector.filter(filter, handler.selector())),
                    AnyConverter.asLookup(false),
                    "base",
                    AnyConverter.asTemplate(false),
                    "filter")),
            NativeBinding.of(
                "fuzzy",
                function(
                    LOOKUP_HANDLER,
                    cutoff -> new LookupHandler(LookupExplorer.fuzzy(cutoff), LookupSelector.FIRST),
                    AnyConverter.asInt(false),
                    "cutoff")),
            NativeBinding.of(
                "transforming_explorer",
                function(
                    LOOKUP_HANDLER,
                    (handler, mapper) ->
                        handler.withExplorer(LookupExplorer.map(mapper, handler.explorer())),
                    AnyConverter.asLookup(false),
                    "base",
                    AnyConverter.asTemplate(false),
                    "mapper")),
            NativeBinding.of(
                "transforming_selector",
                function(
                    LOOKUP_HANDLER,
                    (handler, template) ->
                        handler.withSelector(LookupSelector.map(template, handler.selector())),
                    AnyConverter.asLookup(false),
                    "base",
                    AnyConverter.asTemplate(false),
                    "template")),
            NativeBinding.of(
                "take_first",
                function(
                    LOOKUP_HANDLER,
                    LookupHandler::takeFirst,
                    AnyConverter.asLookup(false),
                    "tail",
                    AnyConverter.asInt(false),
                    "count",
                    AnyConverter.asLookup(false),
                    "head")),
            NativeBinding.of(
                "take_until",
                function(
                    LOOKUP_HANDLER,
                    (tail, referenceName, head) ->
                        tail.takeUntil(
                            new Predicate<Name>() {
                              @Override
                              public boolean test(Name name) {
                                return name.equals(referenceName);
                              }

                              @Override
                              public String toString() {
                                return "is " + referenceName;
                              }
                            },
                            head),
                    AnyConverter.asLookup(false),
                    "tail",
                    AnyConverter.asName(false),
                    "name",
                    AnyConverter.asLookup(false),
                    "head")),
            NativeBinding.of(
                "take_until_any",
                function(
                    LOOKUP_HANDLER,
                    (tail, referenceNames, head) ->
                        tail.takeUntil(
                            new Predicate<Name>() {
                              @Override
                              public boolean test(Name name) {
                                return referenceNames.containsValue(name);
                              }

                              @Override
                              public String toString() {
                                return referenceNames
                                    .values()
                                    .stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(", ", "is (", ")"));
                              }
                            },
                            head),
                    AnyConverter.asLookup(false),
                    "tail",
                    AnyConverter.frameOf(AnyConverter.asName(false), false),
                    "names",
                    AnyConverter.asLookup(false),
                    "head")),
            NativeBinding.of(
                "take_last",
                function(
                    LOOKUP_HANDLER,
                    LookupHandler::takeLast,
                    AnyConverter.asLookup(false),
                    "tail",
                    AnyConverter.asInt(false),
                    "count",
                    AnyConverter.asLookup(false),
                    "head")),
            NativeBinding.of(
                "or_else",
                LookupAssistant.create(
                    () ->
                        new LookupAssistant.Recipient() {
                          Any defaultValue;
                          LookupHandler handler;

                          @Override
                          public void run(
                              Future<Any> future,
                              SourceReference sourceReference,
                              Context context) {
                            future.complete(
                                Any.of(
                                    handler.withSelector(
                                        LookupSelector.orElse(handler.selector(), defaultValue))));
                          }
                        },
                    LookupAssistant.find(
                        AnyConverter.asLookup(false), (h, handler) -> h.handler = handler, "base"),
                    LookupAssistant.find((h, value) -> h.defaultValue = value, "default"))),
            NativeBinding.of(
                "or_else_compute",
                function(
                    LOOKUP_HANDLER,
                    (handler, template) ->
                        handler.withSelector(
                            LookupSelector.orElseCompute(handler.selector(), template)),
                    AnyConverter.asLookup(false),
                    "base",
                    AnyConverter.asTemplate(false),
                    "default")),
            NativeBinding.of(
                "splice_handlers",
                function(
                    LOOKUP_HANDLER,
                    LookupHandler::withSelector,
                    AnyConverter.asLookup(false),
                    "explorer",
                    AnyConverter.asLookup(false),
                    "selector"))),
        NativeBinding.create(
            "math",
            NativeBinding.of(
                "abs", numberMapFunction(x -> Any.of(Math.abs(x)), x -> Any.of(Math.abs(x)))),
            NativeBinding.of("ceiling", mapFunction(FLOAT, asFloat(false), Math::ceil)),
            NativeBinding.of(
                "circle.arccos",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> Math.acos(x) / angleUnit,
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of(
                "circle.arcsin",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> Math.asin(x) / angleUnit,
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of(
                "circle.arctan",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> Math.atan(x) / angleUnit,
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of(
                "circle.cos",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> Math.cos(x * angleUnit),
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of(
                "circle.sin",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> Math.sin(x * angleUnit),
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of(
                "circle.tan",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> Math.tan(x * angleUnit),
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of("floor", mapFunction(FLOAT, asFloat(false), Math::floor)),
            NativeBinding.of(
                "hyperbola.arccos",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> Math.log(x + Math.sqrt(x * x - 1.0)) / angleUnit,
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of(
                "hyperbola.arcsin",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> Math.log(x + Math.sqrt(x * x + 1.0)) / angleUnit,
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of(
                "hyperbola.arctan",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> 0.5 * Math.log((1.0 + x) / (1.0 - x)) / angleUnit,
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of(
                "hyperbola.cos",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> Math.cosh(x * angleUnit),
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of(
                "hyperbola.sin",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> Math.sinh(x * angleUnit),
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of(
                "hyperbola.tan",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, angleUnit) -> Math.tanh(x * angleUnit),
                    asFloat(false),
                    "angle_unit")),
            NativeBinding.of(
                "log",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, base) -> Math.log(x) / Math.log(base),
                    asFloat(false),
                    "real_base")),
            NativeBinding.of(
                "power",
                mapFunction(FLOAT, asFloat(false), Math::pow, asFloat(false), "real_exponent")),
            NativeBinding.of(
                "round",
                mapFunction(
                    FLOAT,
                    asFloat(false),
                    (x, places) -> {
                      final var shift = Math.pow(10, places);
                      return Math.round(x * shift) / shift;
                    },
                    asInt(false),
                    "real_places"))),
        NativeBinding.create("sql", NativeBinding.of("query", SqlQuery.DEFINITION)),
        NativeBinding.create(
            "time",
            NativeBinding.of(
                "compare",
                mapFunction(
                    INT,
                    asDateTime(false),
                    (left, right) -> ChronoUnit.SECONDS.between(right, left),
                    asDateTime(false),
                    "to")),
            NativeBinding.of("from_parts", TimeCreate.DEFINITION),
            NativeBinding.of(
                "from_unix",
                function(
                    TIME,
                    (epoch, isUtc) ->
                        ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(epoch),
                            isUtc ? ZoneId.of("Z") : ZoneId.systemDefault()),
                    AnyConverter.asInt(false),
                    "epoch",
                    AnyConverter.asBool(false),
                    "is_utc")),
            NativeBinding.of("modify", TimeModify.DEFINITION),
            NativeBinding.of(
                "switch_zone",
                mapFunction(
                    TIME,
                    AnyConverter.asDateTime(false),
                    (time, isUtc) ->
                        time.withZoneSameInstant(isUtc ? ZoneId.of("Z") : ZoneId.systemDefault()),
                    AnyConverter.asBool(false),
                    "is_utc"))),
        NativeBinding.create(
            "utils",
            Stream.concat(
                Stream.of(
                    NativeBinding.of(
                        "bin.compress.gzip",
                        mapOverride(
                            BIN,
                            asBin(false),
                            input -> {
                              try (final var output = new ByteArrayOutputStream();
                                  final var gzip = new GZIPOutputStream(output)) {
                                gzip.write(input);
                                return output.toByteArray();
                              } catch (final Exception e) {
                                return new byte[0];
                              }
                            })),
                    NativeBinding.of("bin.empty", Any.of(new byte[0])),
                    NativeBinding.of(
                        "bin.from.base64",
                        mapOverride(BIN, asString(false), Base64.getDecoder()::decode)),
                    NativeBinding.of(
                        "bin.hash.md5", mapOverride(BIN, asBin(false), checksum("MD5"))),
                    NativeBinding.of(
                        "bin.hash.sha1", mapOverride(BIN, asBin(false), checksum("SHA-1"))),
                    NativeBinding.of(
                        "bin.hash.sha256", mapOverride(BIN, asBin(false), checksum("SHA-256"))),
                    NativeBinding.of(
                        "bin.to.base64",
                        mapOverride(BIN, asBin(false), Base64.getEncoder()::encode)),
                    NativeBinding.of(
                        "bin.to.hexstr",
                        mapOverride(
                            STRING,
                            asBin(false),
                            (input, delimiter, upper) ->
                                IntStream.range(0, input.length)
                                    .map(idx -> input[idx])
                                    .mapToObj(b -> String.format(upper ? "%02X" : "%02x", b))
                                    .collect(Collectors.joining(delimiter)),
                            asString(false),
                            "delimiter",
                            asBool(false),
                            "uppercase")),
                    NativeBinding.of(
                        "bin.uncompress.gzip",
                        mapOverride(
                            BIN,
                            asBin(false),
                            input -> {
                              try (final var gunzip =
                                      new GZIPInputStream(new ByteArrayInputStream(input));
                                  final var output = new ByteArrayOutputStream()) {

                                int count;
                                final var buffer = new byte[1024];
                                while ((count = gunzip.read(buffer, 0, buffer.length)) > 0) {
                                  output.write(buffer, 0, count);
                                }
                                return output.toByteArray();
                              }
                            })),
                    NativeBinding.of(
                        "bin.uuid",
                        NativeBinding.generator(
                            BIN,
                            () -> {
                              final var uuid = UUID.randomUUID();
                              return ByteBuffer.allocate(16)
                                  .putLong(uuid.getMostSignificantBits())
                                  .putLong(uuid.getLeastSignificantBits())
                                  .array();
                            })),
                    NativeBinding.of(
                        "counter.int",
                        NativeBinding.generator(
                            asGeneratorTemplate(INT), () -> new AtomicLong()::getAndIncrement)),
                    NativeBinding.of(
                        "counter.unicode",
                        NativeBinding.generator(
                            asGeneratorTemplate(CODEPOINT),
                            () -> {
                              final var counter = new AtomicInteger();
                              return () -> {
                                int v = counter.incrementAndGet();
                                if (v >= 0 && v <= 6400) {
                                  return v + 0xE000;
                                } else if (v > 6400 && v <= 6400 + 65535) {
                                  return v - 6400 + 0xF0000;
                                } else if (v > 6400 + 65535 && v <= 6400 + 2 * 65535) {
                                  return v - (6400 + 65535) + 0x100000;
                                } else {
                                  return null;
                                }
                              };
                            })),
                    NativeBinding.of(
                        "float32.to.bin",
                        mapOverride(
                            BIN,
                            asFloat(false),
                            (value, bigEndian) ->
                                ByteBuffer.allocate(Float.BYTES)
                                    .order(
                                        bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                                    .putFloat(value.floatValue())
                                    .array(),
                            asBool(false),
                            "big_endian")),
                    NativeBinding.of(
                        "float64.to.bin",
                        mapOverride(
                            BIN,
                            asFloat(false),
                            (value, bigEndian) ->
                                ByteBuffer.allocate(Double.BYTES)
                                    .order(
                                        bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                                    .putDouble(value)
                                    .array(),
                            asBool(false),
                            "big_endian")),
                    NativeBinding.of(
                        "float.to.str",
                        mapOverride(
                            STR,
                            asFloat(false),
                            Str::from,
                            asBool(false),
                            "exponential",
                            asInt(false),
                            "digits")),
                    NativeBinding.of(
                        "int8.to.bin",
                        mapOverride(BIN, asInt(false), value -> new byte[] {value.byteValue()})),
                    NativeBinding.of(
                        "int16.to.bin",
                        mapOverride(
                            BIN,
                            asInt(false),
                            (value, bigEndian) ->
                                ByteBuffer.allocate(Short.BYTES)
                                    .order(
                                        bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                                    .putShort(value.shortValue())
                                    .array(),
                            asBool(false),
                            "big_endian")),
                    NativeBinding.of(
                        "int32.to.bin",
                        mapOverride(
                            BIN,
                            asInt(false),
                            (value, bigEndian) ->
                                ByteBuffer.allocate(Integer.BYTES)
                                    .order(
                                        bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                                    .putInt(value.intValue())
                                    .array(),
                            asBool(false),
                            "big_endian")),
                    NativeBinding.of(
                        "int64.to.bin",
                        mapOverride(
                            BIN,
                            asInt(false),
                            (value, bigEndian) ->
                                ByteBuffer.allocate(Long.BYTES)
                                    .order(
                                        bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                                    .putLong(value)
                                    .array(),
                            asBool(false),
                            "big_endian")),
                    NativeBinding.of(
                        "int.to.str",
                        mapOverride(
                            STR,
                            asInt(false),
                            Str::from,
                            asBool(false),
                            "hex",
                            asInt(false),
                            "digits")),
                    NativeBinding.of(
                        "parse.float", mapOverride(FLOAT, asString(false), Double::parseDouble)),
                    NativeBinding.of(
                        "parse.int",
                        mapOverride(
                            INT,
                            asString(false),
                            (x, radix) -> Long.parseLong(x, radix.intValue()),
                            asInt(false),
                            "radix")),
                    NativeBinding.of("random.bool", UtilRandomGenerator.BOOL.create()),
                    NativeBinding.of("random.float", UtilRandomGenerator.FLOAT.create()),
                    NativeBinding.of("random.gaussian", UtilRandomGenerator.GAUSSIAN.create()),
                    NativeBinding.of("random.int", UtilRandomGenerator.INT.create()),
                    NativeBinding.of(
                        "str.decode.punycode",
                        mapOverride(
                            STRING,
                            asString(false),
                            (x, allowUnassigned) ->
                                IDN.toUnicode(x, allowUnassigned ? IDN.ALLOW_UNASSIGNED : 0),
                            asBool(false),
                            "allow_unassigned")),
                    NativeBinding.of(
                        "str.encode.punycode",
                        mapOverride(
                            STRING,
                            asString(false),
                            (x, allowUnassigned, strictAscii) ->
                                IDN.toASCII(
                                    x,
                                    (allowUnassigned ? IDN.ALLOW_UNASSIGNED : 0)
                                        | (strictAscii ? IDN.USE_STD3_ASCII_RULES : 0)),
                            asBool(false),
                            "allow_unassigned",
                            asBool(false),
                            "strict_ascii")),
                    NativeBinding.of(
                        "str.escape",
                        function(
                            asTemplate("escape builder"),
                            inputs -> {
                              final var ranges = new ArrayList<UtilEscape.Range>();
                              final var singleSubstitutions = new TreeMap<Integer, String>();
                              final var namedSubstitutions = new TreeMap<String, List<Integer>>();
                              for (final var input : inputs.values()) {
                                input.accept(ranges, namedSubstitutions, singleSubstitutions);
                              }
                              ranges.sort(null);
                              return AttributeSource.of(
                                  Attribute.of(
                                      "value",
                                      UtilEscape.create(
                                          singleSubstitutions,
                                          namedSubstitutions,
                                          ranges.stream())));
                            },
                            AnyConverter.frameOf(
                                AnyConverter.asProxy(
                                    EscapeTransformation.class,
                                    false,
                                    SpecialLocation.library("utils")
                                        .attributes("str_transform")
                                        .instantiated()),
                                false),
                            "arg_values")),
                    NativeBinding.of(
                        "str.escape.char",
                        function(
                            NativeBinding.<EscapeTransformation>asProxy(),
                            (character, replacement) ->
                                (ranges, namedSubstitutions, singleSubstitutions) ->
                                    singleSubstitutions.put(character, replacement),
                            AnyConverter.asCodepoint(),
                            "char",
                            AnyConverter.asString(false),
                            "replacement")),
                    NativeBinding.of(
                        "str.escape.lookup",
                        function(
                            NativeBinding.<EscapeTransformation>asProxy(),
                            (character, name) ->
                                (ranges, namedSubstitutions, singleSubstitutions) ->
                                    namedSubstitutions
                                        .computeIfAbsent(name, k -> new ArrayList<>())
                                        .add(character),
                            AnyConverter.asCodepoint(),
                            "char",
                            AnyConverter.asString(false),
                            "name")),
                    NativeBinding.of("str.escape.range", UtilEscapeRangeBuilder.DEFINITION),
                    NativeBinding.of(
                        "str.find",
                        mapFunction(
                            INT,
                            asStr(false),
                            Str::find,
                            asString(false),
                            "str",
                            asInt(false),
                            "start",
                            asBool(false),
                            "backward")),
                    NativeBinding.of(
                        "str.from.codepoint", mapOverride(STR, asInt(false), Str::fromCodepoint)),
                    NativeBinding.of(
                        "str.from.system",
                        mapOverride(
                            STRING, asBin(false), x -> new String(x, Charset.defaultCharset()))),
                    NativeBinding.of(
                        "str.from.utf16be",
                        mapOverride(
                            STRING, asBin(false), x -> new String(x, StandardCharsets.UTF_16BE))),
                    NativeBinding.of(
                        "str.from.utf16le",
                        mapOverride(
                            STRING, asBin(false), x -> new String(x, StandardCharsets.UTF_16LE))),
                    NativeBinding.of(
                        "str.from.utf32be",
                        mapOverride(
                            STRING, asBin(false), x -> new String(x, Charset.forName("UTF-32BE")))),
                    NativeBinding.of(
                        "str.from.utf32le",
                        mapOverride(
                            STRING, asBin(false), x -> new String(x, Charset.forName("UTF-32LE")))),
                    NativeBinding.of(
                        "str.from.utf8",
                        mapFunction(
                            STRING, asBin(false), x -> new String(x, StandardCharsets.UTF_8))),
                    NativeBinding.of(
                        "str.length.utf16", mapFunction(INT, asStr(false), Str::length16)),
                    NativeBinding.of(
                        "str.length.utf8", mapFunction(INT, asStr(false), Str::length8)),
                    NativeBinding.of(
                        "str.lower_case",
                        mapFunction(STRING, asString(false), String::toLowerCase)),
                    NativeBinding.of(
                        "str.prefixed",
                        mapFunction(
                            BOOL, asString(false), String::startsWith, asString(false), "str")),
                    NativeBinding.of(
                        "str.replace",
                        mapFunction(
                            STRING,
                            asString(false),
                            String::replace,
                            asString(false),
                            "str",
                            asString(false),
                            "with")),
                    NativeBinding.of(
                        "str.slice",
                        mapFunction(
                            STRING,
                            asStr(false),
                            Str::slice,
                            asInt(false),
                            "start",
                            asInt(true),
                            "end",
                            asInt(true),
                            "length")),
                    NativeBinding.of(
                        "str.suffixed",
                        mapFunction(
                            BOOL, asString(false), String::endsWith, asString(false), "str")),
                    NativeBinding.of("str.to.categories", UtilCharacterCategory.DEFINITION),
                    NativeBinding.of(
                        "str.to.codepoints",
                        mapOverride(
                            FRAME_BY_ATTRIBUTES,
                            AnyConverter.asString(false),
                            input ->
                                AttributeSource.listOfAny(
                                    input.codePoints().asLongStream().mapToObj(Any::of)))),
                    NativeBinding.of(
                        "str.to.system",
                        mapOverride(
                            BIN, asString(false), x -> x.getBytes(Charset.defaultCharset()))),
                    NativeBinding.of(
                        "str.to.utf16be", mapOverride(BIN, asStr(false), x -> x.toUtf16(true))),
                    NativeBinding.of(
                        "str.to.utf16le", mapOverride(BIN, asStr(false), x -> x.toUtf16(false))),
                    NativeBinding.of(
                        "str.to.utf32be", mapOverride(BIN, asStr(false), x -> x.toUtf32(true))),
                    NativeBinding.of(
                        "str.to.utf32le", mapOverride(BIN, asStr(false), x -> x.toUtf32(false))),
                    NativeBinding.of("str.to.utf8", mapOverride(BIN, asStr(false), Str::toUtf8)),
                    NativeBinding.of(
                        "str.trim", mapOverride(STRING, asString(false), String::strip)),
                    NativeBinding.of(
                        "str.upper_case",
                        mapOverride(STRING, asString(false), String::toUpperCase)),
                    NativeBinding.of("trace", UtilTraceRenderer.DEFINITION)),
                UtilEscape.createUnicodeActions())),
        NativeBinding.create(
            "zip",
            NativeBinding.of(
                "archive",
                function(
                    BIN,
                    files -> {
                      try (final var buffer = new ByteArrayOutputStream();
                          final var zip = new ZipOutputStream(buffer)) {
                        for (final var file : files.values()) {
                          file.write(zip);
                        }
                        return buffer.toByteArray();
                      }
                    },
                    AnyConverter.frameOf(
                        AnyConverter.asProxy(
                            BaseZipFile.class,
                            false,
                            SpecialLocation.library("zip")
                                .choose("file_tmpl", "symlink_tmpl")
                                .instantiated()),
                        false),
                    "args")),
            NativeBinding.of("file", ZipFile.DEFINITION),
            NativeBinding.of("symlink", ZipSymlink.DEFINITION))
      };
  private static final UriHandler[] PUBLIC_HANDLERS =
      new UriHandler[] {
        HandlerHttp.INSTANCE, HandlerSettings.INSTANCE, HandlerEnvironment.INSTANCE
      };
  private static final UriService[] URI_SERVICES =
      new UriService[] {
        (finder, flags) -> Stream.of(INTEROP_HANDLERS),
        (finder, flags) -> Stream.of(new HandlerCurrentInformation(flags)),
        (finder, flags) ->
            flags.test(ServiceFlag.SANDBOXED) ? Stream.empty() : Stream.of(PUBLIC_HANDLERS),
        new ServiceFile(),
        new ServiceFtp(),
        new ServiceResource(),
        new ServiceSqlLiteFile(),
        new ServiceSqlLiteResource(),
        new ServiceSqlMsSqlServer(),
        new ServiceSqlMySql(),
        new ServiceSqlOracleHost(),
        new ServiceSqlOracleTns(),
        new ServiceSqlPostgres()
      };

  private static WhinyFunction<byte[], byte[]> checksum(String algorithm) {
    return input -> MessageDigest.getInstance(algorithm).digest(input);
  }

  /**
   * Create a new standard URI service.
   *
   * <p>All instances are interchangable.
   */
  public StandardUriService() {}

  /**
   * Get standard URI schemes included in the standard library. This includes SQL drivers, HTTP
   * URLs, FTP URLs, file URLs, resource URLs, JVM settings URLs, UNIX environment variable URLs,
   * and native: URLs for lookup, math, sql, time, utils, and zip libraries.
   *
   * @param finder a utility to find resource files on disk based on the users's setup
   * @param flags the configuration of what kinds of handlers should be included and excluded, based
   */
  @Override
  public Stream<UriHandler> create(ResourcePathFinder finder, Predicate<ServiceFlag> flags) {
    return Stream.of(URI_SERVICES).flatMap(s -> s.create(finder, flags));
  }
}
