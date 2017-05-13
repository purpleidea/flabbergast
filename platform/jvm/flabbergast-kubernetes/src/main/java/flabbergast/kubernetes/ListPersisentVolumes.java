package flabbergast.kubernetes;

import flabbergast.lang.Any;
import flabbergast.lang.Attribute;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import java.util.List;

class ListPersisentVolumes
    extends BaseKubernetesListOperation<PersistentVolume, PersistentVolumeList> {

  @Override
  protected FilterWatchListDeletable<
          PersistentVolume, PersistentVolumeList, Boolean, Watch, Watcher<PersistentVolume>>
      invoke(KubernetesClient client) throws KubernetesClientException {
    return client.persistentVolumes();
  }

  @Override
  protected List<PersistentVolume> items(PersistentVolumeList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, PersistentVolume item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("access_modes", strings(item.getSpec().getAccessModes())));
    builder.add(Attribute.of("status_message", Any.of(item.getStatus().getMessage())));
    builder.add(Attribute.of("status_reason", Any.of(item.getStatus().getReason())));
  }
}
