package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListRoles extends BaseKubernetesNamespacedListOperation<Role, RoleList> {

  @Override
  protected Operation<Role, RoleList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.rbac().roles();
  }

  @Override
  protected List<Role> items(RoleList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, Role item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("is_cluster", Any.of(false)));
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
