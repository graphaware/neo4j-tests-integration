/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.integration.neo4j.test;

import java.io.File;
import java.io.FileNotFoundException;
import org.apache.commons.io.IOUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.AbstractNeoServer;
import org.neo4j.server.database.Database;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;

/**
 *
 * @author ale
 */
public class GraphDatabaseTestServer {

    private final Integer port;
    private final Integer transactionTimeoutSeconds;
    private final Boolean enableAuthentication;
    private final Boolean enableBolt;

    private GraphDatabaseService database;
    private ServerControls controls;

    private GraphDatabaseTestServer(Builder builder) {

        this.port = builder.port == null ? getAvailablePort() : builder.port;
        this.transactionTimeoutSeconds = builder.transactionTimeoutSeconds;
        this.enableAuthentication = builder.enableAuthentication;
        this.enableBolt = builder.enableBolt;
        startServer();
    }

    private void startServer() {
        try {

            if (enableBolt) {
                controls = TestServerBuilders.newInProcessBuilder()
                        .withConfig("dbms.connector.0.enabled", String.valueOf(enableBolt))
                        .newServer();
            } else {
                controls = TestServerBuilders.newInProcessBuilder()
                        .withConfig("dbms.security.auth_enabled", String.valueOf(enableAuthentication))
                        .withConfig("org.neo4j.server.webserver.port", String.valueOf(port))
                        .withConfig("org.neo4j.server.transaction.timeout", String.valueOf(transactionTimeoutSeconds))
                        .withConfig("dbms.transaction_timeout", String.valueOf(transactionTimeoutSeconds))
                        .withConfig("dbms.security.auth_store.location", createAuthStore())
                        .withConfig("unsupported.dbms.security.auth_store.location", createAuthStore())
                        .withConfig("remote_shell_enabled", "false")
                        .newServer();
            }

            initialise(controls);

            // ensure we shutdown this server when the JVM terminates, if its not been shutdown by user code
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    shutdown();
                }
            });

        } catch (Exception e) {
            throw new RuntimeException("Error starting in-process server", e);
        }

    }

    private String createAuthStore() {
        // creates a temp auth store, with encrypted credentials "neo4j:password" if the server is authenticating connections
        try {
            Path authStore = Files.createTempFile("neo4j", "credentials");
            authStore.toFile().deleteOnExit();

            if (enableAuthentication) {
                try (Writer authStoreWriter = new FileWriter(authStore.toFile())) {
                    IOUtils.write("neo4j:SHA-256,03C9C54BF6EEF1FF3DFEB75403401AA0EBA97860CAC187D6452A1FCF4C63353A,819BDB957119F8DFFF65604C92980A91:", authStoreWriter);
                }
            }

            return authStore.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initialise(ServerControls controls) throws Exception {
        setDatabase(controls);
    }

    private void setDatabase(ServerControls controls) throws Exception {
        try {
            Method method = controls.getClass().getMethod("graph");
            database = (GraphDatabaseService) method.invoke(controls);
        } catch (NoSuchMethodException nsme) {
            Class clazz = Class.forName("org.neo4j.harness.internal.InProcessServerControls");
            Field field = clazz.getDeclaredField("server");
            field.setAccessible(true);
            AbstractNeoServer server = (AbstractNeoServer) field.get(controls);
            Database db = server.getDatabase();
            database = db.getGraph();
        }
    }

    /**
     * Stops the underlying server bootstrapper and, in turn, the Neo4j server.
     */
    public synchronized void shutdown() {
        database.shutdown();
        controls.close();
    }

    public void loadClasspathCypherScriptFile(String cqlFileName) {
        String complexCypher = readCQLFile(cqlFileName).toString();
        String[] cyphers = complexCypher.split(";");
        for (String statement : cyphers) {
            String statementTrimmed = statement.trim();
            if (statementTrimmed.length() > 0) {
                try (Transaction tx = database.beginTx()) {
                    database.execute(statementTrimmed);
                    tx.success();
                }
            }
        }

    }

    /**
     * Waits for a period of time and checks the database availability
     * afterwards
     *
     * @param timeout milliseconds to wait
     * @return true if the database is available, false otherwise
     */
    public boolean isRunning(long timeout) {
        return database.isAvailable(timeout);
    }

    /**
     * Retrieves the base URL of the Neo4j database server used in the test.
     *
     * @return The URL of the Neo4j test server
     */
    public String url() {

        Method method;
        try {
            if (enableBolt) {
                method = controls.getClass().getMethod("boltURI");
            } else {
                method = controls.getClass().getMethod("httpURI");
            }
            Object url = method.invoke(controls);
            return url.toString();
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the underlying {@link org.neo4j.graphdb.GraphDatabaseService}
     * used in this test.
     *
     * @return The test {@link org.neo4j.graphdb.GraphDatabaseService}
     */
    public GraphDatabaseService getGraphDatabaseService() {
        return this.database;
    }

    public static int getAvailablePort() {
        try {
            ServerSocket socket = new ServerSocket(0);
            try {
                return socket.getLocalPort();
            } finally {
                socket.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot find available port: " + e.getMessage(), e);
        }
    }

    public static StringBuilder readCQLFile(String cqlFileName) {

        StringBuilder cypher = new StringBuilder();
        try (Scanner scanner = new Scanner(new File(cqlFileName))) {
            scanner.useDelimiter(System.getProperty("line.separator"));
            while (scanner.hasNext()) {
                cypher.append(scanner.next()).append(' ');
            }
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Error while loading cypher query file " + cqlFileName);
        }
        return cypher;
    }

    public static class Builder {

        private Integer port = null;
        private Integer transactionTimeoutSeconds = 60;
        private boolean enableAuthentication = false;
        private boolean enableBolt = false;

        public Builder() {
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder transactionTimeoutSeconds(int transactionTimeoutSeconds) {
            this.transactionTimeoutSeconds = transactionTimeoutSeconds;
            return this;
        }

        public Builder enableAuthentication(boolean enableAuthentication) {
            this.enableAuthentication = enableAuthentication;
            return this;
        }

        public Builder enableBolt(boolean enableBolt) {
            this.enableBolt = enableBolt;
            return this;
        }

        public GraphDatabaseTestServer build() {
            return new GraphDatabaseTestServer(this);
        }

    }

}
