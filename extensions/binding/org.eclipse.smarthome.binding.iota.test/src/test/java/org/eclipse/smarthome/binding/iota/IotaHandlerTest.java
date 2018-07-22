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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.binding.iota.IotaBridgeHandlerTest.MockIotaBridgeHandler;
import org.eclipse.smarthome.binding.iota.handler.ChannelConfig;
import org.eclipse.smarthome.binding.iota.handler.IotaThingHandler;
import org.eclipse.smarthome.binding.iota.internal.TextValue;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.io.iota.utils.IotaUtils;
import org.eclipse.smarthome.test.java.JavaTest;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The {@link IotaHandlerTest} provides test cases for {@link IotaHandler}. The tests provide mocks for supporting
 * entities using Mockito.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaHandlerTest extends JavaTest {

    private MockIotaBridgeHandler iotaBridgeHandler;
    private MockIotaThingHandler iotaThingHandler;

    private Map<String, Object> bridgeProperties;
    private Map<String, Object> thingProperties;

    private Bridge bridge;
    private Thing thing;

    private JsonObject data;
    private JsonObject itemName;
    private JsonObject itemState;
    private ChannelConfig config;

    @Before
    public void setUp() {
        /**
         * Mimic json data retrieved through MAM
         */
        data = new Gson().fromJson("{\"Items\":[]}", JsonObject.class);
        itemName = new JsonObject();
        itemState = new JsonObject();
        itemState.addProperty("TOPIC", "TEMPERATURE");
        itemState.addProperty("STATE", "2.0 °C");
        itemState.addProperty("TIME", Instant.now().toString());
        itemName.addProperty("NAME", "item");
        itemName.add("STATUS", itemState);
        data.get("Items").getAsJsonArray().add(itemName);
    }

    @Test
    public void thingShouldInitialize() {
        initializeThing();
        waitForAssert(() -> assertEquals(ThingStatus.ONLINE, thing.getStatus()));
    }

    @Test
    public void bridgeShouldInitialize() {
        initializeBridge();
        waitForAssert(() -> assertEquals(ThingStatus.ONLINE, bridge.getStatus()));
    }

    @Test
    public void stateShouldBeUpdatedGivenJsonData() {
        // Initialize thing and set value to channel
        initializeThingAndChannel("1.0 °C", "TEMPERATURE");
        // Current state value should be 1.0 °C
        assertEquals("1.0 °C", config.getValue().getValue().toFullString());
        // Updating the channel value with the Json Data
        iotaThingHandler.updateAllStates(data.get("Items").getAsJsonArray());
        // New state value should be 2.0 °C
        assertEquals("2.0 °C", config.getValue().getValue().toFullString());
    }

    @Test
    public void stateShouldNotUpdateIfStateTopicIsDifferentFromData() {
        // Initialize thing and set value to channel
        initializeThingAndChannel("1.0 °C", "PRESSURE");
        // Current state value should be 1.0 °C
        assertEquals("1.0 °C", config.getValue().getValue().toFullString());
        // Updating the channel value with the Json Data
        iotaThingHandler.updateAllStates(data.get("Items").getAsJsonArray());
        // Since this channel's topic is set to "PRESSURE" and that the topic of the Json Data is set to "TEMPERATURE"
        // it is expected that the channel's value is not updated
        // New state value should be 1.0 °C
        assertEquals("1.0 °C", config.getValue().getValue().toFullString());
    }

    @Test
    public void stateShouldUpdateIfHasAnyWildcard() {
        // Initialize thing and set value to channel
        initializeThingAndChannel("1.0 °C", "ANY");
        // Current state value should be 1.0 °C
        assertEquals("1.0 °C", config.getValue().getValue().toFullString());
        // Updating the channel value with the Json Data
        iotaThingHandler.updateAllStates(data.get("Items").getAsJsonArray());
        // New state value should be 2.0 °C
        assertEquals("2.0 °C", config.getValue().getValue().toFullString());
    }

    @Test
    public void thingStatusShouldChangeIfBridgeStatusDoes() {
        initializeThing();
        // Check that the thing is online
        waitForAssert(() -> assertEquals(ThingStatus.ONLINE, thing.getStatus()));
        // Check that thing properly follows bridge status
        ThingHandler handler = thing.getHandler();
        assertNotNull(handler);
        handler.bridgeStatusChanged(ThingStatusInfoBuilder.create(ThingStatus.OFFLINE).build());
        waitForAssert(() -> assertEquals(ThingStatus.OFFLINE, thing.getStatusInfo().getStatus()));
        handler.bridgeStatusChanged(ThingStatusInfoBuilder.create(ThingStatus.ONLINE).build());
        waitForAssert(() -> assertEquals(ThingStatus.ONLINE, thing.getStatusInfo().getStatus()));
    }

    private void initializeThingAndChannel(String value, String topic) {

        /**
         * Initialize thing and add channel
         */
        initializeThing();
        ThingBuilder builder = iotaThingHandler.getThingBuilder();
        Channel channel = ChannelBuilder
                .create(new ChannelUID(iotaThingHandler.getThing().getUID().toString() + ":groupid"),
                        IotaBindingConstants.THING_TYPE_IOTA.toString())
                .withKind(ChannelKind.STATE).build();
        iotaThingHandler.updateThingMock(builder.withChannel(channel).build());

        /**
         * Add the corresponding ChannelConfig configuration for this channel
         */
        config = channel.getConfiguration().as(ChannelConfig.class);
        config.setChannelUID(channel.getUID());
        config.setChannelStateUpdateListener(iotaThingHandler);

        /**
         * Set the current value of the channel
         */
        config.setValue(new TextValue(value));
        config.setStateTopic(topic);
        iotaThingHandler.addChannelDataByChannelUID(channel.getUID(), config);

    }

    private void initializeBridge() {
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

    private void initializeThing() {
        initializeBridge();
        waitForAssert(() -> assertEquals(ThingStatus.ONLINE, bridge.getStatus()));
        thingProperties = new HashMap<>();
        thing = ThingBuilder.create(new ThingTypeUID("binding:thing-type:thing:groupid"), "testthing")
                .withLabel("Test Thing").withConfiguration(new Configuration(thingProperties))
                .withBridge(bridge.getBridgeUID()).build();
        iotaThingHandler = new MockIotaThingHandler(thing);
        thing.setHandler(iotaThingHandler);
        ThingHandlerCallback thingHandler = mock(ThingHandlerCallback.class);
        doAnswer(answer -> {
            ((Thing) answer.getArgument(0)).setStatusInfo(answer.getArgument(1));
            return null;
        }).when(thingHandler).statusUpdated(any(), any());
        iotaThingHandler.setCallback(thingHandler);
        iotaThingHandler.initialize();
    }

    class MockIotaThingHandler extends IotaThingHandler {

        MockIotaThingHandler(Thing thing) {
            super(thing);
        }

        @Override
        public void initialize() {
            this.setRoot("ZVFYRCUYNBIGJXCKRW9DGBMVMONWSQWFY9GZNDBOD9OZEATN9RXRHKXFCB9LVYWURVTHTXJGQW9VHVNTI");
            this.setMode("public");
            this.setRefresh(60);
            this.setKey(null);
            this.setUtils(new IotaUtils("https", "nodes.testnet.iota.org", 443));
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
        }

        ThingBuilder getThingBuilder() {
            return this.editThing();
        }

        void updateThingMock(Thing thing) {
            updateThing(thing);
        }
    }
}
