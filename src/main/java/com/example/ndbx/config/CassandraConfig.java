package com.example.ndbx.config;

import com.example.ndbx.util.Constants;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CassandraConfig {

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.port}")
    private int port;

    @Value("${spring.cassandra.keyspace-name}")
    private String keyspace;

    @Value("${spring.cassandra.local-datacenter}")
    private String localDatacenter;

    @Value("${CASSANDRA_USERNAME:}")
    private String username;

    @Value("${CASSANDRA_PASSWORD:}")
    private String password;

    @Value("${CASSANDRA_CONSISTENCY:ONE}")
    private String consistency;

    @Bean
    public CqlSession cassandraSession() {
        try (CqlSession initSession = buildSessionBuilder().build()) {
            initSession.execute(
                "CREATE KEYSPACE IF NOT EXISTS \"" + keyspace + "\" " +
                "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}"
            );
            initSession.execute(
                "CREATE TABLE IF NOT EXISTS \"" + keyspace + "\"." + Constants.CASSANDRA_TABLE_EVENT_REACTIONS + " (" +
                "  " + Constants.PV_EVENT_ID + " text," +
                "  " + Constants.FLD_CREATED_BY + " text," +
                "  " + Constants.CASSANDRA_COL_LIKE_VALUE + " tinyint," +
                "  " + Constants.FLD_CREATED_AT + " timestamp," +
                "  PRIMARY KEY (" + Constants.PV_EVENT_ID + ", " + Constants.FLD_CREATED_BY + ")" +
                ")"
            );
        }

        return buildSessionBuilder()
            .withKeyspace(keyspace)
            .withConfigLoader(
                DriverConfigLoader.programmaticBuilder()
                    .withString(DefaultDriverOption.REQUEST_CONSISTENCY, consistency)
                    .build()
            )
            .build();
    }

    private CqlSessionBuilder buildSessionBuilder() {
        List<InetSocketAddress> addresses = Arrays.stream(contactPoints.split(","))
            .map(h -> new InetSocketAddress(h.trim(), port))
            .collect(Collectors.toList());

        CqlSessionBuilder builder = CqlSession.builder()
            .addContactPoints(addresses)
            .withLocalDatacenter(localDatacenter);

        if (username != null && !username.isBlank()) {
            builder.withAuthCredentials(username, password);
        }

        return builder;
    }
}
