/** Support for etcd configuration system */
module flabbergast.etcd {
  provides flabbergast.lang.UriService with
      flabbergast.coreos.etcd.EtcdUriService;

  requires flabbergast.base;
  requires jetcd.core;
}
