/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphDatabaseServiceWrapperImpl implements GraphDatabaseServiceWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(GraphDatabaseServiceWrapperImpl.class);
    private GraphDatabaseTestServer neoServerBootstrapper;
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
                    LOG.info("Embedded Neo4j started ...");
                    Thread.currentThread().setContextClassLoader(currentClassLoader);
                    
                    GraphDatabaseTestServer.Builder neoServerBootstrapperBuilder = new GraphDatabaseTestServer.Builder();
                    boolean enableBolt = Boolean.parseBoolean(getParameter(parameters, EmbeddedGraphDatabaseServerConfig.CONFIG_REST_ENABLE_BOLT, "false"));
                    neoServerBootstrapperBuilder.enableBolt(enableBolt);
                    
                    int port = Integer.parseInt(getParameter(parameters, EmbeddedGraphDatabaseServerConfig.CONFIG_REST_PORT, "7474"));
                    neoServerBootstrapperBuilder.port(port);
                    
                    boolean enableAuth = Boolean.parseBoolean(getParameter(parameters, EmbeddedGraphDatabaseServerConfig.CONFIG_REST_ENABLE_AUTH, "false"));
                    neoServerBootstrapperBuilder.enableAuthentication(enableAuth);
                    
                    neoServerBootstrapper = neoServerBootstrapperBuilder.build();
                    if (!neoServerBootstrapper.isRunning(10000))
                        throw new RuntimeException("Could not start graph db after 10 seconds");
                    graphDb = neoServerBootstrapper.getGraphDatabaseService();
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
        neoServerBootstrapper.shutdown();
        if (tmpDirectory != null && tmpDirectory.exists())
            tmpDirectory.delete();
    }

    @Override
    public void populate(String cypherPath) {
        if (graphDb == null || !graphDb.isAvailable(10000))
            throw new RuntimeException("GraphDb Cannot be populate: Database not available");
        neoServerBootstrapper.loadClasspathCypherScriptFile(cypherPath);
//        EmbeddedGraphgenPopulator populator = new EmbeddedGraphgenPopulator(cypherPath);
        //populator.populate(graphDb);
    }

    @Override
    public String getURL() {
        if (neoServerBootstrapper == null)
            throw new RuntimeException("neoServerBootstrapper is still not initialized");
        return neoServerBootstrapper.url();
    }
}
