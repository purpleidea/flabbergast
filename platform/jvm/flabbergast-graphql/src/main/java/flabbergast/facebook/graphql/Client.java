package flabbergast.facebook.graphql;

import java.net.URI;

final class Client {
  private final URI uri;

  Client(String uri) {
    this.uri = URI.create(uri);
  }

  URI uri() {
    return uri;
  }
}
