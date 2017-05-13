package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscalerList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.time.ZonedDateTime;
import java.util.List;

final class ListHorizontalPodAutoScalers
    extends BaseKubernetesNamespacedListOperation<
        HorizontalPodAutoscaler, HorizontalPodAutoscalerList> {

  @Override
  protected Operation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, ?, ?> invokeNamespace(
      KubernetesClient client) throws KubernetesClientException {
    return client.autoscaling().horizontalPodAutoscalers();
  }

  @Override
  protected List<HorizontalPodAutoscaler> items(HorizontalPodAutoscalerList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, HorizontalPodAutoscaler item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("max_replicas", Any.of(item.getSpec().getMaxReplicas())));
    builder.add(Attribute.of("min_replicas", Any.of(item.getSpec().getMinReplicas())));
    builder.add(
        Attribute.of("status_current_replicas", Any.of(item.getStatus().getCurrentReplicas())));
    builder.add(
        Attribute.of("status_desired_replicas", Any.of(item.getStatus().getDesiredReplicas())));
    builder.add(
        Attribute.of(
            "status_last_scale_time",
            Frame.of(ZonedDateTime.parse(item.getStatus().getLastScaleTime()))));
    builder.add(
        Attribute.of(
            "status_observed_generation", Any.of(item.getStatus().getObservedGeneration())));
  }
}
