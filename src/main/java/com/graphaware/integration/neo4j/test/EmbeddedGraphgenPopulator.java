/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.integration.neo4j.test;

import com.graphaware.test.data.GraphgenPopulator;
import java.io.IOException;

/**
 *
 * @author ale
 */
public class EmbeddedGraphgenPopulator extends GraphgenPopulator {

    private final String filePath;
    public EmbeddedGraphgenPopulator(String filePath) {
        this.filePath = filePath;
    }
    
    @Override
    protected String file() throws IOException {
        return filePath;
    }
    
}
