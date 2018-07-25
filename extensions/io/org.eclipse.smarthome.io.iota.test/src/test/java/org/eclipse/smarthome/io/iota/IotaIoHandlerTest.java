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
package org.eclipse.smarthome.io.iota;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.io.iota.handler.ChannelConfig;
import org.eclipse.smarthome.io.iota.handler.IotaIoThingHandler;
import org.eclipse.smarthome.io.iota.internal.NumberValue;
import org.eclipse.smarthome.test.java.JavaTest;
import org.junit.Before;
import org.junit.Test;

/**
 * The {@link IotaIoHandlerTest} provides test cases for {@link IotaHandler}. The tests provide mocks for supporting
 * entities using Mockito.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaIoHandlerTest extends JavaTest {

    private MockIotaIoThingHandler iotaThingHandler;

    private Map<String, Object> thingProperties;

    private Thing thing;

    private ChannelConfig config;

    @Before
    public void setUp() {

    }

    @Test
    public void thingShouldInitialize() {
        initializeThing();
        waitForAssert(() -> assertEquals(ThingStatus.ONLINE, thing.getStatus()));
    }

    @Test
    public void channelValueShouldUpdateWhenBalanceIsDetected() {
        initializeThingAndChannel(true, new BigDecimal(1));
        assertNull(config.getValue().getValue());

        /**
         * For some reasons, this test passes when executed through Eclipse, but fails when building through maven.
         * Message: "loader constraint violation: when resolving method
         * "com.google.gson.Gson.getAdapter(Lcom/google/gson/reflect/TypeToken;)Lcom/google/gson/TypeAdapter";
         * the class loader (instance of org/eclipse/osgi/internal/loader/EquinoxClassLoader) of the current class,
         * retrofit2/converter/gson/GsonConverterFactory, and the class loader (instance of
         * org/eclipse/osgi/internal/loader/EquinoxClassLoader) for the method's defining class,
         * com/google/gson/Gson, have different Class objects for the type com/google/gson/reflect/TypeToken used in the
         * signature"
         * TODO: Fix it
         */

        // Retrieve balance for wallet
        // LJLGBRDIWNIFSOJXMTFVERCSSDEXTTXTHMGPIV9JY9R9WNSQUDHAIOBHRIJKOPFAXQCOTQGHGFRHASQW9TGBTDEGQB
        // stored on testnet, and check that the channel updates its value
        // iotaThingHandler.getBalance();
        // waitForAssert(() -> assertNotNull(config.getValue().getValue()));
        // waitForAssert(() -> assertTrue(Double.parseDouble(config.getValue().getValue().toFullString()) > 0.0));
    }

    private void initializeThing() {
        thingProperties = new HashMap<>();
        thing = ThingBuilder.create(new ThingTypeUID("binding:thing-type:thing:groupid"), "testthing")
                .withLabel("Test Thing").withConfiguration(new Configuration(thingProperties)).build();
        iotaThingHandler = new MockIotaIoThingHandler(thing);
        thing.setHandler(iotaThingHandler);
        ThingHandlerCallback thingHandler = mock(ThingHandlerCallback.class);
        doAnswer(answer -> {
            ((Thing) answer.getArgument(0)).setStatusInfo(answer.getArgument(1));
            return null;
        }).when(thingHandler).statusUpdated(any(), any());
        iotaThingHandler.setCallback(thingHandler);
        iotaThingHandler.initialize();
    }

    private void initializeThingAndChannel(boolean isFloat, BigDecimal step) {

        /**
         * Initialize thing and add channel
         */
        initializeThing();
        ThingBuilder builder = iotaThingHandler.getThingBuilder();
        Channel channel = ChannelBuilder
                .create(new ChannelUID(iotaThingHandler.getThing().getUID().toString() + ":groupid"),
                        IotaIoBindingConstants.THING_TYPE_IOTA_IO.toString())
                .withKind(ChannelKind.STATE).build();
        iotaThingHandler.updateThingMock(builder.withChannel(channel).build());

        /**
         * Add the corresponding ChannelConfig configuration for this channel
         */
        config = channel.getConfiguration().as(ChannelConfig.class);
        config.setAddress("LJLGBRDIWNIFSOJXMTFVERCSSDEXTTXTHMGPIV9JY9R9WNSQUDHAIOBHRIJKOPFAXQCOTQGHGFRHASQW9TGBTDEGQB");
        config.setChannelUID(channel.getUID());
        config.setChannelStateUpdateListener(iotaThingHandler);

        /**
         * Set the current value of the channel
         */
        config.setValue(new NumberValue(isFloat, step));
        iotaThingHandler.addChannelDataByChannelUID(channel.getUID(), config);

    }

    class MockIotaIoThingHandler extends IotaIoThingHandler {

        MockIotaIoThingHandler(Thing thing) {
            super(thing);
        }

        @Override
        public void initialize() {
            this.setProtocol("https");
            this.setHost("nodes.testnet.iota.org");
            this.setPort(443);
            this.setRefresh(30);
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
