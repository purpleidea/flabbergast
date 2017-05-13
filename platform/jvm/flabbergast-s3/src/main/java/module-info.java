import flabbergast.lang.UriService;

module flabbergast.sthree {
  requires flabbergast.base;
  requires software.amazon.awssdk.auth;
  requires software.amazon.awssdk.regions;
  requires software.amazon.awssdk.services.s3;
  requires software.amazon.awssdk.core;

  provides UriService with
      flabbergast.amazon.s3.S3UriService;
}
