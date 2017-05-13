package flabbergast.kubernetes;

import flabbergast.lang.Any;
import flabbergast.lang.Attribute;
import io.fabric8.kubernetes.api.model.scheduling.PriorityClass;
import io.fabric8.kubernetes.api.model.scheduling.PriorityClassList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

class ListPriorityClass
    extends BaseKubernetesNamespacedListOperation<PriorityClass, PriorityClassList> {

  @Override
  protected Operation<PriorityClass, PriorityClassList, ?, ?> invokeNamespace(
      KubernetesClient client) throws KubernetesClientException {
    return client.scheduling().priorityClass();
  }

  @Override
  protected List<PriorityClass> items(PriorityClassList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, PriorityClass item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("description", Any.of(item.getDescription())));
    builder.add(Attribute.of("global_default", Any.of(item.getGlobalDefault())));
    builder.add(Attribute.of("value", Any.of(item.getValue())));
  }
}
