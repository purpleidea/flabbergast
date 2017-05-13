package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListDeployments
    extends BaseKubernetesNamespacedListOperation<Deployment, DeploymentList> {

  @Override
  protected Operation<Deployment, DeploymentList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.apps().deployments();
  }

  @Override
  protected List<Deployment> items(DeploymentList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, Deployment item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("paused", Any.of(item.getSpec().getPaused())));
    builder.add(
        Attribute.of(
            "process_deadline_seconds", Any.of(item.getSpec().getProgressDeadlineSeconds())));
    builder.add(Attribute.of("replicas", Any.of(item.getSpec().getReplicas())));
    builder.add(
        Attribute.of("revision_history_limit", Any.of(item.getSpec().getRevisionHistoryLimit())));
    builder.add(Attribute.of("min_ready_seconds", Any.of(item.getSpec().getMinReadySeconds())));
    builder.add(
        Attribute.of("revision_history_limit", Any.of(item.getSpec().getRevisionHistoryLimit())));
    builder.add(
        Attribute.of("status_available_replicas", Any.of(item.getStatus().getAvailableReplicas())));
    builder.add(
        Attribute.of("status_collision_count", Any.of(item.getStatus().getCollisionCount())));
    builder.add(
        Attribute.of(
            "status_observed_generation", Any.of(item.getStatus().getObservedGeneration())));
    builder.add(Attribute.of("status_ready_replicas", Any.of(item.getStatus().getReadyReplicas())));
    builder.add(Attribute.of("status_replicas", Any.of(item.getStatus().getReplicas())));
    builder.add(
        Attribute.of(
            "status_unavailable_replicas", Any.of(item.getStatus().getUnavailableReplicas())));
    builder.add(
        Attribute.of("status_updated_replicas", Any.of(item.getStatus().getUpdatedReplicas())));
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
