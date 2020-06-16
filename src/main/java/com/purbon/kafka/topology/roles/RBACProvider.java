package com.purbon.kafka.topology.roles;

import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.DEVELOPER_READ;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.DEVELOPER_WRITE;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.RESOURCE_OWNER;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.SECURITY_ADMIN;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.SYSTEM_ADMIN;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.ClusterState;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RBACProvider implements AccessControlProvider {

  private static final Logger LOGGER = LogManager.getLogger(RBACProvider.class);

  public static final String LITERAL = "LITERAL";
  public static final String PREFIX = "PREFIXED";
  private final MDSApiClient apiClient;

  public RBACProvider(MDSApiClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public void clearAcls(ClusterState clusterState) {}

  @Override
  public List<TopologyAclBinding> setAclsForConnect(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {

    List<TopologyAclBinding> bindings = new ArrayList<>();

    TopologyAclBinding secAdminBinding =
        apiClient.bind(principal, SECURITY_ADMIN).forKafkaConnect().apply();
    bindings.add(secAdminBinding);

    apiClient.bind(principal, DEVELOPER_READ, topicPrefix, PREFIX);
    if (readTopics != null && readTopics.isEmpty()) {
      readTopics.forEach(
          topic -> {
            TopologyAclBinding binding = apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL);
            bindings.add(binding);
          });
    }
    if (writeTopics != null && readTopics.isEmpty()) {
      writeTopics.forEach(
          topic -> {
            TopologyAclBinding binding = apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL);
            bindings.add(binding);
          });
    }

    String[] resources =
        new String[] {
          "Topic:connect-configs",
          "Topic:connect-offsets",
          "Topic:connect-status",
          "Group:connect-cluster",
          "Group:secret-registry",
          "Topic:_confluent-secrets"
        };

    Arrays.asList(resources)
        .forEach(
            resourceObject -> {
              String[] elements = resourceObject.split(":");
              String resource = elements[1];
              String resourceType = elements[0];
              TopologyAclBinding binding =
                  apiClient.bind(principal, RESOURCE_OWNER, resource, resourceType, LITERAL);
              bindings.add(binding);
            });
    return bindings;
  }

  @Override
  public List<TopologyAclBinding> setAclsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {
    List<TopologyAclBinding> bindings = new ArrayList<>();

    TopologyAclBinding binding = apiClient.bind(principal, DEVELOPER_READ, topicPrefix, PREFIX);
    bindings.add(binding);

    readTopics.forEach(
        topic -> {
          TopologyAclBinding readBinding =
              apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL);
          bindings.add(readBinding);
        });
    writeTopics.forEach(
        topic -> {
          TopologyAclBinding writeBinding =
              apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL);
          bindings.add(writeBinding);
        });

    binding = apiClient.bind(principal, RESOURCE_OWNER, topicPrefix, PREFIX);
    bindings.add(binding);
    binding = apiClient.bind(principal, RESOURCE_OWNER, topicPrefix, "Group", PREFIX);
    bindings.add(binding);

    return bindings;
  }

  @Override
  public List<TopologyAclBinding> setAclsForConsumers(Collection<String> principals, String topic) {
    List<TopologyAclBinding> bindings = new ArrayList<>();
    principals.forEach(
        principal -> {
          TopologyAclBinding binding = apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL);
          bindings.add(binding);
        });
    return bindings;
  }

  @Override
  public List<TopologyAclBinding> setAclsForProducers(Collection<String> principals, String topic) {
    List<TopologyAclBinding> bindings = new ArrayList<>();
    principals.forEach(
        principal -> {
          TopologyAclBinding binding = apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL);
          bindings.add(binding);
        });
    return bindings;
  }

  @Override
  public TopologyAclBinding setPredefinedRole(
      String principal, String predefinedRole, String topicPrefix) {
    return apiClient.bind(principal, predefinedRole, topicPrefix, PREFIX);
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public List<TopologyAclBinding> setAclsForSchemaRegistry(String principal) {
    List<TopologyAclBinding> bindings = new ArrayList<>();
    TopologyAclBinding binding =
        apiClient.bind(principal, SECURITY_ADMIN).forSchemaRegistry().apply();
    bindings.add(binding);
    binding = apiClient.bind(principal, RESOURCE_OWNER, "_schemas", LITERAL);
    bindings.add(binding);
    binding = apiClient.bind(principal, RESOURCE_OWNER, "schema-registry", "Group", LITERAL);
    bindings.add(binding);
    return bindings;
  }

  @Override
  public List<TopologyAclBinding> setAclsForControlCenter(String principal, String appId) {
    TopologyAclBinding binding = apiClient.bind(principal, SYSTEM_ADMIN).forControlCenter().apply();
    return Arrays.asList(binding);
  }

  @Override
  public Map<String, List<TopologyAclBinding>> listAcls() {
    LOGGER.warn("Not implemented yet!");
    return new HashMap<>();
  }
}
