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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.Instant;

import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.binding.iota.handler.IotaThingHandler;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.iota.utils.IotaUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The {@link IotaHandlerTest} provides test cases for {@link IotaHandler}. The tests provide mocks for supporting
 * entities using Mockito.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaHandlerTest {

    private MockIotaThingHandler thingHandler;
    private Item item;
    private State state;
    private JsonObject data;
    private JsonObject itemName;
    private JsonObject itemState;

    @Mock
    private ThingHandlerCallback thingCallback;

    @Mock
    private Thing thing;

    @Before
    public void setUp() {
        initMocks(this);
        thingHandler = new MockIotaThingHandler(thing);
        thingHandler.setCallback(thingCallback);
        item = new NumberItem("item");
        state = new QuantityType<Temperature>(Double.parseDouble("10"), SIUnits.CELSIUS);
        ((GenericItem) item).setState(state);
        ((GenericItem) item).setCategory("TEMPERATURE");
        data = new Gson().fromJson("{\"Items\":[]}", JsonObject.class);
        itemName = new JsonObject();
        itemState = new JsonObject();
        itemState.addProperty("Topic", item.getCategory().toString());
        itemState.addProperty("State", state.toFullString());
        itemState.addProperty("Time", Instant.now().toString());
        itemName.addProperty("Name", item.getName().toString());
        itemName.add("Status", itemState);
        data.get("Items").getAsJsonArray().add(itemName);
    }

    @Test
    public void initializeThingShouldCallTheCallback() {
        thingHandler.initialize();
        ArgumentCaptor<ThingStatusInfo> statusInfoCaptor = ArgumentCaptor.forClass(ThingStatusInfo.class);
        verify(thingCallback).statusUpdated(eq(thing), statusInfoCaptor.capture());
        ThingStatusInfo thingStatusInfo = statusInfoCaptor.getValue();
        assertThat(thingStatusInfo.getStatus(), is(equalTo(ThingStatus.ONLINE)));
    }

    @Test
    public void stateShouldBeUpdatedGivenJsonData() {
        // TODO: update channel with the data
    }

    class MockIotaThingHandler extends IotaThingHandler {

        public MockIotaThingHandler(Thing thing) {
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
    }
}
