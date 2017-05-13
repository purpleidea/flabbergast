package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListStatefulSets
    extends BaseKubernetesNamespacedListOperation<StatefulSet, StatefulSetList> {

  @Override
  protected Operation<StatefulSet, StatefulSetList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.apps().statefulSets();
  }

  @Override
  protected List<StatefulSet> items(StatefulSetList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, StatefulSet item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(
        Attribute.of("pod_management_policy", Any.of(item.getSpec().getPodManagementPolicy())));
    builder.add(Attribute.of("service_name", Any.of(item.getSpec().getServiceName())));
    builder.add(Attribute.of("replicas", Any.of(item.getSpec().getReplicas())));
    builder.add(
        Attribute.of("revision_history_limit", Any.of(item.getSpec().getRevisionHistoryLimit())));
    builder.add(
        Attribute.of("status_collision_count", Any.of(item.getStatus().getCollisionCount())));
    builder.add(
        Attribute.of("status_current_revision", Any.of(item.getStatus().getCurrentRevision())));
    builder.add(
        Attribute.of(
            "status_observed_generation", Any.of(item.getStatus().getObservedGeneration())));
    builder.add(Attribute.of("status_ready_replicas", Any.of(item.getStatus().getReadyReplicas())));
    builder.add(Attribute.of("status_replicas", Any.of(item.getStatus().getReplicas())));
    builder.add(
        Attribute.of("status_updated_replicas", Any.of(item.getStatus().getUpdatedReplicas())));
    builder.add(
        Attribute.of("status_updated_revision", Any.of(item.getStatus().getUpdateRevision())));
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
    builder.add(
        Attribute.of(
            "persistent_volume_claim",
            Frame.define(
                AttributeSource.list(
                    Attribute::of,
                    item.getSpec()
                        .getVolumeClaimTemplates()
                        .stream()
                        .map(ListPersisentVolumeClaims::asFrame)))));
  }
}
