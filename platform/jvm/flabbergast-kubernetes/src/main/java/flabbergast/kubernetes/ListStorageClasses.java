package flabbergast.kubernetes;

import static flabbergast.lang.AttributeSource.toSource;

import flabbergast.lang.Any;
import flabbergast.lang.Attribute;
import flabbergast.lang.AttributeSource;
import flabbergast.lang.Frame;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.api.model.storage.StorageClassList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListStorageClasses
    extends BaseKubernetesNamespacedListOperation<StorageClass, StorageClassList> {

  @Override
  protected Operation<StorageClass, StorageClassList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.storage().storageClasses();
  }

  @Override
  protected List<StorageClass> items(StorageClassList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, StorageClass item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("allow_volume_expansion", Any.of(item.getAllowVolumeExpansion())));
    builder.add(Attribute.of("provisioner", Any.of(item.getProvisioner())));
    builder.add(Attribute.of("reclaim_policy", Any.of(item.getReclaimPolicy())));
    builder.add(Attribute.of("volume_binding_mode", Any.of(item.getVolumeBindingMode())));
    builder.add(Attribute.of("mount_options", strings(item.getMountOptions())));
    builder.add(
        Attribute.of(
            "allowed_toplogies",
            Frame.define(
                AttributeSource.list(
                    Attribute::of,
                    item.getAllowedTopologies()
                        .stream()
                        .map(
                            topology ->
                                Frame.define(
                                    topology
                                        .getMatchLabelExpressions()
                                        .stream()
                                        .map(
                                            requirement ->
                                                Attribute.of(
                                                    requirement.getKey(),
                                                    strings(requirement.getValues())))
                                        .collect(toSource())))))));
  }
}
