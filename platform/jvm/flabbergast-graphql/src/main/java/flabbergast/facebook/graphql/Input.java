package flabbergast.facebook.graphql;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

interface Input {

  void write(ObjectNode writer, String name);

  void write(ArrayNode writer);
}
