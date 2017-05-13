package flabbergast.kubernetes;

import static flabbergast.lang.AttributeSource.toSource;

import flabbergast.lang.Any;
import flabbergast.lang.Attribute;
import flabbergast.lang.Frame;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

final class ListConfigMaps extends BaseKubernetesNamespacedListOperation<ConfigMap, ConfigMapList> {

  @Override
  protected Operation<ConfigMap, ConfigMapList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.configMaps();
  }

  @Override
  protected List<ConfigMap> items(ConfigMapList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, ConfigMap item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(
        Attribute.of(
            "data",
            Frame.define(
                Stream.concat(
                        item.getData()
                            .entrySet()
                            .stream()
                            .map(e -> Attribute.of(e.getKey(), Any.of(e.getValue()))),
                        item.getBinaryData()
                            .entrySet()
                            .stream()
                            .map(
                                e ->
                                    Attribute.of(
                                        e.getKey(),
                                        Any.of(Base64.getDecoder().decode(e.getValue())))))
                    .collect(toSource()))));
  }
}
