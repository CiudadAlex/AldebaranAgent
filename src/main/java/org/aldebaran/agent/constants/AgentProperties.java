package org.aldebaran.agent.constants;

/**
 * AgentProperties.
 *
 * @author Alejandro
 *
 */
public final class AgentProperties {

    private AgentProperties() {
    }

    /** PATH_STORE_JARS. */
    public static final String PATH_STORE_JARS = "aldebaran.agent.path_store_jar";

    /** PATH_STORE_JOB_SERIALIZATION. */
    public static final String PATH_STORE_JOB_SERIALIZATION = "aldebaran.agent.path_store_job_derialization";

    /** SERVICE_PASSWORD. */
    public static final String SERVICE_PASSWORD = "aldebaran.agent.service.password";

    /** SERVICE_ALLOWED_IPS. */
    public static final String SERVICE_ALLOWED_IPS = "aldebaran.agent.service.allowed_ips";

    /** WORKER_JAVA_EXECUTABLE. */
    public static final String WORKER_JAVA_EXECUTABLE = "aldebaran.agent.worker.java.executable";

    /** WORKER_JAVA_CLASSPATH_SEPARATOR. */
    public static final String WORKER_JAVA_CLASSPATH_SEPARATOR = "aldebaran.agent.worker.java.classpath_separator";

    /** WORKER_DEBUG. */
    public static final String WORKER_DEBUG = "aldebaran.agent.worker.debug";
}
