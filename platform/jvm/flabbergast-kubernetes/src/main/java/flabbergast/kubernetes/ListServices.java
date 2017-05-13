package flabbergast.kubernetes;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListServices extends BaseKubernetesNamespacedListOperation<Service, ServiceList> {

  @Override
  protected Operation<Service, ServiceList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.services();
  }

  @Override
  protected List<Service> items(ServiceList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, Service item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(Attribute.of("cluster_ip", Any.of(item.getSpec().getClusterIP())));
    builder.add(Attribute.of("external_name", Any.of(item.getSpec().getExternalName())));
    builder.add(
        Attribute.of("external_traffic_policy", Any.of(item.getSpec().getExternalTrafficPolicy())));
    builder.add(
        Attribute.of("health_check_node_port", Any.of(item.getSpec().getHealthCheckNodePort())));
    builder.add(Attribute.of("load_balancer_ip", Any.of(item.getSpec().getLoadBalancerIP())));
    builder.add(
        Attribute.of(
            "publish_non_ready_addresses", Any.of(item.getSpec().getPublishNotReadyAddresses())));
    builder.add(Attribute.of("session_affinity", Any.of(item.getSpec().getSessionAffinity())));
    builder.add(Attribute.of("type", Any.of(item.getSpec().getType())));
    builder.add(
        Attribute.of(
            "loadbalancer_ingresses",
            Frame.define(
                AttributeSource.list(
                    Attribute::of,
                    item.getStatus()
                        .getLoadBalancer()
                        .getIngress()
                        .stream()
                        .map(
                            ingress ->
                                Template.instantiate(
                                    AttributeSource.of(
                                        Attribute.of("ip", Any.of(ingress.getIp())),
                                        Attribute.of("hostname", Any.of(ingress.getHostname()))),
                                    "Kubernetes service ingress",
                                    "kubernetes",
                                    "service_ingress_tmpl"))))));
  }
}
