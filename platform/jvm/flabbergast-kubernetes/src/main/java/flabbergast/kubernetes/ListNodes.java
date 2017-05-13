package flabbergast.kubernetes;

import flabbergast.lang.Any;
import flabbergast.lang.Attribute;
import flabbergast.lang.AttributeSource;
import flabbergast.lang.Frame;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import java.util.List;

class ListNodes extends BaseKubernetesListOperation<Node, NodeList> {

  @Override
  protected FilterWatchListDeletable<Node, NodeList, Boolean, Watch, Watcher<Node>> invoke(
      KubernetesClient client) throws KubernetesClientException {
    return client.nodes();
  }

  @Override
  protected List<Node> items(NodeList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, Node item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(
        Attribute.of(
            "addresses",
            Frame.define(
                AttributeSource.list(
                    Attribute::of,
                    item.getStatus().getAddresses().stream().map(a -> Any.of(a.getAddress()))))));
    builder.add(
        Attribute.of("architecture", Any.of(item.getStatus().getNodeInfo().getArchitecture())));
    builder.add(Attribute.of("boot_id", Any.of(item.getStatus().getNodeInfo().getBootID())));
    builder.add(
        Attribute.of(
            "container_runtime_version",
            Any.of(item.getStatus().getNodeInfo().getContainerRuntimeVersion())));
    builder.add(
        Attribute.of("kernel_version", Any.of(item.getStatus().getNodeInfo().getKernelVersion())));
    builder.add(
        Attribute.of("kublet_version", Any.of(item.getStatus().getNodeInfo().getKubeletVersion())));
    builder.add(
        Attribute.of(
            "kube_proxy_version", Any.of(item.getStatus().getNodeInfo().getKubeProxyVersion())));
    builder.add(Attribute.of("machine_id", Any.of(item.getStatus().getNodeInfo().getMachineID())));
    builder.add(Attribute.of("os", Any.of(item.getStatus().getNodeInfo().getOperatingSystem())));
    builder.add(Attribute.of("os_image", Any.of(item.getStatus().getNodeInfo().getOsImage())));
    builder.add(
        Attribute.of("system_uuid", Any.of(item.getStatus().getNodeInfo().getSystemUUID())));
    builder.add(Attribute.of("external_id", Any.of(item.getSpec().getExternalID())));
    builder.add(Attribute.of("pod_cidr", Any.of(item.getSpec().getPodCIDR())));
    builder.add(Attribute.of("provider_id", Any.of(item.getSpec().getProviderID())));
    builder.add(Attribute.of("unschedulable", Any.of(item.getSpec().getUnschedulable())));
    builder.add(Attribute.of("phase", Any.of(item.getStatus().getPhase())));
  }
}
