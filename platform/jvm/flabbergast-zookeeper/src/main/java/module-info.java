import flabbergast.apache.zookeeper.ZooKeeperUriService;
import flabbergast.lang.UriService;

module flabbergast.zookeeper {
  requires flabbergast.base;

  provides UriService with
      ZooKeeperUriService;
  // TODO: https://issues.apache.org/jira/browse/ZOOKEEPER-3625
  requires zookeeper;
  requires zookeeper.jute;
}
