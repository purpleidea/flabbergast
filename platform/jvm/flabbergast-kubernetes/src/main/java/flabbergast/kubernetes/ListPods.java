package flabbergast.kubernetes;

import flabbergast.lang.Any;
import flabbergast.lang.Attribute;
import flabbergast.lang.Frame;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.time.ZonedDateTime;
import java.util.List;

class ListPods extends BaseKubernetesNamespacedListOperation<Pod, PodList> {

  @Override
  protected Operation<Pod, PodList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.pods();
  }

  @Override
  protected List<Pod> items(PodList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, Pod item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("status_message", Any.of(item.getStatus().getMessage())));
    builder.add(Attribute.of("status_reason", Any.of(item.getStatus().getReason())));
    builder.add(Attribute.of("phase", Any.of(item.getStatus().getPhase())));
    builder.add(
        Attribute.of("start_time", Frame.of(ZonedDateTime.parse(item.getStatus().getStartTime()))));
  }
}
