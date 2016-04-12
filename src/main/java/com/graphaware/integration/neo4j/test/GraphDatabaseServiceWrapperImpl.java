/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.integration.neo4j.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphDatabaseServiceWrapperImpl implements GraphDatabaseServiceWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(GraphDatabaseServiceWrapperImpl.class);
    private WrappingNeoServerBootstrapper neoServerBootstrapper;
    private GraphDatabaseService graphDb;
    private File tmpDirectory;

    @Override
    public void startEmbeddedServer() {
        startEmbeddedServer(Collections.<String, Object>emptyMap());
    }

    @Override
    public void startEmbeddedServer(final Map<String, Object> parameters) {
        final ClassLoader currentClassLoader = this.getClass().getClassLoader();
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setContextClassLoader(currentClassLoader);
                    tmpDirectory = getTempDirectory();
                    GraphDatabaseBuilder newImpermanentDatabaseBuilder = new TestGraphDatabaseFactory().newEmbeddedDatabaseBuilder(tmpDirectory);
                    if (parameters != null && parameters.containsKey(EmbeddedGraphDatabaseServerConfig.CONFIG_FILE_PATH)) {
                        newImpermanentDatabaseBuilder.loadPropertiesFromFile(String.valueOf(parameters.get(EmbeddedGraphDatabaseServerConfig.CONFIG_FILE_PATH)));
                    }
                    graphDb = newImpermanentDatabaseBuilder
                            .newGraphDatabase();

                    LOG.info("Embedded Neo4j started ...");
                    GraphDatabaseAPI api = (GraphDatabaseAPI) graphDb;

                    String address = getParameter(parameters, EmbeddedGraphDatabaseServerConfig.CONFIG_REST_ADDRESS, "127.0.0.1");
                    ServerConfigurator config = new ServerConfigurator(api);
                    config.configuration()
                            .addProperty(Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY, address);
                    String port = getParameter(parameters, EmbeddedGraphDatabaseServerConfig.CONFIG_REST_PORT, "7474");

                    config.configuration()
                            .addProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, "7474");
                    neoServerBootstrapper = new WrappingNeoServerBootstrapper(api, config);
                    neoServerBootstrapper.start();

                } catch (Exception e) {
                    LOG.error("Error while starting Neo4j embedded server!", e);
                }
            }

            private File getTempDirectory() throws IOException {
                String defaultTmp = System.getProperty("java.io.tmpdir");
                System.out.println(defaultTmp);
                Path tmpDirectory = Files.createTempDirectory(new File(defaultTmp).toPath(), "neoTestDb_");
                File tmpDirectoryFile = tmpDirectory.toFile();
                tmpDirectoryFile.deleteOnExit();
                return tmpDirectoryFile;
            }

            private String getParameter(Map<String, Object> parameters, String key, String defaultValue) {
                if (parameters != null && parameters.containsKey(key)) {
                    return String.valueOf(parameters.get(key));
                }
                return defaultValue;
            }
        }
        );

        try {
            LOG.info("Waiting for Neo4j embedded server...");
            executor.shutdown();
            executor.awaitTermination(20, TimeUnit.SECONDS);
            LOG.info("Finished waiting.");
        } catch (InterruptedException ex) {
            LOG.error("Error while waiting!", ex);
        }
    }

    @Override
    public void stopEmbeddedServer() {
        neoServerBootstrapper.stop();
        graphDb.shutdown();
        if (tmpDirectory != null && tmpDirectory.exists())
            tmpDirectory.delete();
    }

    @Override
    public void populate(String cypherPath) {
        if (graphDb == null || !graphDb.isAvailable(10000))
            throw new RuntimeException("GraphDb Cannot be populate: Database not available");
        EmbeddedGraphgenPopulator populator = new EmbeddedGraphgenPopulator(cypherPath);
        populator.populate(graphDb);
    }
}
