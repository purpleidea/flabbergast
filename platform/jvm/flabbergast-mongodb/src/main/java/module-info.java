import flabbergast.lang.UriService;

module flabbergast.mongodb {
  requires flabbergast.base;
  requires org.mongodb.bson;
  requires org.mongodb.driver.core;
  requires org.mongodb.driver.sync.client;

  provides UriService with
      flabbergast.mongodb.MongoUriService;
}
