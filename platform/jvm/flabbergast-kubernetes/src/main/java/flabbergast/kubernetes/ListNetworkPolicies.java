package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyList;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyPeer;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyPort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

class ListNetworkPolicies
    extends BaseKubernetesNamespacedListOperation<NetworkPolicy, NetworkPolicyList> {

  @Override
  protected Operation<NetworkPolicy, NetworkPolicyList, ?, ?> invokeNamespace(
      KubernetesClient client) throws KubernetesClientException {
    return client.network().networkPolicies();
  }

  @Override
  protected List<NetworkPolicy> items(NetworkPolicyList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, NetworkPolicy item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("policy_types", strings(item.getSpec().getPolicyTypes())));
    builder.add(
        Attribute.of(
            "ingress",
            Frame.define(
                AttributeSource.list(
                    Attribute::of,
                    item.getSpec()
                        .getIngress()
                        .stream()
                        .map(
                            rule ->
                                rule("ingress_policy_tmpl", rule.getFrom(), rule.getPorts()))))));
    builder.add(
        Attribute.of(
            "egress",
            Frame.define(
                AttributeSource.list(
                    Attribute::of,
                    item.getSpec()
                        .getEgress()
                        .stream()
                        .map(rule -> rule("egress_policy_tmpl", rule.getTo(), rule.getPorts()))))));
  }

  private Definition rule(
      String template, List<NetworkPolicyPeer> peers, List<NetworkPolicyPort> ports) {

    return Template.instantiate(
        AttributeSource.of(
            Attribute.of(
                "ports",
                Frame.define(
                    AttributeSource.list(
                        Attribute::of,
                        ports
                            .stream()
                            .map(
                                port ->
                                    Frame.define(
                                        AttributeSource.of(
                                            Attribute.of("protocol", Any.of(port.getProtocol())),
                                            Attribute.of("port", convert(port.getPort())))))))),
            Attribute.of(
                "peers",
                Frame.define(
                    AttributeSource.list(
                        Attribute::of,
                        peers
                            .stream()
                            .map(
                                peer ->
                                    Frame.define(
                                        AttributeSource.of(
                                            Attribute.of(
                                                "namespace_selector",
                                                labelSelector(peer.getNamespaceSelector())),
                                            Attribute.of(
                                                "pod_selector",
                                                labelSelector(peer.getPodSelector()))))))))),
        "Kubernetes network policy",
        "kubernetes",
        template);
  }
}
