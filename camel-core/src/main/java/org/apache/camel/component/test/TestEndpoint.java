/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.util.EndpointHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The test component extends the mock component by on startup to pull messages from another endpoint to set the expected message bodies.
 *
 * That is, you use the test endpoint in a route and messages arriving on it will be implicitly compared to some
 * expected messages extracted from some other location.
 * So you can use, for example, an expected set of message bodies as files.
 * This will then set up a properly configured Mock endpoint, which is only valid if the received messages
 * match the number of expected messages and their message payloads are equal.
 */
@UriEndpoint(scheme = "test", title = "Test", syntax = "test:name", producerOnly = true, label = "core,testing")
public class TestEndpoint extends MockEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(TestEndpoint.class);
    private final Endpoint expectedMessageEndpoint;
    @UriPath(description = "Name of endpoint to lookup in the registry to use for polling messages used for testing") @Metadata(required = "true")
    private String name;
    @UriParam(label = "producer", defaultValue = "2000")
    private long timeout = 2000L;

    public TestEndpoint(String endpointUri, Component component, Endpoint expectedMessageEndpoint) {
        super(endpointUri, component);
        this.expectedMessageEndpoint = expectedMessageEndpoint;
    }

    @Override
    protected void doStart() throws Exception {
        LOG.debug("Consuming expected messages from: {}", expectedMessageEndpoint);

        final List<Object> expectedBodies = new ArrayList<Object>();
        EndpointHelper.pollEndpoint(expectedMessageEndpoint, new Processor() {
            public void process(Exchange exchange) throws Exception {
                Object body = getInBody(exchange);
                LOG.trace("Received message body {}", body);
                expectedBodies.add(body);
            }
        }, timeout);

        LOG.debug("Received: {} expected message(s) from: {}", expectedBodies.size(), expectedMessageEndpoint);
        expectedBodiesReceived(expectedBodies);
    }

    /**
     * This method allows us to convert or coerce the expected message body into some other type
     */
    protected Object getInBody(Exchange exchange) {
        return exchange.getIn().getBody();
    }

    public long getTimeout() {
        return timeout;
    }

    /**
     * The timeout to use when polling for message bodies from the URI
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
