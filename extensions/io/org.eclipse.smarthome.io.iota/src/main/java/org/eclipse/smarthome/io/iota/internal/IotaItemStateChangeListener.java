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
package org.eclipse.smarthome.io.iota.internal;

import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.StateChangeListener;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;

/**
 * Listens for changes to the state of registered items.
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaItemStateChangeListener implements StateChangeListener {

    private final Logger logger = LoggerFactory.getLogger(IotaItemStateChangeListener.class);
    private IotaAPI bridge;
    private final IotaUtils utils = new IotaUtils();
    private final JsonObject inputStates = new Gson().fromJson("{\"Items\":[]}", JsonObject.class);
    final Debouncer debouncer = new Debouncer();

    @Override
    public void stateChanged(@NonNull Item item, @NonNull State oldState, @NonNull State newState) {
        // this.addToStates(item, newState);
        // Publish the new states to the Tangle if no items see their state changed after 200 ms
        // debouncer.debounce(IotaItemStateChangeListener.class, new Runnable() {
        // @Override
        // public void run() {
        // utils.publishState(bridge, inputStates);
        // }
        // }, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stateUpdated(@NonNull Item item, @NonNull State state) {
        // not needed: the state has been updated but is identical to the oldState, therefore it is not necessary to
        // re-upload the state to the Tangle
        // For testing only
        this.addToStates(item, state);
        debouncer.debounce(IotaItemStateChangeListener.class, new Runnable() {
            @Override
            public void run() {
                utils.publishState(bridge, inputStates.get("Items"));
                JsonObject res = utils.getItemState(bridge);
            }
        }, 200, TimeUnit.MILLISECONDS);
    }

    public void setBridge(IotaAPI bridge) {
        this.bridge = bridge;
        if (this.bridge != null) {
            GetNodeInfoResponse getNodeInfoResponse = bridge.getNodeInfo();
            logger.debug("Iota connexion success: {}", getNodeInfoResponse);
        }
    }

    /**
     * Constructs a json object with all item names and states that need to be published on the Tangle
     *
     * @param item
     * @param state
     */
    public synchronized void addToStates(@NonNull Item item, @NonNull State state) {
        JsonObject newState = new JsonObject();
        if (this.inputStates.get("Items").getAsJsonArray().size() == 0) {
            newState.addProperty("Name", item.getName().toString());
            newState.addProperty("State", state.toFullString());
            newState.addProperty("Time", Instant.now().toString());
            this.inputStates.get("Items").getAsJsonArray().add(newState);
        } else {
            for (Iterator<JsonElement> it = this.inputStates.get("Items").getAsJsonArray().iterator(); it.hasNext();) {
                JsonElement el = it.next();
                String name = el.getAsJsonObject().get("Name").toString().replace("\"", "");
                if (name.equals(item.getName().toString())) {
                    // Item already tracked. Removing it to update value
                    it.remove();
                }
            }
            newState.addProperty("Name", item.getName().toString());
            newState.addProperty("State", state.toFullString());
            newState.addProperty("Time", Instant.now().toString());
            this.inputStates.get("Items").getAsJsonArray().add(newState);
        }
    }

    public void removeItemFromJson(@NonNull Item item) {
        for (Iterator<JsonElement> it = this.inputStates.get("Items").getAsJsonArray().iterator(); it.hasNext();) {
            JsonElement el = it.next();
            String name = el.getAsJsonObject().get("Name").toString().replace("\"", "");
            if (name.equals(item.getName().toString())) {
                it.remove();
            }
        }
    }

}
