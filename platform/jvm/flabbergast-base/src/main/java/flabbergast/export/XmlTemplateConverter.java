package flabbergast.export;

import static flabbergast.lang.AttributeSource.toSource;

import flabbergast.lang.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

final class XmlTemplateConverter extends DefaultHandler {
  private static final Pattern COLON = Pattern.compile(":");
  private final Deque<Attributes> attributes = new ArrayDeque<>();
  private final Deque<List<Attribute>> current = new ArrayDeque<>();
  private boolean ok = true;
  Definition result;

  @Override
  public void characters(char[] ch, int start, int length) {
    push(Attribute::of, Any.of(new String(ch, start, length)));
  }

  private Stream<Definition> convert(
      Attributes attributes, String uri, Function<String, String> determineNsUri) {
    return IntStream.range(0, attributes.getLength())
        .filter(
            i ->
                !attributes.getLocalName(i).equals("xmlns")
                    && !attributes.getQName(i).startsWith("xmlns:"))
        .mapToObj(
            i -> {
              final var xmlns = COLON.split(attributes.getQName(i));
              return Template.instantiate(
                  AttributeSource.of(
                      Attribute.of("attr_name", Any.of(attributes.getLocalName(i))),
                      Attribute.of(
                          "xml_ns",
                          Any.of(xmlns.length == 1 ? uri : determineNsUri.apply(xmlns[0]))),
                      Attribute.of("attr_value", Any.of(attributes.getValue(i)))),
                  "XML attribute",
                  "xml",
                  "attribute");
            });
  }

  @Override
  public void endDocument() {
    if (ok) {
      result =
          Template.instantiate(
              AttributeSource.of(current.removeLast()), "XML document", "xml", "document");
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    final var currentAttributes = attributes.removeLast();
    var nsDefs =
        IntStream.range(0, currentAttributes.getLength())
            .filter(
                i ->
                    currentAttributes.getLocalName(i).equals("xmlns")
                        || currentAttributes.getQName(i).startsWith("xmlns:"))
            .boxed()
            .collect(
                Collectors.toMap(
                    currentAttributes::getValue,
                    i -> {
                      if (currentAttributes.getLocalName(i).equals("xmlns")) {
                        return "";
                      }
                      return COLON.split(currentAttributes.getQName(i), 2)[1];
                    }));
    final var xmlns = COLON.split(qName, 2);
    final var definition =
        Template.instantiate(
            AttributeSource.of(
                Attribute.of("children", Frame.define(AttributeSource.of(current.removeLast()))),
                Attribute.of("node_name", Any.of(localName)),
                Attribute.of("xml_ns", Any.of(xmlns.length == 1 ? "" : nsDefs.get(xmlns[0]))),
                Attribute.of(
                    "attributes",
                    Frame.define(
                        AttributeSource.list(
                            Attribute::of, convert(currentAttributes, uri, nsDefs::get)))),
                Attribute.of(
                    "namespaces",
                    Frame.define(
                        nsDefs
                            .entrySet()
                            .stream()
                            .map(e -> Attribute.of(e.getKey(), Any.of(e.getValue())))
                            .collect(toSource())))),
            "XML element",
            "xml",
            "element");
    push(Attribute::of, definition);
  }

  @Override
  public void error(SAXParseException e) {
    result = Definition.error(e.getLocalizedMessage());
    ok = false;
  }

  @Override
  public void fatalError(SAXParseException e) {
    result = Definition.error(e.getLocalizedMessage());
    ok = false;
  }

  @Override
  public void processingInstruction(String target, String data) {
    push(
        Attribute::of,
        Template.instantiate(
            AttributeSource.of(
                Attribute.of("target", Any.of(target)), Attribute.of("parameters", Any.of(data))),
            "XML processing instruction",
            "xml",
            "processing_instruction"));
  }

  private <T> void push(BiFunction<Long, T, Attribute> make, T value) {
    final var list = current.peekLast();
    list.add(make.apply((long) (list.size() + 1), value));
  }

  @Override
  public void startDocument() {
    current.addLast(new ArrayList<>());
    ok = true;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    current.addLast(new ArrayList<>());
    this.attributes.addLast(attributes);
  }
}
