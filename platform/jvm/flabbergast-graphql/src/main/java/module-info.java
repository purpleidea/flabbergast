import flabbergast.facebook.graphql.GraphQLUriService;
import flabbergast.lang.UriService;

/** Flabbergast library and native bindings for GraphQL interface */
module flabbergast.graphql {
  requires flabbergast.base;
  requires java.net.http;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;

  provides UriService with
      GraphQLUriService;
}
