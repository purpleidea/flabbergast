package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.ArrayList;
import java.util.List;

class ListPersisentVolumeClaims
    extends BaseKubernetesNamespacedListOperation<
        PersistentVolumeClaim, PersistentVolumeClaimList> {

  public static Definition asFrame(PersistentVolumeClaim persistentVolumeClaim) {
    final var builder = new ArrayList<Attribute>();
    unpack(builder, persistentVolumeClaim);
    return Template.instantiate(
        AttributeSource.of(builder),
        "Kubernetes persistent volume claim",
        "kubernetes",
        "persistent_volume_claim_tmpl");
  }

  static void unpack(List<Attribute> builder, PersistentVolumeClaim item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("access_modes", strings(item.getStatus().getAccessModes())));
    builder.add(Attribute.of("capacity", convert(item.getStatus().getCapacity())));
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
    builder.add(Attribute.of("storage_class_name", Any.of(item.getSpec().getStorageClassName())));
    builder.add(Attribute.of("volume_mode", Any.of(item.getSpec().getVolumeMode())));
    builder.add(Attribute.of("volume_name", Any.of(item.getSpec().getVolumeName())));
  }

  @Override
  protected Operation<PersistentVolumeClaim, PersistentVolumeClaimList, ?, ?> invokeNamespace(
      KubernetesClient client) throws KubernetesClientException {
    return client.persistentVolumeClaims();
  }

  @Override
  protected List<PersistentVolumeClaim> items(PersistentVolumeClaimList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, PersistentVolumeClaim item) {
    unpack(builder, item);
  }
}
