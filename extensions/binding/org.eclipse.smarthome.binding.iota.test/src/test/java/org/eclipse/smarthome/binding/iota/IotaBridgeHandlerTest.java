/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http:www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.binding.iota;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import org.eclipse.smarthome.binding.iota.handler.IotaBridgeHandler;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.io.iota.utils.IotaUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * The {@link IotaBridgeHandlerTest} provides test cases for the {@link IotaBridgeHandler}. The tests provide mocks for
 * supporting entities using Mockito.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaBridgeHandlerTest {

    private MockIotaBridgeHandler bridgeHandler;

    @Mock
    private Bridge bridge;

    @Before
    public void setUp() {
        initMocks(this);
        bridgeHandler = new MockIotaBridgeHandler(bridge);
    }

    @Test
    public void checkDefaultApiIsOnline() {
        bridgeHandler.initialize();
        assertThat(bridgeHandler.getUtils().checkAPI(), is(equalTo(true)));
    }

    class MockIotaBridgeHandler extends IotaBridgeHandler {

        public MockIotaBridgeHandler(Bridge bridge) {
            super(bridge);
        }

        @Override
        public void initialize() {
            this.setProtocol("https");
            this.setHost("nodes.testnet.iota.org");
            this.setPort(443);
            this.setUtils(new IotaUtils(this.getProtocol(), this.getHost(), this.getPort()));
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
        }

    }
}
