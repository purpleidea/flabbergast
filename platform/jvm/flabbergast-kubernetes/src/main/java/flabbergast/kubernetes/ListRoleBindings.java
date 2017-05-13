package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListRoleBindings
    extends BaseKubernetesNamespacedListOperation<RoleBinding, RoleBindingList> {

  @Override
  protected Operation<RoleBinding, RoleBindingList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.rbac().roleBindings();
  }

  @Override
  protected List<RoleBinding> items(RoleBindingList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, RoleBinding item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("is_cluster", Any.of(false)));
    builder.add(Attribute.of("role_name", Any.of(item.getRoleRef().getName())));
    builder.add(Attribute.of("role_kind", Any.of(item.getRoleRef().getKind())));
    builder.add(Attribute.of("role_api_group", Any.of(item.getRoleRef().getApiGroup())));
    builder.add(
        Attribute.of(
            "subjects",
            Frame.define(
                AttributeSource.list(
                    Attribute::of,
                    item.getSubjects()
                        .stream()
                        .map(
                            subject ->
                                Template.instantiate(
                                    AttributeSource.of(
                                        Attribute.of("api_group", Any.of(subject.getApiGroup())),
                                        Attribute.of("name", Any.of(subject.getName())),
                                        Attribute.of("namespace", Any.of(subject.getNamespace())),
                                        Attribute.of("kind", Any.of(subject.getKind()))),
                                    "Kubernetes subject",
                                    "kubernetes",
                                    "subject_tmpl"))))));
  }
}
