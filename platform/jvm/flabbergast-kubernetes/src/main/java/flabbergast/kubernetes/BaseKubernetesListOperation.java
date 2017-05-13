package flabbergast.kubernetes;

import static flabbergast.lang.AttributeSource.toSource;

import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import flabbergast.lang.Context;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

abstract class BaseKubernetesListOperation<T extends HasMetadata, L extends KubernetesResourceList>
    implements LookupAssistant.Recipient {
  static Definition convert(Map<String, Quantity> limits) {
    // TODO: Can we convert quantities to numbers?
    return Frame.define(
        limits
            .entrySet()
            .stream()
            .map(e -> Attribute.of(e.getKey(), Any.of(e.getValue().getAmount())))
            .collect(toSource()));
  }

  private static Definition convert(Affinity affinity) {

    return Template.instantiate(
        AttributeSource.of(
            Attribute.of(
                "node_required",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        affinity
                            .getNodeAffinity()
                            .getRequiredDuringSchedulingIgnoredDuringExecution()
                            .getNodeSelectorTerms()
                            .stream()
                            .map(BaseKubernetesListOperation::convert)))),
            Attribute.of(
                "node_preferred",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        affinity
                            .getNodeAffinity()
                            .getPreferredDuringSchedulingIgnoredDuringExecution()
                            .stream()
                            .map(
                                term ->
                                    Frame.define(
                                        AttributeSource.of(
                                            Attribute.of("term", convert(term.getPreference())),
                                            Attribute.of("weight", Any.of(term.getWeight())))))))),
            Attribute.of(
                "pod_affinity_required",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        affinity
                            .getPodAffinity()
                            .getRequiredDuringSchedulingIgnoredDuringExecution()
                            .stream()
                            .map(BaseKubernetesListOperation::convert)))),
            Attribute.of(
                "pod_affinity_preferred",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        affinity
                            .getPodAffinity()
                            .getPreferredDuringSchedulingIgnoredDuringExecution()
                            .stream()
                            .map(
                                term ->
                                    Frame.define(
                                        AttributeSource.of(
                                            Attribute.of(
                                                "term", convert(term.getPodAffinityTerm())),
                                            Attribute.of("weight", Any.of(term.getWeight())))))))),
            Attribute.of(
                "pod_antiaffinity_required",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        affinity
                            .getPodAntiAffinity()
                            .getRequiredDuringSchedulingIgnoredDuringExecution()
                            .stream()
                            .map(BaseKubernetesListOperation::convert)))),
            Attribute.of(
                "pod_antiaffinity_preferred",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        affinity
                            .getPodAntiAffinity()
                            .getPreferredDuringSchedulingIgnoredDuringExecution()
                            .stream()
                            .map(
                                term ->
                                    Frame.define(
                                        AttributeSource.of(
                                            Attribute.of(
                                                "term", convert(term.getPodAffinityTerm())),
                                            Attribute.of("weight", Any.of(term.getWeight()))))))))),
        "Kubernetes affinity",
        "kubernetes",
        "affinity_tmpl");
  }

  private static Definition convert(Container container) {
    return Template.instantiate(
        AttributeSource.of(
            Attribute.of(
                "args",
                Frame.define(AttributeSource.listOfAny(container.getArgs().stream().map(Any::of)))),
            Attribute.of(
                "command",
                Frame.define(
                    AttributeSource.listOfAny(container.getCommand().stream().map(Any::of)))),
            Attribute.of(
                "env",
                Frame.define(
                    container
                        .getEnv()
                        .stream()
                        .map(BaseKubernetesListOperation::convert)
                        .collect(toSource()))),
            Attribute.of(
                "env_from",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        container
                            .getEnvFrom()
                            .stream()
                            .map(BaseKubernetesListOperation::convert)))),
            Attribute.of("image", Any.of(container.getImage())),
            Attribute.of("image_pull_policy", Any.of(container.getImagePullPolicy())),
            container.getLifecycle() == null
                ? Attribute.of("lifecycle_pre_start", Any.NULL)
                : Attribute.of(
                    "lifecycle_pre_start", convert(container.getLifecycle().getPreStop())),
            container.getLifecycle() == null
                ? Attribute.of("lifecycle_post_stop", Any.NULL)
                : Attribute.of(
                    "lifecycle_post_stop", convert(container.getLifecycle().getPostStart())),
            Attribute.of("liveness_probe", convert(container.getLivenessProbe())),
            Attribute.of("name", Any.of(container.getName())),
            Attribute.of(
                "ports",
                Frame.define(
                    container
                        .getPorts()
                        .stream()
                        .map(BaseKubernetesListOperation::convert)
                        .collect(toSource()))),
            Attribute.of("readiness_probe", convert(container.getReadinessProbe())),
            container.getResources() == null
                ? Attribute.of("resource_limits", Any.NULL)
                : Attribute.of("resource_limits", convert(container.getResources().getLimits())),
            container.getResources() == null
                ? Attribute.of("resource_requests", Any.NULL)
                : Attribute.of(
                    "resource_requests", convert(container.getResources().getRequests())),
            Attribute.of(
                "security_context_allow_privilege_escalation",
                Any.of(container.getSecurityContext().getAllowPrivilegeEscalation())),
            Attribute.of(
                "security_context_privileged",
                Any.of(container.getSecurityContext().getPrivileged())),
            Attribute.of(
                "security_context_proc_mount",
                Any.of(container.getSecurityContext().getProcMount())),
            Attribute.of(
                "security_context_read_only_root_file_system",
                Any.of(container.getSecurityContext().getReadOnlyRootFilesystem())),
            Attribute.of(
                "security_context_run_as_group",
                Any.of(container.getSecurityContext().getRunAsGroup())),
            Attribute.of(
                "security_context_run_as_non_root",
                Any.of(container.getSecurityContext().getRunAsNonRoot())),
            Attribute.of(
                "security_context_capabilities_added",
                Frame.define(
                    AttributeSource.listOfAny(
                        container
                            .getSecurityContext()
                            .getCapabilities()
                            .getAdd()
                            .stream()
                            .map(Any::of)))),
            Attribute.of(
                "security_context_capabilities_droped",
                Frame.define(
                    AttributeSource.listOfAny(
                        container
                            .getSecurityContext()
                            .getCapabilities()
                            .getDrop()
                            .stream()
                            .map(Any::of)))),
            Attribute.of("stdin", Any.of(container.getStdin())),
            Attribute.of("stdin_once", Any.of(container.getStdinOnce())),
            Attribute.of("termination_message_path", Any.of(container.getTerminationMessagePath())),
            Attribute.of(
                "termination_message_policy", Any.of(container.getTerminationMessagePolicy())),
            Attribute.of("tty", Any.of(container.getTty())),
            Attribute.of(
                "volume_devices",
                Frame.define(
                    container
                        .getVolumeDevices()
                        .stream()
                        .map(e -> Attribute.of(e.getName(), Any.of(e.getDevicePath())))
                        .collect(toSource()))),
            Attribute.of(
                "volume_mounts",
                Frame.define(
                    container
                        .getVolumeMounts()
                        .stream()
                        .map(BaseKubernetesListOperation::convert)
                        .collect(toSource()))),
            Attribute.of("working_dir", Any.of(container.getWorkingDir()))),
        "Kubernetes container",
        "kubernetes",
        "container_tmpl");
  }

  private static Attribute convert(ContainerPort containerPort) {
    return Attribute.of(
        containerPort.getName(),
        Template.instantiate(
            AttributeSource.of(
                Attribute.of("container_port", Any.of(containerPort.getContainerPort())),
                Attribute.of("host_ip", Any.of(containerPort.getHostIP())),
                Attribute.of("host_port", Any.of(containerPort.getHostPort())),
                Attribute.of("protocol", Any.of(containerPort.getProtocol()))),
            "Kubernetes container port",
            "kubernetes",
            "container_port_tmpl"));
  }

  private static Definition convert(EnvFromSource envFromSource) {
    final var attributes = new ArrayList<Attribute>();
    attributes.add(Attribute.of("prefix", Any.of(envFromSource.getPrefix())));
    final String type;
    if (envFromSource.getConfigMapRef() != null) {
      type = "config_map";
      attributes.add(Attribute.of("name", Any.of(envFromSource.getConfigMapRef().getName())));
      attributes.add(
          Attribute.of("optional", Any.of(envFromSource.getConfigMapRef().getOptional())));
    } else if (envFromSource.getSecretRef() != null) {
      type = "secret";
      attributes.add(Attribute.of("name", Any.of(envFromSource.getSecretRef().getName())));
      attributes.add(Attribute.of("optional", Any.of(envFromSource.getSecretRef().getOptional())));
    } else {
      type = "unknown";
    }
    return Template.instantiate(
        AttributeSource.of(attributes),
        "Kubernetes environment variable",
        "kubernetes",
        type + "_env_source_tmpl");
  }

  private static Attribute convert(EnvVar envVar) {
    final var attributes = new ArrayList<Attribute>();
    attributes.add(Attribute.of("value", Any.of(envVar.getValue())));
    final String type;
    if (envVar.getValueFrom().getConfigMapKeyRef() != null) {
      type = "config_map";
      attributes.add(
          Attribute.of("key", Any.of(envVar.getValueFrom().getConfigMapKeyRef().getKey())));
      attributes.add(
          Attribute.of("name", Any.of(envVar.getValueFrom().getConfigMapKeyRef().getName())));
      attributes.add(
          Attribute.of(
              "optional", Any.of(envVar.getValueFrom().getConfigMapKeyRef().getOptional())));
    } else if (envVar.getValueFrom().getFieldRef() != null) {
      type = "field";
      attributes.add(
          Attribute.of("path", Any.of(envVar.getValueFrom().getFieldRef().getFieldPath())));
    } else if (envVar.getValueFrom().getResourceFieldRef() != null) {
      type = "resource_field";
      attributes.add(
          Attribute.of(
              "divisor",
              Any.of(envVar.getValueFrom().getResourceFieldRef().getDivisor().getAmount())));
      attributes.add(
          Attribute.of(
              "resource", Any.of(envVar.getValueFrom().getResourceFieldRef().getResource())));
      attributes.add(
          Attribute.of(
              "container_name",
              Any.of(envVar.getValueFrom().getResourceFieldRef().getContainerName())));
    } else if (envVar.getValueFrom().getSecretKeyRef() != null) {
      type = "secret_key";
      attributes.add(Attribute.of("key", Any.of(envVar.getValueFrom().getSecretKeyRef().getKey())));
      attributes.add(
          Attribute.of("name", Any.of(envVar.getValueFrom().getSecretKeyRef().getName())));
      attributes.add(
          Attribute.of("optional", Any.of(envVar.getValueFrom().getSecretKeyRef().getOptional())));
    } else {
      type = "unknown";
    }
    return Attribute.of(
        envVar.getName(),
        Template.instantiate(
            AttributeSource.of(attributes),
            "Kubernetes environment variable",
            "kubernetes",
            type + "_env_var_tmpl"));
  }

  private static Definition convert(Handler handler) {
    final var attributes = new ArrayList<Attribute>();
    final String type;
    if (handler.getExec() != null) {
      attributes.add(
          Attribute.of(
              "command",
              Frame.define(
                  AttributeSource.listOfAny(
                      handler.getExec().getCommand().stream().map(Any::of)))));
      type = "exec";
    } else if (handler.getHttpGet() != null) {
      attributes.add(Attribute.of("host", Any.of(handler.getHttpGet().getHost())));
      attributes.add(
          Attribute.of(
              "host",
              Frame.define(
                  handler
                      .getHttpGet()
                      .getHttpHeaders()
                      .stream()
                      .map(header -> Attribute.of(header.getName(), Any.of(header.getValue())))
                      .collect(toSource()))));
      attributes.add(Attribute.of("path", Any.of(handler.getHttpGet().getPath())));
      attributes.add(Attribute.of("port", convert(handler.getHttpGet().getPort())));
      attributes.add(Attribute.of("scheme", Any.of(handler.getHttpGet().getScheme())));
      type = "exec";
    } else if (handler.getTcpSocket() != null) {
      attributes.add(Attribute.of("host", Any.of(handler.getTcpSocket().getHost())));
      attributes.add(Attribute.of("port", convert(handler.getTcpSocket().getPort())));
      type = "tcp";
    } else {
      type = "unknown";
    }

    return Template.instantiate(
        AttributeSource.of(attributes), "Kubernetes handler", "kubernetes", type + "_handler_tmpl");
  }

  static Any convert(IntOrString value) {
    return value.getStrVal() == null ? Any.of(value.getIntVal()) : Any.of(value.getStrVal());
  }

  private static Definition convert(NodeSelectorTerm term) {
    return Template.instantiate(
        AttributeSource.of(
            Attribute.of(
                "match_expressions",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        term.getMatchExpressions()
                            .stream()
                            .map(BaseKubernetesListOperation::convert)))),
            Attribute.of(
                "match_fields",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        term.getMatchFields()
                            .stream()
                            .map(BaseKubernetesListOperation::convert))))),
        "Kubernetes node selector term",
        "kubernetes",
        "node_selector_term_tmpl");
  }

  private static Definition convert(NodeSelectorRequirement term) {
    final String operator;
    switch (term.getOperator()) {
      case "In":
        operator = "in";
        break;
      case "NotIn":
        operator = "not_in";
        break;
      case "Exists":
        operator = "exists";
        break;
      case "DoesNotExist":
        operator = "does_not_exist";
        break;
      case "Gt":
        operator = "greater_than";
        break;
      case "Lt":
        operator = "less_than";
        break;
      default:
        operator = "unknown";
    }
    return Template.instantiate(
        AttributeSource.of(
            Attribute.of("key", Any.of(term.getKey())),
            Attribute.of(
                "values",
                Frame.define(AttributeSource.listOfAny(term.getValues().stream().map(Any::of))))),
        "Kubernetes node selector requirement",
        "node_selector",
        operator);
  }

  private static Definition convert(PodAffinityTerm term) {
    return Template.instantiate(
        AttributeSource.of(
            Attribute.of("selector", labelSelector(term.getLabelSelector())),
            Attribute.of("topology_key", Any.of(term.getTopologyKey())),
            Attribute.of(
                "namespaces",
                Frame.define(
                    AttributeSource.listOfAny(term.getNamespaces().stream().map(Any::of))))),
        "Kuberentes pod affinity term",
        "kubernetes",
        "pod_affinity_term_tmpl");
  }

  private static Definition convert(Probe probe) {
    final var attributes = new ArrayList<Attribute>();
    attributes.add(Attribute.of("failure_threshold", Any.of(probe.getFailureThreshold())));
    attributes.add(Attribute.of("initial_delay_seconds", Any.of(probe.getInitialDelaySeconds())));
    attributes.add(Attribute.of("period_seconds", Any.of(probe.getPeriodSeconds())));
    attributes.add(Attribute.of("success_threshold", Any.of(probe.getSuccessThreshold())));
    attributes.add(Attribute.of("timeout_seconds", Any.of(probe.getTimeoutSeconds())));
    final String type;
    if (probe.getExec() != null) {
      attributes.add(
          Attribute.of(
              "command",
              Frame.define(
                  AttributeSource.listOfAny(probe.getExec().getCommand().stream().map(Any::of)))));
      type = "exec";
    } else if (probe.getHttpGet() != null) {
      attributes.add(Attribute.of("host", Any.of(probe.getHttpGet().getHost())));
      attributes.add(
          Attribute.of(
              "host",
              Frame.define(
                  probe
                      .getHttpGet()
                      .getHttpHeaders()
                      .stream()
                      .map(header -> Attribute.of(header.getName(), Any.of(header.getValue())))
                      .collect(toSource()))));
      attributes.add(Attribute.of("path", Any.of(probe.getHttpGet().getPath())));
      attributes.add(Attribute.of("port", convert(probe.getHttpGet().getPort())));
      attributes.add(Attribute.of("scheme", Any.of(probe.getHttpGet().getScheme())));
      type = "exec";
    } else if (probe.getTcpSocket() != null) {
      attributes.add(Attribute.of("host", Any.of(probe.getTcpSocket().getHost())));
      attributes.add(Attribute.of("port", convert(probe.getTcpSocket().getPort())));
      type = "tcp";
    } else {
      type = "unknown";
    }

    return Template.instantiate(
        AttributeSource.of(attributes), "Kubernetes probe", "kubernetes", type + "_probe_tmpl");
  }

  private static Definition convert(Volume volume) {
    final var attributes = new ArrayList<Attribute>();
    attributes.add(Attribute.of("name", Any.of(volume.getName())));
    final String name;
    if (volume.getAwsElasticBlockStore() != null) {
      name = "aws_ebs";
      attributes.add(
          Attribute.of("partition", Any.of(volume.getAwsElasticBlockStore().getPartition())));
      attributes.add(
          Attribute.of("read_only", Any.of(volume.getAwsElasticBlockStore().getReadOnly())));
      attributes.add(
          Attribute.of("volume_id", Any.of(volume.getAwsElasticBlockStore().getVolumeID())));
    } else if (volume.getAzureDisk() != null) {
      name = "azure_disk";
      attributes.add(Attribute.of("caching_mode", Any.of(volume.getAzureDisk().getCachingMode())));
      attributes.add(Attribute.of("disk_name", Any.of(volume.getAzureDisk().getDiskName())));
      attributes.add(Attribute.of("disk_uri", Any.of(volume.getAzureDisk().getDiskURI())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getAzureDisk().getReadOnly())));
    } else if (volume.getAzureFile() != null) {
      name = "azure_file";
      attributes.add(Attribute.of("read_only", Any.of(volume.getAzureFile().getReadOnly())));
      attributes.add(Attribute.of("secret_name", Any.of(volume.getAzureFile().getSecretName())));
      attributes.add(Attribute.of("share_name", Any.of(volume.getAzureFile().getShareName())));
    } else if (volume.getCephfs() != null) {
      name = "ceph";
      attributes.add(Attribute.of("path", Any.of(volume.getCephfs().getPath())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getCephfs().getReadOnly())));
      attributes.add(Attribute.of("secret_File", Any.of(volume.getCephfs().getSecretFile())));
      attributes.add(Attribute.of("user", Any.of(volume.getCephfs().getUser())));
      attributes.add(
          Attribute.of(
              "monitors",
              Frame.define(
                  AttributeSource.listOfAny(
                      volume.getCephfs().getMonitors().stream().map(Any::of)))));
    } else if (volume.getCinder() != null) {
      name = "cinder";
      attributes.add(Attribute.of("fs_type", Any.of(volume.getCinder().getFsType())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getCinder().getReadOnly())));
      attributes.add(Attribute.of("volume_id", Any.of(volume.getCinder().getVolumeID())));
    } else if (volume.getConfigMap() != null) {
      name = "config_map";
      attributes.add(Attribute.of("default_mode", Any.of(volume.getConfigMap().getDefaultMode())));
      attributes.add(Attribute.of("name", Any.of(volume.getConfigMap().getName())));
      attributes.add(Attribute.of("optional", Any.of(volume.getConfigMap().getOptional())));
      attributes.add(Attribute.of("monitors", keysToPaths(volume.getConfigMap().getItems())));
    } else if (volume.getDownwardAPI() != null) {
      name = "downward_api";
      attributes.add(
          Attribute.of("default_mode", Any.of(volume.getDownwardAPI().getDefaultMode())));
      attributes.add(
          Attribute.of(
              "monitors",
              Frame.define(
                  AttributeSource.listOfDefinition(
                      volume
                          .getDownwardAPI()
                          .getItems()
                          .stream()
                          .map(
                              k ->
                                  Template.instantiate(
                                      AttributeSource.of(
                                          Attribute.of(
                                              "container_name",
                                              Any.of(k.getResourceFieldRef().getContainerName())),
                                          Attribute.of(
                                              "resource",
                                              Any.of(k.getResourceFieldRef().getResource())),
                                          Attribute.of(
                                              "divisor",
                                              Any.of(
                                                  k.getResourceFieldRef()
                                                      .getDivisor()
                                                      .getAmount())),
                                          Attribute.of("path", Any.of(k.getPath())),
                                          Attribute.of("mode", Any.of(k.getMode()))),
                                      "Kubernetes config map volume items",
                                      "kubernetes",
                                      "key_to_path_tmpl"))))));
    } else if (volume.getEmptyDir() != null) {
      name = "empty_dir";
      attributes.add(Attribute.of("medium", Any.of(volume.getEmptyDir().getMedium())));
      attributes.add(
          Attribute.of("size_limit", Any.of(volume.getEmptyDir().getSizeLimit().getAmount())));
    } else if (volume.getFc() != null) {
      name = "fc";
      attributes.add(Attribute.of("fs_type", Any.of(volume.getFc().getFsType())));
      attributes.add(Attribute.of("partition", Any.of(volume.getFc().getLun())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getFc().getReadOnly())));
      attributes.add(
          Attribute.of(
              "target_wwns",
              Frame.define(
                  AttributeSource.listOfAny(
                      volume.getFc().getTargetWWNs().stream().map(Any::of)))));
      attributes.add(
          Attribute.of(
              "wwids",
              Frame.define(
                  AttributeSource.listOfAny(volume.getFc().getWwids().stream().map(Any::of)))));
    } else if (volume.getFlexVolume() != null) {
      name = "flex";
      attributes.add(Attribute.of("fs_type", Any.of(volume.getFlexVolume().getFsType())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getFlexVolume().getReadOnly())));
      attributes.add(Attribute.of("driver", Any.of(volume.getFlexVolume().getDriver())));
      attributes.add(
          Attribute.of(
              "options",
              Frame.define(
                  volume
                      .getFlexVolume()
                      .getOptions()
                      .entrySet()
                      .stream()
                      .map(e -> Attribute.of(e.getKey(), Any.of(e.getValue())))
                      .collect(toSource()))));
    } else if (volume.getFlocker() != null) {
      name = "flocker";
      attributes.add(Attribute.of("fs_type", Any.of(volume.getFlocker().getDatasetName())));
      attributes.add(Attribute.of("partition", Any.of(volume.getFlocker().getDatasetUUID())));
    } else if (volume.getGcePersistentDisk() != null) {
      name = "gce";
      attributes.add(Attribute.of("fs_type", Any.of(volume.getGcePersistentDisk().getFsType())));
      attributes.add(
          Attribute.of("partition", Any.of(volume.getGcePersistentDisk().getPartition())));
      attributes.add(
          Attribute.of("read_only", Any.of(volume.getGcePersistentDisk().getReadOnly())));
      attributes.add(Attribute.of("pd_name", Any.of(volume.getGcePersistentDisk().getPdName())));
    } else if (volume.getGitRepo() != null) {
      name = "git";
      attributes.add(Attribute.of("directory", Any.of(volume.getGitRepo().getDirectory())));
      attributes.add(Attribute.of("repository", Any.of(volume.getGitRepo().getRepository())));
      attributes.add(Attribute.of("revision", Any.of(volume.getGitRepo().getRevision())));
    } else if (volume.getGlusterfs() != null) {
      name = "gluster";
      attributes.add(Attribute.of("endpoints", Any.of(volume.getGlusterfs().getEndpoints())));
      attributes.add(Attribute.of("path", Any.of(volume.getGlusterfs().getPath())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getGlusterfs().getReadOnly())));
    } else if (volume.getHostPath() != null) {
      name = "host_path";
      attributes.add(Attribute.of("path", Any.of(volume.getHostPath().getPath())));
      attributes.add(Attribute.of("type", Any.of(volume.getHostPath().getType())));
    } else if (volume.getIscsi() != null) {
      name = "iscsi";
      attributes.add(
          Attribute.of("chap_auth_discovery", Any.of(volume.getIscsi().getChapAuthDiscovery())));
      attributes.add(
          Attribute.of("chap_auth_session", Any.of(volume.getIscsi().getChapAuthSession())));
      attributes.add(Attribute.of("fs_type", Any.of(volume.getIscsi().getFsType())));
      attributes.add(Attribute.of("initiator_name", Any.of(volume.getIscsi().getInitiatorName())));
      attributes.add(Attribute.of("iqn", Any.of(volume.getIscsi().getIqn())));
      attributes.add(Attribute.of("iscsi_name", Any.of(volume.getIscsi().getIscsiInterface())));
      attributes.add(Attribute.of("lun", Any.of(volume.getIscsi().getLun())));
      attributes.add(
          Attribute.of(
              "portals",
              Frame.define(
                  AttributeSource.listOfAny(
                      volume.getIscsi().getPortals().stream().map(Any::of)))));
      attributes.add(Attribute.of("read_only", Any.of(volume.getIscsi().getReadOnly())));
      attributes.add(Attribute.of("target_portal", Any.of(volume.getIscsi().getTargetPortal())));
    } else if (volume.getNfs() != null) {
      name = "nfs";
      attributes.add(Attribute.of("path", Any.of(volume.getNfs().getPath())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getNfs().getReadOnly())));
      attributes.add(Attribute.of("server", Any.of(volume.getNfs().getServer())));
    } else if (volume.getPersistentVolumeClaim() != null) {
      name = "pv_claim";
      attributes.add(
          Attribute.of("claim_name", Any.of(volume.getPersistentVolumeClaim().getClaimName())));
      attributes.add(
          Attribute.of("read_only", Any.of(volume.getPersistentVolumeClaim().getReadOnly())));
    } else if (volume.getPhotonPersistentDisk() != null) {
      name = "photon";
      attributes.add(Attribute.of("fs_type", Any.of(volume.getPhotonPersistentDisk().getFsType())));
      attributes.add(Attribute.of("pd_id", Any.of(volume.getPhotonPersistentDisk().getPdID())));
    } else if (volume.getPortworxVolume() != null) {
      name = "portworx";
      attributes.add(Attribute.of("fs_type", Any.of(volume.getPortworxVolume().getFsType())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getPortworxVolume().getReadOnly())));
      attributes.add(Attribute.of("volume_id", Any.of(volume.getPortworxVolume().getVolumeID())));
    } else if (volume.getProjected() != null) {
      name = "projected";
      attributes.add(Attribute.of("default_mode", Any.of(volume.getProjected().getDefaultMode())));
      // TODO sources
    } else if (volume.getQuobyte() != null) {
      name = "quobyte";
      attributes.add(Attribute.of("group", Any.of(volume.getQuobyte().getGroup())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getQuobyte().getReadOnly())));
      attributes.add(Attribute.of("registry", Any.of(volume.getQuobyte().getRegistry())));
      attributes.add(Attribute.of("user", Any.of(volume.getQuobyte().getUser())));
      attributes.add(Attribute.of("volume", Any.of(volume.getQuobyte().getVolume())));
    } else if (volume.getRbd() != null) {
      name = "rbd";
      attributes.add(Attribute.of("image", Any.of(volume.getRbd().getImage())));
      attributes.add(Attribute.of("key_ring", Any.of(volume.getRbd().getKeyring())));
      attributes.add(Attribute.of("fs_type", Any.of(volume.getRbd().getFsType())));
      attributes.add(Attribute.of("pool", Any.of(volume.getRbd().getPool())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getRbd().getReadOnly())));
      attributes.add(Attribute.of("user", Any.of(volume.getRbd().getUser())));
      attributes.add(
          Attribute.of(
              "monitors",
              Frame.define(
                  AttributeSource.listOfAny(volume.getRbd().getMonitors().stream().map(Any::of)))));
    } else if (volume.getScaleIO() != null) {
      name = "scale_io";
      attributes.add(Attribute.of("fs_type", Any.of(volume.getScaleIO().getFsType())));
      attributes.add(Attribute.of("gateway", Any.of(volume.getScaleIO().getGateway())));
      attributes.add(
          Attribute.of("protection_domain", Any.of(volume.getScaleIO().getProtectionDomain())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getScaleIO().getReadOnly())));
      attributes.add(Attribute.of("ssl_enabled", Any.of(volume.getScaleIO().getSslEnabled())));
      attributes.add(Attribute.of("storage_mode", Any.of(volume.getScaleIO().getStorageMode())));
      attributes.add(Attribute.of("storage_pool", Any.of(volume.getScaleIO().getStoragePool())));
      attributes.add(Attribute.of("system", Any.of(volume.getScaleIO().getSystem())));
      attributes.add(Attribute.of("volume", Any.of(volume.getScaleIO().getVolumeName())));
    } else if (volume.getSecret() != null) {
      name = "secret";
      attributes.add(Attribute.of("default_mode", Any.of(volume.getSecret().getDefaultMode())));
      attributes.add(Attribute.of("optional", Any.of(volume.getSecret().getOptional())));
      attributes.add(Attribute.of("secret_name", Any.of(volume.getSecret().getSecretName())));
      attributes.add(Attribute.of("items", keysToPaths(volume.getSecret().getItems())));
    } else if (volume.getStorageos() != null) {
      name = "storage_os";
      attributes.add(Attribute.of("fs_type", Any.of(volume.getStorageos().getFsType())));
      attributes.add(Attribute.of("read_only", Any.of(volume.getStorageos().getReadOnly())));
      attributes.add(Attribute.of("volume_name", Any.of(volume.getStorageos().getVolumeName())));
      attributes.add(
          Attribute.of("volume_namespace", Any.of(volume.getStorageos().getVolumeNamespace())));
    } else if (volume.getVsphereVolume() != null) {
      name = "vsphere";
      attributes.add(Attribute.of("fs_type", Any.of(volume.getVsphereVolume().getFsType())));
      attributes.add(
          Attribute.of(
              "storage_policy_id", Any.of(volume.getVsphereVolume().getStoragePolicyID())));
      attributes.add(
          Attribute.of(
              "storage_policy_name", Any.of(volume.getVsphereVolume().getStoragePolicyName())));
      attributes.add(
          Attribute.of("volume_path", Any.of(volume.getVsphereVolume().getVolumePath())));
    } else {
      name = "unknown";
    }
    return Template.instantiate(
        AttributeSource.of(attributes), "Kubernetes volume", "kubernetes", name + "_volume_tmpl");
  }

  private static Attribute convert(VolumeMount mount) {
    return Attribute.of(
        mount.getName(),
        Template.instantiate(
            AttributeSource.of(
                Attribute.of("mount_path", Any.of(mount.getMountPath())),
                Attribute.of("mount_propagation", Any.of(mount.getMountPropagation())),
                Attribute.of("read_only", Any.of(mount.getReadOnly())),
                Attribute.of("subpath", Any.of(mount.getSubPath()))),
            "Kubernetes volume mount",
            "kubernetes",
            "volume_mount_tmpl"));
  }

  private static Definition keysToPaths(List<KeyToPath> items) {
    return Frame.define(
        AttributeSource.listOfDefinition(
            items
                .stream()
                .map(
                    k ->
                        Template.instantiate(
                            AttributeSource.of(
                                Attribute.of("key", Any.of(k.getKey())),
                                Attribute.of("path", Any.of(k.getPath())),
                                Attribute.of("mode", Any.of(k.getMode()))),
                            "Kubernetes key-to-path",
                            "kubernetes",
                            "key_to_path_tmpl"))));
  }

  static Definition labelSelector(LabelSelector selector) {
    return Frame.define(
        AttributeSource.listOfDefinition(
            selector
                .getMatchExpressions()
                .stream()
                .flatMap(
                    requirement -> {
                      final String templateName;
                      switch (requirement.getOperator()) {
                        case "In":
                          templateName = "in";
                          break;
                        case "NotIn":
                          templateName = "not_int";
                          break;
                        case "Exists":
                          templateName = "with";
                          break;
                        case "DoesNotExist":
                          templateName = "without";
                          break;
                        default:
                          return Stream.of(
                              Definition.error(
                                  String.format(
                                      "Unknown label selection operation: “%s”",
                                      requirement.getOperator())));
                      }
                      return Stream.of(
                          Template.function(
                              AttributeSource.of(
                                  Attribute.of("key", Any.of(requirement.getKey())),
                                  Attribute.of("args", strings(requirement.getValues()))),
                              "Kubernetes label selector",
                              "label_selector",
                              templateName));
                    })));
  }

  static <
          F extends BaseKubernetesListOperation<T, L>,
          T extends HasMetadata,
          L extends KubernetesResourceList<?>>
      Definition make(
          Supplier<F> ctor, Stream<LookupAssistant<F>> savers, String... templateNames) {
    return LookupAssistant.create(
        ctor,
        Stream.concat(
            savers,
            Stream.of(
                LookupAssistant.find(
                    AnyConverter.frameOf(
                        AnyConverter.asProxy(
                            LabelSelection.class,
                            false,
                            SpecialLocation.library("cncf", "kubernetes")
                                .attributes("label_selector")
                                .any()
                                .instantiated()),
                        true),
                    BaseKubernetesListOperation::setLabelSelections,
                    "label_selectors"),
                LookupAssistant.find(
                    KubernetesUriService.K8S_API_CONVERTER,
                    BaseKubernetesListOperation::setClient,
                    "k8s_connection"),
                LookupAssistant.find(
                    AnyConverter.asTemplate(false),
                    BaseKubernetesListOperation::setTemplate,
                    templateNames))));
  }

  static Definition podTemplate(PodTemplateSpec spec) {
    return Template.instantiate(
        AttributeSource.of(
            Attribute.of(
                "active_deadline_seconds", Any.of(spec.getSpec().getActiveDeadlineSeconds())),
            Attribute.of("affinity", convert(spec.getSpec().getAffinity())),
            Attribute.of(
                "automount_service_account_token",
                Any.of(spec.getSpec().getAutomountServiceAccountToken())),
            Attribute.of(
                "containers",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        spec.getSpec()
                            .getContainers()
                            .stream()
                            .map(BaseKubernetesListOperation::convert)))),
            Attribute.of("dns_policy", Any.of(spec.getSpec().getDnsPolicy())),
            Attribute.of(
                "dns_policy_nameservers",
                Frame.define(
                    AttributeSource.listOfAny(
                        spec.getSpec().getDnsConfig().getNameservers().stream().map(Any::of)))),
            Attribute.of(
                "dns_policy_searches",
                Frame.define(
                    AttributeSource.listOfAny(
                        spec.getSpec().getDnsConfig().getSearches().stream().map(Any::of)))),
            Attribute.of(
                "dns_policy_options",
                Frame.define(
                    spec.getSpec()
                        .getDnsConfig()
                        .getOptions()
                        .stream()
                        .map(option -> Attribute.of(option.getName(), Any.of(option.getValue())))
                        .collect(toSource()))),
            Attribute.of(
                "host_aliases",
                Frame.define(
                    spec.getSpec()
                        .getHostAliases()
                        .stream()
                        .map(
                            alias ->
                                Attribute.of(
                                    alias.getIp(),
                                    Frame.define(
                                        AttributeSource.listOfAny(
                                            alias.getHostnames().stream().map(Any::of)))))
                        .collect(toSource()))),
            Attribute.of("host_ipc", Any.of(spec.getSpec().getHostIPC())),
            Attribute.of("hostname", Any.of(spec.getSpec().getHostname())),
            Attribute.of("host_network", Any.of(spec.getSpec().getHostNetwork())),
            Attribute.of("host_pid", Any.of(spec.getSpec().getHostPID())),
            Attribute.of("nodename", Any.of(spec.getSpec().getNodeName())),
            Attribute.of(
                "image_pull_secrets",
                Frame.define(
                    AttributeSource.listOfAny(
                        spec.getSpec()
                            .getImagePullSecrets()
                            .stream()
                            .map(ips -> Any.of(ips.getName()))))),
            Attribute.of(
                "node_selector",
                Frame.define(
                    spec.getSpec()
                        .getNodeSelector()
                        .entrySet()
                        .stream()
                        .map(e -> Attribute.of(e.getKey(), Any.of(e.getValue())))
                        .collect(toSource()))),
            Attribute.of("priority", Any.of(spec.getSpec().getPriority())),
            Attribute.of("priority_classname", Any.of(spec.getSpec().getPriorityClassName())),
            Attribute.of(
                "readiness_gates",
                Frame.define(
                    AttributeSource.listOfAny(
                        spec.getSpec()
                            .getReadinessGates()
                            .stream()
                            .map(rg -> Any.of(rg.getConditionType()))))),
            Attribute.of("restart_policy", Any.of(spec.getSpec().getRestartPolicy())),
            Attribute.of("runtime_class_name", Any.of(spec.getSpec().getRuntimeClassName())),
            Attribute.of("scheduler_name", Any.of(spec.getSpec().getSchedulerName())),
            Attribute.of(
                "security_context_fs_group",
                Any.of(spec.getSpec().getSecurityContext().getFsGroup())),
            Attribute.of(
                "security_context_run_as_group",
                Any.of(spec.getSpec().getSecurityContext().getRunAsGroup())),
            Attribute.of(
                "security_context_run_as_non_root",
                Any.of(spec.getSpec().getSecurityContext().getRunAsNonRoot())),
            Attribute.of(
                "security_context_run_as_user",
                Any.of(spec.getSpec().getSecurityContext().getRunAsUser())),
            Attribute.of(
                "security_context_supplemental_groups",
                Frame.define(
                    AttributeSource.listOfAny(
                        spec.getSpec()
                            .getSecurityContext()
                            .getSupplementalGroups()
                            .stream()
                            .map(Any::of)))),
            Attribute.of(
                "security_context_sysctls",
                Frame.define(
                    spec.getSpec()
                        .getSecurityContext()
                        .getSysctls()
                        .stream()
                        .map(s -> Attribute.of(s.getName(), Any.of(s.getValue())))
                        .collect(toSource()))),
            Attribute.of(
                "security_context_selinux_level",
                spec.getSpec().getSecurityContext().getSeLinuxOptions() == null
                    ? Any.NULL
                    : Any.of(spec.getSpec().getSecurityContext().getSeLinuxOptions().getLevel())),
            Attribute.of(
                "security_context_selinux_role",
                spec.getSpec().getSecurityContext().getSeLinuxOptions() == null
                    ? Any.NULL
                    : Any.of(spec.getSpec().getSecurityContext().getSeLinuxOptions().getRole())),
            Attribute.of(
                "security_context_selinux_type",
                spec.getSpec().getSecurityContext().getSeLinuxOptions() == null
                    ? Any.NULL
                    : Any.of(spec.getSpec().getSecurityContext().getSeLinuxOptions().getType())),
            Attribute.of(
                "security_context_selinux_user",
                spec.getSpec().getSecurityContext().getSeLinuxOptions() == null
                    ? Any.NULL
                    : Any.of(spec.getSpec().getSecurityContext().getSeLinuxOptions().getUser())),
            Attribute.of("service_account", Any.of(spec.getSpec().getServiceAccount())),
            Attribute.of("service_account_name", Any.of(spec.getSpec().getServiceAccountName())),
            Attribute.of(
                "share_process_namespace", Any.of(spec.getSpec().getShareProcessNamespace())),
            Attribute.of("subdomain", Any.of(spec.getSpec().getSubdomain())),
            Attribute.of(
                "termination_period_seconds",
                Any.of(spec.getSpec().getTerminationGracePeriodSeconds())),
            Attribute.of(
                "tolerations",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        spec.getSpec()
                            .getTolerations()
                            .stream()
                            .map(
                                t ->
                                    Template.instantiate(
                                        AttributeSource.of(
                                            Attribute.of("effect", Any.of(t.getEffect())),
                                            Attribute.of("key", Any.of(t.getKey())),
                                            Attribute.of("operator", Any.of(t.getOperator())),
                                            Attribute.of(
                                                "toleration_seconds",
                                                Any.of(t.getTolerationSeconds())),
                                            Attribute.of("value", Any.of(t.getValue()))),
                                        "Kubernetes toleration",
                                        "template",
                                        "toleration_tpl"))))),
            Attribute.of(
                "volumes",
                Frame.define(
                    AttributeSource.listOfDefinition(
                        spec.getSpec()
                            .getVolumes()
                            .stream()
                            .map(BaseKubernetesListOperation::convert))))),
        "Kubernetes pod template",
        "kubernetes",
        "pod_spec_tmpl");
  }

  private static void setClient(BaseKubernetesListOperation<?, ?> target, KubernetesClient client) {
    target.client = client;
  }

  private static void setLabelSelections(
      BaseKubernetesListOperation<?, ?> target, Map<Name, LabelSelection> selections) {
    target.labelSelections = selections.values();
  }

  private static void setTemplate(BaseKubernetesListOperation<?, ?> target, Template template) {
    target.template = template;
  }

  static Any status(String status) {
    switch (status) {
      case "True":
        return Any.of(true);
      case "False":
        return Any.of(false);
      default:
        return Any.NULL;
    }
  }

  static Definition strings(List<String> strings) {
    return Frame.define(AttributeSource.listOfAny(strings.stream().map(Any::of)));
  }

  private KubernetesClient client;
  private Collection<LabelSelection> labelSelections = List.of();
  private Template template;

  protected abstract FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> invoke(
      KubernetesClient client) throws KubernetesClientException;

  protected abstract List<T> items(L list);

  protected abstract void populate(List<Attribute> builder, T item);

  @Override
  public void run(Future<Any> future, SourceReference sourceReference, Context context) {
    try {
      var operation = invoke(client);
      for (final var selection : labelSelections) {
        operation = selection.applySelection(operation);
      }

      future.complete(
          Any.of(
              Frame.create(
                  future,
                  sourceReference,
                  context,
                  AttributeSource.listOfAny(
                      items(operation.list())
                          .stream()
                          .map(
                              item -> {
                                final var builder = new ArrayList<Attribute>();
                                populate(builder, item);
                                return Any.of(
                                    Frame.create(
                                        future,
                                        sourceReference,
                                        context,
                                        template,
                                        AttributeSource.of(builder)));
                              })))));
    } catch (Exception ex) {
      future.error(sourceReference, ex.getMessage());
    }
  }
}
