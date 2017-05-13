package flabbergast.mongodb;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.bson.Document;
import org.bson.conversions.Bson;

class FindOperation extends BaseOperation {
  static final Definition DEFINITION =
      LookupAssistant.create(
          FindOperation::new,
          Stream.concat(
              assistants(),
              Stream.of(
                  LookupAssistant.find(
                      AnyConverter.frameOf(
                          AnyConverter.asProxy(
                              FindTransformation.class,
                              false,
                              SpecialLocation.library("mongodb")
                                  .attributes("operations")
                                  .any()
                                  .invoked()),
                          false),
                      (instance1, operations1) ->
                          instance1.operations = new ArrayList<>(operations1.values()),
                      "args"),
                  LookupAssistant.find(
                      MongoUriService.BSON_ANY_CONVERTER,
                      (instance, criteria) -> instance.criteria = criteria,
                      "criteria"),
                  LookupAssistant.find(
                      AnyConverter.asString(false),
                      (instance, collection) -> instance.collection = collection,
                      "collection"))));
  String collection;
  Bson criteria;
  List<FindTransformation> operations;

  @Override
  protected MongoIterable<Document> run(MongoDatabase database) {
    var iterable = database.getCollection(collection).find(criteria);
    for (final FindTransformation operation : operations) {
      iterable = operation.apply(iterable);
    }
    return iterable;
  }
}
