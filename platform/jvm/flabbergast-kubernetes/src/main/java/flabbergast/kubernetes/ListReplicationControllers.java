package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListReplicationControllers
    extends BaseKubernetesNamespacedListOperation<
        ReplicationController, ReplicationControllerList> {

  @Override
  protected Operation<ReplicationController, ReplicationControllerList, ?, ?> invokeNamespace(
      KubernetesClient client) throws KubernetesClientException {
    return client.replicationControllers();
  }

  @Override
  protected List<ReplicationController> items(ReplicationControllerList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, ReplicationController item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("min_ready_seconds", Any.of(item.getSpec().getMinReadySeconds())));
    builder.add(Attribute.of("replicas", Any.of(item.getSpec().getReplicas())));
    builder.add(
        Attribute.of("status_available_replicas", Any.of(item.getStatus().getAvailableReplicas())));
    builder.add(
        Attribute.of(
            "status_fully_labelled_replicas", Any.of(item.getStatus().getFullyLabeledReplicas())));
    builder.add(
        Attribute.of(
            "status_observed_generation", Any.of(item.getStatus().getObservedGeneration())));
    builder.add(Attribute.of("status_ready_replicas", Any.of(item.getStatus().getReadyReplicas())));
    builder.add(Attribute.of("status_replicas", Any.of(item.getStatus().getReplicas())));
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
