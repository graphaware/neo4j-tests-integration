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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.server.NeoServer;
import org.neo4j.server.helpers.CommunityServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GraphDatabaseServiceWrapperImpl implements GraphDatabaseServiceWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(GraphDatabaseServiceWrapperImpl.class);
    private NeoServer neoServer;
    private GraphDatabaseService database;
    private File tmpDirectory;
    private AtomicBoolean started = new AtomicBoolean(false);

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

                                     CommunityServerBuilder builder = CommunityServerBuilder
                                             .server()
                                             .onAddress(new HostnamePort("127.0.0.1", 7474))
                                             .usingDataDir(tmpDirectory.getAbsolutePath());

                                     for (String key : parameters.keySet()) {
                                         builder = builder.withProperty(key, parameters.get(key).toString());
                                     }

                                     neoServer = builder.build();

                                     neoServer.start();

                                     database = neoServer.getDatabase().getGraph();

                                     LOG.info("Test Neo4j Server started.");

                                     if (parameters.containsKey("cypherPath")) {
                                         LOG.info("Populating Neo4j...");

                                         new EmbeddedGraphgenPopulator(parameters.get("cypherPath").toString()).populate(database);

                                         LOG.info("Done populating Neo4j.");
                                     }

                                     started.set(true);

                                 } catch (Exception e) {
                                     LOG.error("Error while starting Test Neo4j Server!", e);
                                 }
                             }
                         }
        );

        try {
            LOG.info("Waiting for Test Neo4j Server...");
            executor.shutdown();
            executor.awaitTermination(20, TimeUnit.SECONDS);
            LOG.info("Finished waiting.");
        } catch (InterruptedException ex) {
            LOG.error("Error while waiting!", ex);
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

    @Override
    public void stopEmbeddedServer() {
        neoServer.stop();
        if (tmpDirectory != null && tmpDirectory.exists())
            tmpDirectory.delete();
    }
}
