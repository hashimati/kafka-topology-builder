package com.purbon.kafka.topology;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TopologyBuilderConfig {

  public static final String ACCESS_CONTROL_IMPLEMENTATION_CLASS =
      "topology.builder.access.control.class";

  public static final String ACCESS_CONTROL_DEFAULT_CLASS =
      "com.purbon.kafka.topology.roles.SimpleAclsProvider";
  public static final String RBAC_ACCESS_CONTROL_CLASS =
      "com.purbon.kafka.topology.roles.RBACProvider";

  public static final String STATE_PROCESSOR_IMPLEMENTATION_CLASS =
      "topology.builder.state.processor.class";

  public static final String STATE_PROCESSOR_DEFAULT_CLASS =
      "com.purbon.kafka.topology.clusterstate.FileStateProcessor";
  public static final String REDIS_STATE_PROCESSOR_CLASS =
      "com.purbon.kafka.topology.clusterstate.RedisStateProcessor";

  public static final String REDIS_HOST_CONFIG = "topology.builder.redis.host";

  public static final String REDIS_PORT_CONFIG = "topology.builder.redis.port";

  public static final String MDS_SERVER = "topology.builder.mds.server";
  public static final String MDS_USER_CONFIG = "topology.builder.mds.user";
  public static final String MDS_PASSWORD_CONFIG = "topology.builder.mds.password";
  public static final String MDS_KAFKA_CLUSTER_ID_CONFIG = "topology.builder.mds.kafka.cluster.id";
  public static final String MDS_SR_CLUSTER_ID_CONFIG =
      "topology.builder.mds.schema.registry.cluster.id";
  public static final String MDS_KC_CLUSTER_ID_CONFIG =
      "topology.builder.mds.kafka.connect.cluster.id";

  private final Map<String, String> cliParams;
  private final Properties properties;

  public TopologyBuilderConfig(Map<String, String> cliParams, Properties properties) {
    this.cliParams = cliParams;
    this.properties = properties;
  }

  public void validateWith(Topology topology) throws ConfigurationException {

    raiseIfNull(ACCESS_CONTROL_IMPLEMENTATION_CLASS);

    boolean isRbac =
        properties
            .getProperty(ACCESS_CONTROL_IMPLEMENTATION_CLASS)
            .equalsIgnoreCase(RBAC_ACCESS_CONTROL_CLASS);
    if (!isRbac) {
      return;
    }

    raiseIfNull(MDS_SERVER, MDS_USER_CONFIG, MDS_PASSWORD_CONFIG);

    boolean hasSchemaRegistry = !topology.getPlatform().getSchemaRegistry().isEmpty();
    boolean hasKafkaConnect = false;
    List<Project> projects = topology.getProjects();
    for (int i = 0; !hasKafkaConnect && i < projects.size(); i++) {
      Project project = projects.get(i);
      hasKafkaConnect = !project.getConnectors().isEmpty();
    }

    raiseIfNull(MDS_KAFKA_CLUSTER_ID_CONFIG);

    if (hasSchemaRegistry) {
      raiseIfNull(MDS_SR_CLUSTER_ID_CONFIG);
    } else if (hasKafkaConnect && properties.getProperty(MDS_KC_CLUSTER_ID_CONFIG) == null) {
      raiseIfNull(MDS_KC_CLUSTER_ID_CONFIG);
    }
  }

  private void raiseIfNull(String... keys) throws ConfigurationException {
    for (String key : keys) {
      raiseIfNull(key, properties.getProperty(key));
    }
  }

  private void raiseIfNull(String key, String value) throws ConfigurationException {
    if (value == null) {
      throw new ConfigurationException(
          "Required configuration " + key + " is missing, please add it to your configuration");
    }
  }

  public Map<String, String> params() {
    return cliParams;
  }

  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  public Object getOrDefault(Object key, Object _default) {
    return properties.getOrDefault(key, _default);
  }
}
