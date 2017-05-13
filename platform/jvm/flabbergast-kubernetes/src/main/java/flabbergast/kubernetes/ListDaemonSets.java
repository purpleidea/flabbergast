package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListDaemonSets extends BaseKubernetesNamespacedListOperation<DaemonSet, DaemonSetList> {

  @Override
  protected Operation<DaemonSet, DaemonSetList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.apps().daemonSets();
  }

  @Override
  protected List<DaemonSet> items(DaemonSetList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, DaemonSet item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("min_ready_seconds", Any.of(item.getSpec().getMinReadySeconds())));
    builder.add(
        Attribute.of("revision_history_limit", Any.of(item.getSpec().getRevisionHistoryLimit())));
    builder.add(Attribute.of("selector", labelSelector(item.getSpec().getSelector())));
    builder.add(
        Attribute.of("update_strategy", Any.of(item.getSpec().getUpdateStrategy().getType())));
    builder.add(
        Attribute.of(
            "max_unavailable",
            item.getSpec().getUpdateStrategy().getRollingUpdate() == null
                ? Any.NULL
                : convert(
                    item.getSpec().getUpdateStrategy().getRollingUpdate().getMaxUnavailable())));
    builder.add(Attribute.of("template", podTemplate(item.getSpec().getTemplate())));
    builder.add(
        Attribute.of("status_collision_count", Any.of(item.getStatus().getCollisionCount())));
    builder.add(
        Attribute.of(
            "status_current_number_scheduled",
            Any.of(item.getStatus().getCurrentNumberScheduled())));
    builder.add(
        Attribute.of(
            "status_desired_number_scheduled",
            Any.of(item.getStatus().getDesiredNumberScheduled())));
    builder.add(
        Attribute.of("status_number_available", Any.of(item.getStatus().getNumberAvailable())));
    builder.add(
        Attribute.of(
            "status_number_misscheduled", Any.of(item.getStatus().getNumberMisscheduled())));
    builder.add(Attribute.of("status_number_ready", Any.of(item.getStatus().getNumberReady())));
    builder.add(
        Attribute.of("status_number_unavailable", Any.of(item.getStatus().getNumberUnavailable())));
    builder.add(
        Attribute.of(
            "status_observed_generation", Any.of(item.getStatus().getObservedGeneration())));
    builder.add(
        Attribute.of(
            "status_updated_number_scheduled",
            Any.of(item.getStatus().getUpdatedNumberScheduled())));
    builder.add(
        Attribute.of(
            "conditions",
            Frame.define(
                AttributeSource.list(
                    Attribute::of,
                    item.getStatus()
                        .getConditions()
                        .stream()
                        .map(
                            condition ->
                                Template.instantiate(
                                    AttributeSource.of(
                                        Attribute.of("message", Any.of(condition.getMessage())),
                                        Attribute.of("reason", Any.of(condition.getReason())),
                                        Attribute.of("status", status(condition.getStatus())),
                                        Attribute.of("type", Any.of(condition.getType()))),
                                    "Kubernetes condition",
                                    "kubernetes",
                                    "condition_tmpl"))))));
  }
}
