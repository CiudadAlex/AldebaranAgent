package org.aldebaran.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// CHECKSTYLE:OFF

@SpringBootApplication
public class AgentServiceApplication {

    /**
     * Main method of Agent.
     *
     * @param args
     *            args
     */
    public static void main(final String[] args) {
        SpringApplication.run(AgentServiceApplication.class, args);
    }

}
