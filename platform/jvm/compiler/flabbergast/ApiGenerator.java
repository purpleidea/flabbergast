package flabbergast;

import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class ApiGenerator {
  public static ApiGenerator create(String library_name, String github)
      throws ParserConfigurationException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

    Document doc = docBuilder.newDocument();
    doc.setXmlVersion("1.0");
    doc.appendChild(
        doc.createProcessingInstruction("xml-stylesheet", "href=\"o_0.xsl\" type=\"text/xsl\""));
    Element node = doc.createElementNS("http://flabbergast.org/api", "o_0:lib");
    node.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");
    node.setAttributeNS(node.getNamespaceURI(), "name", library_name);
    if (github != null) {
      node.setAttributeNS(node.getNamespaceURI(), "github", github + "/" + library_name + ".o_0");
    }
    doc.appendChild(node);
    return new ApiGenerator(doc, node, new String[0]);
  }

  private Element _desc = null;
  private final Document document;
  private final Map<Environment, Boolean> environments = new HashMap<Environment, Boolean>();

  private final String[] names;
  private final Node node;
  private final Map<String, Node> refs = new HashMap<String, Node>();
  private final Map<String, Node> uses = new HashMap<String, Node>();

  private ApiGenerator(Document doc, Node node, String[] names) {
    document = doc;
    this.node = node;
    this.names = names;
  }

  public Element appendDescriptionTag(String xmlns, String tag, String text) {
    Element node = document.createElementNS(xmlns, tag);
    node.appendChild(document.createTextNode(text));
    getDescription().appendChild(node);
    return node;
  }

  public void appendDescriptionText(String text) {
    Node node = document.createTextNode(text);
    getDescription().appendChild(node);
  }

  public void collectEnvironment(Environment environment) {
    if (!environments.containsKey(environment)) {
      environments.put(environment, true);
      environment.collectUses(this);
    }
  }

  public ApiGenerator createChild(
      String name, CodeRegion region, TypeSet type, boolean informative) {
    Element node =
        document.createElementNS(document.getDocumentElement().getNamespaceURI(), "o_0:attr");
    node.setAttributeNS(document.getDocumentElement().getNamespaceURI(), "name", name);
    node.setAttributeNS(
        document.getDocumentElement().getNamespaceURI(),
        "startline",
        Integer.toString(region.getStartRow()));
    node.setAttributeNS(
        document.getDocumentElement().getNamespaceURI(),
        "startcol",
        Integer.toString(region.getStartColumn()));
    node.setAttributeNS(
        document.getDocumentElement().getNamespaceURI(),
        "endline",
        Integer.toString(region.getEndRow()));
    node.setAttributeNS(
        document.getDocumentElement().getNamespaceURI(),
        "endcol",
        Integer.toString(region.getEndColumn()));
    node.setAttributeNS(
        document.getDocumentElement().getNamespaceURI(),
        "informative",
        informative ? "true" : "false");
    this.node.appendChild(node);

    String base_name = name;
    int it = 0;
    do {
      Element def_node =
          document.createElementNS(document.getDocumentElement().getNamespaceURI(), "o_0:def");
      def_node.appendChild(document.createTextNode(base_name));
      node.appendChild(def_node);
      if (it < names.length) {
        base_name = names[it] + "." + base_name;
      }
    } while (it++ < names.length);
    String[] new_names;
    if (!type.contains(Type.Template)) {
      new_names = new String[0];
    } else {
      new_names = new String[names.length + 1];
      System.arraycopy(names, 0, new_names, 0, names.length);
      new_names[names.length] = name;
    }

    if (!type.hasAll()) {
      for (Type t : Type.values()) {
        if (!type.contains(t)) {
          continue;
        }
        Element type_node =
            document.createElementNS(document.getDocumentElement().getNamespaceURI(), "o_0:type");
        type_node.appendChild(document.createTextNode(t == Type.Unit ? "Null" : t.toString()));
        node.appendChild(type_node);
      }
    }
    return new ApiGenerator(document, node, new String[0]);
  }

  private Element getDescription() {
    if (_desc == null) {
      _desc =
          document.createElementNS(
              getDocument().getDocumentElement().getNamespaceURI(), "o_0:description");
      node.appendChild(_desc);
    }
    return _desc;
  }

  public Document getDocument() {
    return document;
  }

  private void register(Map<String, Node> known, String tag, String content) {
    if (known.containsKey(content)) {
      return;
    }
    Element node = document.createElementNS(document.getDocumentElement().getNamespaceURI(), tag);
    node.appendChild(document.createTextNode(content));
    known.put(content, node);
    this.node.appendChild(node);
  }

  public void registerRef(String uri) {
    if (uri.startsWith("lib:")) {
      register(refs, "o_0:ref", uri.substring(4));
    }
  }

  public void registerUse(Iterable<? extends Object> names, String... suffixes) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Object name : names) {
      if (first) {
        first = false;
      } else {
        sb.append(".");
      }
      sb.append(name.toString());
    }
    if (suffixes.length == 0) {
      registerUse(sb.toString());
    } else {
      if (sb.length() > 0) {
        sb.append(".");
      }
      String prefix = sb.toString();
      for (String suffix : suffixes) {
        registerUse(prefix + suffix);
      }
    }
  }

  public void registerUse(String name) {
    register(uses, "o_0:use", name);
  }
}
