package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.LimitRange;
import io.fabric8.kubernetes.api.model.LimitRangeList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListLimitRanges
    extends BaseKubernetesNamespacedListOperation<LimitRange, LimitRangeList> {

  @Override
  protected Operation<LimitRange, LimitRangeList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.limitRanges();
  }

  @Override
  protected List<LimitRange> items(LimitRangeList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, LimitRange item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(
        Attribute.of(
            "limits",
            Frame.define(
                AttributeSource.list(
                    Attribute::of,
                    item.getSpec()
                        .getLimits()
                        .stream()
                        .map(
                            range -> {
                              return Template.instantiate(
                                  AttributeSource.of(
                                      Attribute.of("type", Any.of(range.getType())),
                                      Attribute.of("default", convert(range.getDefault())),
                                      Attribute.of(
                                          "default_request", convert(range.getDefaultRequest())),
                                      Attribute.of("max", convert(range.getMax())),
                                      Attribute.of(
                                          "max_limit_request_ratio",
                                          convert(range.getMaxLimitRequestRatio())),
                                      Attribute.of("min", convert(range.getMin()))),
                                  "Kubernetes limit range item",
                                  "kubernetes",
                                  "limit_range_item_tmpl");
                            })))));
  }
}
