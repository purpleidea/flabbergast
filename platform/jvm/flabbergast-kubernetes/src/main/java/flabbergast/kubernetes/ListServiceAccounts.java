package flabbergast.kubernetes;

import flabbergast.lang.Any;
import flabbergast.lang.Attribute;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListServiceAccounts
    extends BaseKubernetesNamespacedListOperation<ServiceAccount, ServiceAccountList> {

  @Override
  protected Operation<ServiceAccount, ServiceAccountList, ?, ?> invokeNamespace(
      KubernetesClient client) throws KubernetesClientException {
    return client.serviceAccounts();
  }

  @Override
  protected List<ServiceAccount> items(ServiceAccountList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, ServiceAccount item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(
        Attribute.of(
            "automount_service_account_token", Any.of(item.getAutomountServiceAccountToken())));
  }
}
