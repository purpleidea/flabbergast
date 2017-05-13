package flabbergast.mongodb;

import com.mongodb.client.MongoIterable;
import flabbergast.lang.Any;
import flabbergast.lang.Context;
import flabbergast.lang.Future;
import flabbergast.lang.SourceReference;
import org.bson.Document;

interface ResultSelector {
  Any process(
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      MongoIterable<Document> iterable,
      OutputConverter converter);
}
