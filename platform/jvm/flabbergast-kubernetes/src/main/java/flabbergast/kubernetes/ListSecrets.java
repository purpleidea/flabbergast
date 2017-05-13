package flabbergast.kubernetes;

import flabbergast.lang.Any;
import flabbergast.lang.Attribute;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListSecrets extends BaseKubernetesNamespacedListOperation<Secret, SecretList> {

  @Override
  protected Operation<Secret, SecretList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.secrets();
  }

  @Override
  protected List<Secret> items(SecretList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, Secret item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("type", Any.of(item.getType())));
  }
}
