/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.integration.neo4j.test;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.neo4j.graphdb.GraphDatabaseService;
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

    @Override
    public void startEmbeddedServer(Map<String, Object>... parameters) {
        final ClassLoader currentClassLoader = this.getClass().getClassLoader();
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setContextClassLoader(currentClassLoader);
                    graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
                    LOG.info("Embedded Neo4j started ...");
                    GraphDatabaseAPI api = (GraphDatabaseAPI) graphDb;

                    ServerConfigurator config = new ServerConfigurator(api);
                    config.configuration()
                            .addProperty(Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY, "127.0.0.1");
                    config.configuration()
                            .addProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, "7474");

                    neoServerBootstrapper = new WrappingNeoServerBootstrapper(api, config);
                    neoServerBootstrapper.start();

                } catch (Exception e) {
                    LOG.error("Error while starting Neo4j embedded server!", e);
                }
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
    }
}
