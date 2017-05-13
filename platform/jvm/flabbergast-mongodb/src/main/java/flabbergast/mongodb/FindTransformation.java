package flabbergast.mongodb;

import com.mongodb.client.FindIterable;
import org.bson.Document;

interface FindTransformation {
  FindIterable<Document> apply(FindIterable<Document> input);
}
