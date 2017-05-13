package flabbergast.kubernetes;

import flabbergast.export.NativeBinding;
import flabbergast.lang.*;
import flabbergast.util.Result;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.FileInputStream;
import java.net.URI;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Access an Kubernetes cluster connection in Flabbergast */
public final class KubernetesUriService implements UriService {
  private static final UriHandler INTEROP =
      NativeBinding.create(
          "cncf/kubernetes",
          NativeBinding.of("all", LabelSelection.ALL),
          NativeBinding.of("label_in", LabelSelection.IN_DEFINITION),
          NativeBinding.of("label_not_in", LabelSelection.NOT_IN_DEFINITION),
          NativeBinding.of("label_with", LabelSelection.WITH_DEFINITION),
          NativeBinding.of("label_without", LabelSelection.WITHOUT_DEFINITION),
          NativeBinding.of(
              "list_cluster_role_bindings",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListClusterRoleBindings::new, "kubernetes", "role_binding_tmpl")),
          NativeBinding.of(
              "list_cluster_roles",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListClusterRoles::new, "kubernetes", "role_tmpl")),
          NativeBinding.of(
              "list_config_maps",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListConfigMaps::new, "kubernetes", "config_map_tmpl")),
          NativeBinding.of(
              "list_daemon_sets",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListDaemonSets::new, "kubernetes", "daemon_set_tmpl")),
          NativeBinding.of(
              "list_deployments",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListDeployments::new, "kubernetes", "deployment_tmpl")),
          NativeBinding.of(
              "list_endpoints",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListEndpoints::new, "kubernetes", "endpoint_tmpl")),
          NativeBinding.of(
              "list_horizontal_pod_auto_scalers",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListHorizontalPodAutoScalers::new,
                  "kubernetes",
                  "horizontal_pod_auto_scalers_tmpl")),
          NativeBinding.of(
              "list_limit_ranges",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListSecrets::new, "kubernetes", "limit_range_tmpl")),
          NativeBinding.of(
              "list_namespaces",
              BaseKubernetesListOperation.make(
                  ListNamespaces::new, Stream.empty(), "kubernetes", "namespace_tmpl")),
          NativeBinding.of(
              "list_network_policies",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListNetworkPolicies::new, "kubernetes", "network_policies_tmpl")),
          NativeBinding.of(
              "list_nodes",
              BaseKubernetesListOperation.make(
                  ListNodes::new, Stream.empty(), "kubernetes", "node_tmpl")),
          NativeBinding.of(
              "list_persistent_volume_claims",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListPersisentVolumeClaims::new, "kubernetes", "persistent_volume_claim_tmpl")),
          NativeBinding.of(
              "list_persistent_volumes",
              BaseKubernetesListOperation.make(
                  ListPersisentVolumes::new,
                  Stream.empty(),
                  "kubernetes",
                  "persistent_volume_tmpl")),
          NativeBinding.of(
              "list_pods",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListPods::new, "kubernetes", "pod_tmpl")),
          NativeBinding.of(
              "list_priority_class",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListPriorityClass::new, "kubernetes", "priority_class_tmpl")),
          NativeBinding.of(
              "list_replica_sets",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListReplicaSets::new, "kubernetes", "replica_set_tmpl")),
          NativeBinding.of(
              "list_replication_controllers",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListReplicationControllers::new, "kubernetes", "replication_controller_tmpl")),
          NativeBinding.of(
              "list_resource_quotas",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListResourceQuotas::new, "kubernetes", "resource_quota_tmpl")),
          NativeBinding.of(
              "list_role_bindings",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListRoleBindings::new, "kubernetes", "role_binding_tmpl")),
          NativeBinding.of(
              "list_roles",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListRoles::new, "kubernetes", "role_tmpl")),
          NativeBinding.of(
              "list_secrets",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListSecrets::new, "kubernetes", "secret_tmpl")),
          NativeBinding.of(
              "list_secrets",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListSecrets::new, "kubernetes", "secret_tmpl")),
          NativeBinding.of(
              "list_service_accounts",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListServiceAccounts::new, "kubernetes", "service_account_tmpl")),
          NativeBinding.of(
              "list_services",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListServices::new, "kubernetes", "service_tmpl")),
          NativeBinding.of(
              "list_stateful_sets",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListStatefulSets::new, "kubernetes", "stateful_set_tmpl")),
          NativeBinding.of(
              "list_storage_classes",
              BaseKubernetesNamespacedListOperation.makeWithNamespace(
                  ListStorageClasses::new, "kubernetes", "storage_classes_tmpl")));

  static final AnyConverter<KubernetesClient> K8S_API_CONVERTER =
      AnyConverter.asProxy(KubernetesClient.class, false, SpecialLocation.uri("kube+"));

  @Override
  public Stream<UriHandler> create(ResourcePathFinder finder, Predicate<ServiceFlag> flags) {
    return flags.test(ServiceFlag.SANDBOXED)
        ? Stream.of(INTEROP)
        : Stream.of(
            INTEROP,
            new UriHandler() {
              @Override
              public String name() {
                return "Kubernetes clusters";
              }

              @Override
              public int priority() {
                return 0;
              }

              @Override
              public Result<Promise<Any>> resolveUri(UriExecutor runner, URI uri) {
                if (!uri.getScheme().equals("kube")) {
                  return Result.empty();
                }
                Result<KubernetesClient> client;
                if (uri.getSchemeSpecificPart().isEmpty()) {
                  client = Result.of(new DefaultKubernetesClient());
                } else {
                  client =
                      Result.ofOptional(finder.find(uri.getSchemeSpecificPart(), ".kubeconfig"))
                          .map(FileInputStream::new)
                          .map(DefaultKubernetesClient::fromConfig);
                }
                return client.map(
                    c ->
                        Any.of(
                            Frame.proxyOf(
                                "kube" + c.hashCode(),
                                uri.toString(),
                                c,
                                Stream.of(
                                    ProxyAttribute.fixed("uri", Any.of(toString())),
                                    ProxyAttribute.extractStr(
                                        "base_path", k -> k.getMasterUrl().toString())))));
              }
            });
  }
}
