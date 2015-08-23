package flabbergast;

import java.lang.Iterable;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class ApiGenerator {
	public static ApiGenerator create(String library_name, String github)
			throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		Document doc = docBuilder.newDocument();
		doc.setXmlVersion("1.0");
		doc.appendChild(doc.createProcessingInstruction("xml-stylesheet",
				"href=\"o_0.xsl\" type=\"text/xsl\""));
		Element node = doc.createElementNS("http://flabbergast.org/api",
				"o_0:lib");
		node.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");
		node.setAttribute("name", library_name);
		if (github != null) {
			node.setAttribute("github", github + "/" + library_name + ".o_0");
		}
		doc.appendChild(node);
		return new ApiGenerator(doc, node);
	}
	private final Node node;
	private final Document document;
	private Element _desc = null;

	private final Map<Environment, Boolean> environments = new HashMap<Environment, Boolean>();
	private final Map<String, Node> refs = new HashMap<String, Node>();
	private final Map<String, Node> uses = new HashMap<String, Node>();

	private ApiGenerator(Document doc, Node node) {
		document = doc;
		this.node = node;
	}

	public void appendDescriptionText(String text) {
		Node node = document.createTextNode(text);
		getDescription().appendChild(node);
	}
	public Element appendDescriptionTag(String xmlns, String tag, String text) {
		Element node = document.createElementNS(xmlns, tag);
		node.appendChild(document.createTextNode(text));
		getDescription().appendChild(node);
		return node;
	}
	public void collectEnvironment(Environment environment) {
		if (!environments.containsKey(environment)) {
			environments.put(environment, true);
			environment.collectUses(this);
		}
	}
	public ApiGenerator createChild(String name, CodeRegion region,
			TypeSet type, boolean informative) {
		Element node = document.createElementNS(document.getDocumentElement()
				.getNamespaceURI(), "o_0:attr");
		node.setAttribute("name", name);
		node.setAttribute("startline", Integer.toString(region.getStartRow()));
		node.setAttribute("startcol", Integer.toString(region.getStartColumn()));
		node.setAttribute("endline", Integer.toString(region.getEndRow()));
		node.setAttribute("endcol", Integer.toString(region.getEndColumn()));
		node.setAttribute("informative", informative ? "true" : "false");
		this.node.appendChild(node);
		if (!type.hasAll()) {
			for (Type t : Type.values()) {
				if (!type.contains(t))
					continue;
				Element type_node = document.createElementNS(document
						.getDocumentElement().getNamespaceURI(), "o_0:type");
				type_node.appendChild(document.createTextNode(t == Type.Unit
						? "Null"
						: t.toString()));
				node.appendChild(type_node);
			}
		}
		return new ApiGenerator(document, node);
	}
	private Element getDescription() {
		if (_desc == null) {
			_desc = document.createElementNS(getDocument().getDocumentElement()
					.getNamespaceURI(), "o_0:description");
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
		Element node = document.createElementNS(document.getDocumentElement()
				.getNamespaceURI(), tag);
		node.appendChild(document.createTextNode(content));
		known.put(tag, node);
		this.node.appendChild(node);
	}
	public void registerRef(String uri) {
		if (uri.startsWith("lib:")) {
			register(refs, "o_0:ref", uri.substring(4));
		}
	}
	public void registerUse(Iterable<? extends Object> names) {
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
		registerUse(sb.toString());
	}
	public void registerUse(String name) {
		register(uses, "o_0:use", name);
	}
}
