package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListClusterRoles
    extends BaseKubernetesNamespacedListOperation<ClusterRole, ClusterRoleList> {

  @Override
  protected Operation<ClusterRole, ClusterRoleList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.rbac().clusterRoles();
  }

  @Override
  protected List<ClusterRole> items(ClusterRoleList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, ClusterRole item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("is_cluster", Any.of(true)));
    builder.add(
        Attribute.of(
            "rules",
            Frame.define(
                AttributeSource.list(
                    Attribute::of,
                    item.getRules()
                        .stream()
                        .map(
                            rule ->
                                Template.instantiate(
                                    AttributeSource.of(
                                        Attribute.of("api_groups", strings(rule.getApiGroups())),
                                        Attribute.of(
                                            "non_resource_urls",
                                            strings(rule.getNonResourceURLs())),
                                        Attribute.of(
                                            "resource_names", strings(rule.getResourceNames())),
                                        Attribute.of("resources", strings(rule.getResources())),
                                        Attribute.of("verbs", strings(rule.getVerbs()))),
                                    "Kubernetes rule",
                                    "kubernetes",
                                    "rule_tmpl"))))));
  }
}
