module flabbergast.kubernetes {
  provides flabbergast.lang.UriService with
      flabbergast.kubernetes.KubernetesUriService;

  requires flabbergast.base;
  requires kubernetes.client;
  requires kubernetes.model;
}
