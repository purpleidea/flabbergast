package flabbergast.kubernetes;

import flabbergast.export.LookupAssistant;
import flabbergast.lang.AnyConverter;
import flabbergast.lang.Definition;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.function.Supplier;
import java.util.stream.Stream;

abstract class BaseKubernetesNamespacedListOperation<
        T extends HasMetadata, L extends KubernetesResourceList>
    extends BaseKubernetesListOperation<T, L> {
  public static <
          F extends BaseKubernetesNamespacedListOperation<T, L>,
          T extends HasMetadata,
          L extends KubernetesResourceList<?>>
      Definition makeWithNamespace(Supplier<F> ctor, String... templateNames) {
    return BaseKubernetesListOperation.make(
        ctor,
        Stream.of(
            LookupAssistant.find(
                AnyConverter.asString(true),
                BaseKubernetesNamespacedListOperation::setNamespace,
                "namespace")),
        templateNames);
  }

  private static void setNamespace(
      BaseKubernetesNamespacedListOperation<?, ?> target, String namespace) {
    target.namespace = namespace;
  }

  private String namespace;

  @Override
  protected final FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> invoke(
      KubernetesClient client) throws KubernetesClientException {
    final var operation = invokeNamespace(client);
    if (namespace == null) {
      return operation;
    } else {
      return operation.inNamespace(namespace);
    }
  }

  protected abstract Operation<T, L, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException;
}
