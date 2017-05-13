package flabbergast.mongodb;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import flabbergast.export.LookupAssistant;
import flabbergast.lang.AnyConverter;
import flabbergast.lang.Definition;
import flabbergast.lang.Name;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.bson.Document;
import org.bson.conversions.Bson;

class AggregationOperation extends BaseOperation {
  static final Definition DEFINITION =
      LookupAssistant.create(
          AggregationOperation::new,
          Stream.concat(
              assistants(),
              Stream.of(
                  LookupAssistant.find(
                      AnyConverter.frameOf(MongoUriService.BSON_ANY_CONVERTER, false),
                      (instance1, pipeline1) -> instance1.pipeline = pipeline1,
                      "args"),
                  LookupAssistant.find(
                      AnyConverter.asString(true),
                      (instance, collection) -> instance.collection = collection,
                      "collection"))));
  Map<Name, Bson> pipeline;
  String collection;

  @Override
  protected MongoIterable<Document> run(MongoDatabase database) {
    return collection == null
        ? database.aggregate(List.copyOf(pipeline.values()))
        : database.getCollection(collection).aggregate(List.copyOf(pipeline.values()));
  }
}
