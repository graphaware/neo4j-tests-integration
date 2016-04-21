/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.integration.neo4j.test;

import java.io.File;
import static java.lang.System.getProperty;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.neo4j.driver.v1.Config;
import static org.neo4j.driver.v1.Config.EncryptionLevel.NONE;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

/**
 *
 * @author ale
 */
public class GraphDatabaseTestServerTest {

    private static final File DEFAULT_KNOWN_HOSTS = new File(getProperty("user.home"),
            ".neo4j" + File.separator + "known_hosts");

    public GraphDatabaseTestServerTest() {
    }

    @After
    public void clear() {
        deleteDefaultKnownCertFileIfExists();
    }

    @Test
    public void testBolt() {
        System.out.println("testBolt");
        GraphDatabaseTestServer instance = new GraphDatabaseTestServer.Builder().enableBolt(true).build();
        try (Driver driver = GraphDatabase.driver(instance.url(), Config.build().withEncryptionLevel(NONE).toConfig())) {
            Session session = driver.session();
            StatementResult result = session.run("CREATE (n) RETURN n");
            int theOnesCreated = result.consume().counters().nodesCreated();
            assertEquals(theOnesCreated, 1);
            session.close();
        }
    }

    @Test
    public void testNonBolt() {
        System.out.println("testNonBolt");
        GraphDatabaseTestServer instance = new GraphDatabaseTestServer.Builder().port(7474).build();
        System.out.println("testNonBolt");

    }
    
    
//    @Test
//    public void testWrapper() {
//        System.out.println("testWrapper");
//        EmbeddedGraphDatabaseServer server =  new EmbeddedGraphDatabaseServer();
//        server.start();
//        try (Driver driver = GraphDatabase.driver(server.getURL())) {
//            Session session = driver.session();
//            StatementResult result = session.run("CREATE (n) RETURN n");
//            int theOnesCreated = result.consume().counters().nodesCreated();
//            assertEquals(theOnesCreated, 1);
//            session.close();
//        }
//        System.out.println("testWrapper");
//
//    }

    public static void deleteDefaultKnownCertFileIfExists() {
        if (DEFAULT_KNOWN_HOSTS.exists()) {
            DEFAULT_KNOWN_HOSTS.delete();
        }
    }

}
