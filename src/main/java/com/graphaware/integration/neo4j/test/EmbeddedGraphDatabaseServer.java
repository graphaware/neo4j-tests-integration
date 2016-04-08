/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.integration.neo4j.test;

import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedGraphDatabaseServer {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedGraphDatabaseServer.class);

    private GraphDatabaseServiceWrapper embeddedServer;

    public void start(Map<String, Object> ...parameters) {
        final String classpath = System.getProperty("classpath");
        LOG.info("Neo4j classpath: " + classpath);
        try {
            CustomClassLoader loader = new CustomClassLoader(classpath);
            Class<Object> loadedClass = (Class<Object>) loader.loadClass(GraphDatabaseServiceWrapperImpl.class.getCanonicalName());
            embeddedServer = (GraphDatabaseServiceWrapper) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                    new Class[]{GraphDatabaseServiceWrapper.class},
                    new PassThroughProxyHandler(loadedClass.newInstance()));
            embeddedServer.startEmbeddedServer(parameters);
        } catch (MalformedURLException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException ex) {
            LOG.warn("Error while creating and starting Neo4j server", ex);
        }
    }

    public void stop() {
        embeddedServer.stopEmbeddedServer();
        embeddedServer = null;
        System.gc();
    }
}
