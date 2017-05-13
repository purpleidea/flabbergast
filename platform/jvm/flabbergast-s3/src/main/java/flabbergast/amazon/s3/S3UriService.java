package flabbergast.amazon.s3;

import static flabbergast.export.NativeBinding.function;

import flabbergast.export.NativeBinding;
import flabbergast.lang.*;
import flabbergast.util.Result;
import java.net.URI;
import java.time.ZoneId;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/** Access an S3 API endpoint in Flabbergast */
public final class S3UriService implements UriService {
  private static final Pattern AMPERSAND = Pattern.compile("&");
  private static final Pattern EQUALSIGN = Pattern.compile("=");
  private static final UriHandler INSTANCE =
      new UriHandler() {
        @Override
        public String name() {
          return "S3 Endpoints";
        }

        @Override
        public int priority() {
          return 0;
        }

        @Override
        public Result<Promise<Any>> resolveUri(UriExecutor runner, URI uri) {
          if (!uri.getScheme().equals("s3")) {
            return Result.empty();
          }
          try {
            final var endpoint =
                new URI("http", null, uri.getHost(), uri.getPort(), "", null, null);
            final var region =
                (uri.getPath() == null || uri.getPath().equals("") || uri.getPath().equals("/")
                    ? Region.US_EAST_1
                    : Region.of(uri.getPath().substring(1)));
            String accessKey = null;
            String secretKey = null;
            var overrideSigner = false;

            for (final var paramChunk : AMPERSAND.split(uri.getQuery())) {
              final var parts = EQUALSIGN.split(paramChunk, 2);
              if (parts.length != 2) {
                return Result.error(String.format("Invalid URL parameter “%s”.", paramChunk));
              }
              switch (parts[0]) {
                case "accesskey":
                  accessKey = parts[1];
                  break;
                case "secretkey":
                  secretKey = parts[1];
                  break;
                case "overridesigner":
                  overrideSigner = parts[1].equals("true");
                  break;
                default:
                  return Result.error(String.format("Unknown URL parameter “%s”.", parts[0]));
              }
            }
            if (accessKey == null || secretKey == null) {
              return Result.error(
                  String.format(
                      "Parameters “accesskey” and “secretkey” must be provided for URL: %s", uri));
            }
            final var clientConfiguration = ClientOverrideConfiguration.builder();
            if (overrideSigner) {
              clientConfiguration.putAdvancedOption(
                  SdkAdvancedClientOption.SIGNER, Aws4Signer.create());
            }
            final var s3Client =
                S3Client.builder()
                    .endpointOverride(endpoint)
                    .region(region)
                    .overrideConfiguration(clientConfiguration.build())
                    .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .credentialsProvider(
                        StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
            return Result.of(
                Any.of(
                    Frame.proxyOf(
                        "s3_" + s3Client.hashCode(),
                        uri.toString(),
                        s3Client,
                        Stream.of(ProxyAttribute.fixed("uri", Any.of(uri.toString()))))));
          } catch (Exception e) {
            return Result.error(e.getMessage());
          }
        }
      };
  static final AnyConverter<S3Client> S_3_CONNECTION_ANY_CONVERTER =
      AnyConverter.asProxy(S3Client.class, false, SpecialLocation.uri("s3"));
  private static final UriHandler INTEROP =
      NativeBinding.create(
          "amazon/s3",
          NativeBinding.of(
              "get_bucket_location",
              function(
                  NativeBinding.STRING,
                  (client, bucket) ->
                      client
                          .getBucketLocation(
                              GetBucketLocationRequest.builder().bucket(bucket).build())
                          .locationConstraintAsString(),
                  S_3_CONNECTION_ANY_CONVERTER,
                  "s3_connection",
                  AnyConverter.asString(false),
                  "bucket")),
          NativeBinding.of(
              "get_object",
              function(
                  NativeBinding.BIN,
                  (client, bucket, path) ->
                      client
                          .getObject(
                              GetObjectRequest.builder().bucket(bucket).key(path).build(),
                              ResponseTransformer.toBytes())
                          .asByteArray(),
                  S_3_CONNECTION_ANY_CONVERTER,
                  "s3_connection",
                  AnyConverter.asString(false),
                  "bucket",
                  AnyConverter.asString(false),
                  "path")),
          NativeBinding.of(
              "list_buckets",
              function(
                  NativeBinding.FRAME_BY_ATTRIBUTES,
                  connection ->
                      AttributeSource.listOfDefinition(
                          connection
                              .listBuckets()
                              .buckets()
                              .stream()
                              .map(
                                  bucket ->
                                      Template.instantiate(
                                          AttributeSource.of(
                                              Attribute.of("name", Any.of(bucket.name())),
                                              Attribute.of(
                                                  "creation_time",
                                                  Frame.of(
                                                      bucket
                                                          .creationDate()
                                                          .atZone(ZoneId.of("Z"))))),
                                          "S3 list buckets",
                                          "s3",
                                          "bucket_tmpl"))),
                  S3UriService.S_3_CONNECTION_ANY_CONVERTER,
                  "s3_connection")));

  @Override
  public Stream<UriHandler> create(ResourcePathFinder finder, Predicate<ServiceFlag> flags) {
    return flags.test(ServiceFlag.SANDBOXED) ? Stream.of(INTEROP) : Stream.of(INTEROP, INSTANCE);
  }
}
