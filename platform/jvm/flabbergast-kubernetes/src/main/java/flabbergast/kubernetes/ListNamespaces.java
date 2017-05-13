package flabbergast.kubernetes;

import flabbergast.lang.Any;
import flabbergast.lang.Attribute;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import java.util.List;

final class ListNamespaces extends BaseKubernetesListOperation<Namespace, NamespaceList> {

  @Override
  protected FilterWatchListDeletable<Namespace, NamespaceList, Boolean, Watch, Watcher<Namespace>>
      invoke(KubernetesClient client) throws KubernetesClientException {
    return client.namespaces();
  }

  @Override
  protected List<Namespace> items(NamespaceList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, Namespace item) {
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("phase", Any.of(item.getStatus().getPhase())));
  }
}
