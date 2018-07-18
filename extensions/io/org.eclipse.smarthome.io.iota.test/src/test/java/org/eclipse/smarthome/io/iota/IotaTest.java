/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.iota;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.iota.internal.IotaItemStateChangeListener;
import org.eclipse.smarthome.io.iota.internal.IotaSeedGenerator;
import org.eclipse.smarthome.io.iota.utils.IotaUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The {@link IotaTest} provides tests cases for Iota IO classes. The tests provide mocks for supporting entities using
 * Mockito.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaTest {

    private IotaUtils utils;
    private IotaItemStateChangeListener itemStateChangeListener;
    private Item item1;
    private Item item2;
    private State state1;
    private State state2;

    @Mock
    private Thing thing;

    @Before
    public void setUp() {
        initMocks(this);
        itemStateChangeListener = new IotaItemStateChangeListener();
        IotaSeedGenerator gen = new IotaSeedGenerator();
        String seed = gen.getNewSeed();
        utils = new IotaUtils("https", "nodes.testnet.iota.org", 443, seed, -1);
        item1 = new NumberItem("item1");
        item2 = new NumberItem("item2");
        state1 = new QuantityType<Temperature>(Double.parseDouble("10"), SIUnits.CELSIUS);
        state2 = new QuantityType<Dimensionless>(Double.parseDouble("85"), SmartHomeUnits.PERCENT);
        ((GenericItem) item1).setState(state1);
        ((GenericItem) item2).setState(state2);
    }

    @Test
    public void apiShouldConnect() {
        // assertThat(utils.checkAPI(), is(equalTo(true)));
    }

    @Test
    public void generatedSeedShouldBeValid() {
        IotaSeedGenerator gen = new IotaSeedGenerator();
        assertThat(utils.checkSeed(gen.getNewSeed()), is(equalTo(true)));
    }

    @Test
    public void newStateShouldAddToExistingStates() {
        JsonObject existingStates = new Gson().fromJson("{\"Items\":[]}", JsonObject.class);
        // No items have been added yet
        assertThat(existingStates.get("Items").getAsJsonArray().size(), is(equalTo(0)));
        // Adding state 1
        existingStates = itemStateChangeListener.addToStates(item1, item1.getState(), existingStates);
        // Size of the array should now be 1
        assertThat(existingStates.get("Items").getAsJsonArray().size(), is(equalTo(1)));
        // Adding state 2
        existingStates = itemStateChangeListener.addToStates(item2, item2.getState(), existingStates);
        // Size of the array should now be 2
        assertThat(existingStates.get("Items").getAsJsonArray().size(), is(equalTo(2)));
        // Adding the same state twice should not change the array size, but should update the element
        existingStates = itemStateChangeListener.addToStates(item2, item2.getState(), existingStates);
        // Size of the array should still be 2
        assertThat(existingStates.get("Items").getAsJsonArray().size(), is(equalTo(2)));
    }

    @Test
    public void existingStateShouldDelete() {
        JsonObject existingStates = new Gson().fromJson("{\"Items\":[]}", JsonObject.class);
        // Adding state 1
        existingStates = itemStateChangeListener.addToStates(item1, item1.getState(), existingStates);
        // Size of the array should now be 1
        assertThat(existingStates.get("Items").getAsJsonArray().size(), is(equalTo(1)));
        IotaSeedGenerator gen = new IotaSeedGenerator();
        String seed = gen.getNewSeed();
        itemStateChangeListener.addSeedByUID(item1.getUID(), seed);
        itemStateChangeListener.addJsonObjectBySeed(seed, existingStates);
        // Removing the state
        itemStateChangeListener.removeItemFromJson(item1);
        // Size of the array should now be 0
        assertThat(existingStates.get("Items").getAsJsonArray().size(), is(equalTo(0)));
    }

}
