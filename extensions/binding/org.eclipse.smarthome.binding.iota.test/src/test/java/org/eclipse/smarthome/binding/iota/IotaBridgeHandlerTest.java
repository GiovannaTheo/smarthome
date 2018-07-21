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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.binding.iota.handler.IotaBridgeHandler;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder;
import org.eclipse.smarthome.io.iota.utils.IotaUtils;
import org.eclipse.smarthome.test.java.JavaTest;
import org.junit.Before;
import org.junit.Test;

/**
 * The {@link IotaBridgeHandlerTest} provides test cases for the {@link IotaBridgeHandler}. The tests provide mocks for
 * supporting entities using Mockito.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaBridgeHandlerTest extends JavaTest {

    private MockIotaBridgeHandler iotaBridgeHandler;
    private Map<String, Object> bridgeProperties;
    private Bridge bridge;

    @Before
    public void setUp() {
        initializeBridge();
    }

    @Test
    public void bridgeShouldInitialize() {
        waitForAssert(() -> assertEquals(ThingStatus.ONLINE, bridge.getStatus()));
    }

    @Test
    public void checkDefaultApiIsOnline() {
        assertEquals(iotaBridgeHandler.getUtils().checkAPI(), true);
    }

    public void initializeBridge() {
        bridgeProperties = new HashMap<>();
        bridge = BridgeBuilder.create(new ThingTypeUID("iota", "test-bridge"), "testbridge").withLabel("Test Bridge")
                .withConfiguration(new Configuration(bridgeProperties)).build();
        iotaBridgeHandler = new IotaBridgeHandlerTest().new MockIotaBridgeHandler(bridge);
        bridge.setHandler(iotaBridgeHandler);
        ThingHandlerCallback bridgeHandler = mock(ThingHandlerCallback.class);
        doAnswer(answer -> {
            ((Thing) answer.getArgument(0)).setStatusInfo(answer.getArgument(1));
            return null;
        }).when(bridgeHandler).statusUpdated(any(), any());
        iotaBridgeHandler.setCallback(bridgeHandler);
        iotaBridgeHandler.initialize();
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
