package flabbergast.mongodb;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import java.util.stream.Stream;
import org.bson.Document;

abstract class BaseOperation implements LookupAssistant.Recipient {
  protected static <T extends BaseOperation> Stream<LookupAssistant<T>> assistants() {
    return Stream.of(
        LookupAssistant.find(
            MongoUriService.DATABASE_ANY_CONVERTER,
            (instance, db) -> instance.database = db,
            "mongo_database"),
        LookupAssistant.find(
            AnyConverter.asProxy(
                ResultSelector.class,
                false,
                SpecialLocation.library("mongodb").attributes("result_selector").any()),
            (instance, selector) -> instance.selector = selector,
            "selector"),
        LookupAssistant.find(
            AnyConverter.asProxy(
                OutputConverter.class,
                false,
                SpecialLocation.library("mongodb").attributes("converter").any().invoked()),
            (instance, converter) -> instance.converter = converter,
            "converter"));
  }

  MongoDatabase database;
  ResultSelector selector;
  OutputConverter converter;

  @Override
  public final void run(Future<Any> future, SourceReference sourceReference, Context context) {
    try {
      future.complete(selector.process(future, sourceReference, context, run(database), converter));
    } catch (Exception e) {
      future.error(sourceReference, e.getMessage());
    }
  }

  protected abstract MongoIterable<Document> run(MongoDatabase database);
}
