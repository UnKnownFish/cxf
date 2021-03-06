/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.jaxrs.failover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A test for failover using a WebClient object
 */
public class FailoverWebClientTest extends AbstractBusClientServerTestBase {
    static final String PORT1 = allocatePort(FailoverBookServer.class);
    static final String PORT2 = allocatePort(FailoverBookServer.class, 2);
    static final String PORT3 = allocatePort(FailoverBookServer.class, 3);


    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(FailoverBookServer.class, true));
        createStaticBus();
    }

    @Test
    public void testFailover() throws Exception {
        String address = "http://localhost:" + PORT1 + "/bookstore";

        FailoverFeature failoverFeature = new FailoverFeature();
        SequentialStrategy strategy = new SequentialStrategy();
        List<String> addresses = new ArrayList<>();
        addresses.add("http://localhost:" + PORT2 + "/bookstore");
        addresses.add("http://localhost:" + PORT3 + "/bookstore");
        strategy.setAlternateAddresses(addresses);
        failoverFeature.setStrategy(strategy);

        WebClient webClient = WebClient.create(address, null,
                                               Collections.singletonList(failoverFeature),
                                               null).accept("application/xml");
        // Should hit PORT1
        Book b = webClient.get(Book.class);
        assertEquals(124L, b.getId());
        assertEquals("root", b.getName());
        assertEquals("http://localhost:" + PORT1 + "/bookstore",
                     webClient.getBaseURI().toString());

        // Should failover to PORT2
        webClient.get(Book.class);
        assertEquals(124L, b.getId());
        assertEquals("root", b.getName());
        assertEquals("http://localhost:" + PORT2 + "/bookstore",
                     webClient.getBaseURI().toString());

        // Should failover to PORT3
        webClient.get(Book.class);
        assertEquals(124L, b.getId());
        assertEquals("root", b.getName());
        assertEquals("http://localhost:" + PORT3 + "/bookstore",
                     webClient.getBaseURI().toString());
    }

}