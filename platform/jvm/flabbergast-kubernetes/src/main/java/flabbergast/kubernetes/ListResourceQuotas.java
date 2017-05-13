package flabbergast.kubernetes;

import flabbergast.lang.Any;
import flabbergast.lang.Attribute;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.ResourceQuotaList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListResourceQuotas
    extends BaseKubernetesNamespacedListOperation<ResourceQuota, ResourceQuotaList> {

  @Override
  protected Operation<ResourceQuota, ResourceQuotaList, ?, ?> invokeNamespace(
      KubernetesClient client) throws KubernetesClientException {
    return client.resourceQuotas();
  }

  @Override
  protected List<ResourceQuota> items(ResourceQuotaList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, ResourceQuota item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("hard", convert(item.getSpec().getHard())));
    builder.add(Attribute.of("status_hard", convert(item.getStatus().getHard())));
    builder.add(Attribute.of("status_used", convert(item.getStatus().getUsed())));
    builder.add(Attribute.of("scopes", strings(item.getSpec().getScopes())));
  }
}
