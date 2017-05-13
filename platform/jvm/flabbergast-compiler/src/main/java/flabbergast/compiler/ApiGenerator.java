package flabbergast.compiler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class ApiGenerator {
  private static class AccumulateNames implements Function<String, String> {
    String base;

    @Override
    public String apply(String input) {
      if (base == null) {
        base = input;
      } else {
        base = base + "." + input;
      }
      return base;
    }
  }

  public static ApiGenerator create(String library_name, String github)
      throws ParserConfigurationException {
    final var docFactory = DocumentBuilderFactory.newInstance();
    final var docBuilder = docFactory.newDocumentBuilder();

    final var doc = docBuilder.newDocument();
    doc.setXmlVersion("1.0");
    doc.appendChild(
        doc.createProcessingInstruction("xml-stylesheet", "href=\"o_0.xsl\" type=\"text/xsl\""));
    final var node = doc.createElementNS("http://flabbergast.org/api", "o_0:lib");
    node.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");
    node.setAttributeNS(node.getNamespaceURI(), "o_0:name", library_name);
    if (github != null) {
      node.setAttributeNS(
          node.getNamespaceURI(), "o_0:github", github + "/" + library_name + ".o_0");
    }
    doc.appendChild(node);
    return new ApiGenerator(doc, node, new String[0]);
  }

  private Element description = null;
  private final Document document;

  private final Map<String, Node> interops = new HashMap<>();
  private final String[] names;
  private final Node node;
  private final Map<String, Node> refs = new HashMap<>();
  private final Map<String, Node> uses = new HashMap<>();

  private ApiGenerator(Document doc, Node node, String[] names) {
    document = doc;
    this.node = node;
    this.names = names;
  }

  public Element appendDescriptionTag(String xmlns, String tag, String text) {
    final var node = document.createElementNS(xmlns, tag);
    node.appendChild(document.createTextNode(text));
    getDescription().appendChild(node);
    return node;
  }

  public void appendDescriptionText(String text) {
    final Node node = document.createTextNode(text);
    getDescription().appendChild(node);
  }

  public ApiGenerator createChild(
      String name, SourceLocation location, AttributeFlavour flavour, String... types) {
    final var node =
        document.createElementNS(document.getDocumentElement().getNamespaceURI(), "o_0:attr");
    node.setAttributeNS(document.getDocumentElement().getNamespaceURI(), "o_0:name", name);
    node.setAttributeNS(
        document.getDocumentElement().getNamespaceURI(),
        "o_0:startline",
        Integer.toString(location.getStartLine()));
    node.setAttributeNS(
        document.getDocumentElement().getNamespaceURI(),
        "o_0:startcol",
        Integer.toString(location.getStartColumn()));
    node.setAttributeNS(
        document.getDocumentElement().getNamespaceURI(),
        "o_0:endline",
        Integer.toString(location.getEndLine()));
    node.setAttributeNS(
        document.getDocumentElement().getNamespaceURI(),
        "o_0:endcol",
        Integer.toString(location.getEndColumn()));
    node.setAttributeNS(
        document.getDocumentElement().getNamespaceURI(), "o_0:flavour", flavour.symbol());
    this.node.appendChild(node);

    Stream.of(names)
        .map(new AccumulateNames())
        .map(base -> base + "." + name)
        .forEach(
            defName -> {
              final var defNode =
                  document.createElementNS(
                      document.getDocumentElement().getNamespaceURI(), "o_0:def");
              defNode.appendChild(document.createTextNode(defName));
              node.appendChild(defNode);
            });
    String[] newNames;
    if (Arrays.asList(types).contains("Template")) {
      newNames = new String[0];
    } else {
      newNames = Stream.concat(Stream.of(name), Stream.of(names)).toArray(String[]::new);
    }

    for (final var type : types) {
      final var type_node =
          document.createElementNS(document.getDocumentElement().getNamespaceURI(), "o_0:type");
      type_node.appendChild(document.createTextNode(type));
      node.appendChild(type_node);
    }
    return new ApiGenerator(document, node, newNames);
  }

  private Element getDescription() {
    if (description == null) {
      description =
          document.createElementNS(
              getDocument().getDocumentElement().getNamespaceURI(), "o_0:description");
      node.appendChild(description);
    }
    return description;
  }

  public Document getDocument() {
    return document;
  }

  private void register(Map<String, Node> known, String tag, String content) {
    if (known.containsKey(content)) {
      return;
    }
    final var node = document.createElementNS(document.getDocumentElement().getNamespaceURI(), tag);
    node.appendChild(document.createTextNode(content));
    known.put(content, node);
    this.node.appendChild(node);
  }

  public void registerRef(String uri) {
    if (uri.startsWith("lib:")) {
      register(refs, "o_0:ref", uri.substring(4));
    }
    if (uri.startsWith("native:")) {
      register(interops, "o_0:native", uri.substring(4));
    }
  }

  public void registerUse(Stream<String> names, String... suffixes) {
    final var baseName = names.collect(Collectors.joining("."));
    if (suffixes.length == 0) {
      registerUse(baseName);
    } else {
      var stream = Stream.of(suffixes);
      if (baseName.length() > 0) {
        stream = stream.map(suffix -> baseName + "." + suffix);
      }
      stream.forEach(this::registerUse);
    }
  }

  public void registerUse(String name) {
    register(uses, "o_0:use", name);
  }
}
