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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.StateChangeListener;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.iota.metadata.IotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;

/**
 * Listens for changes to the state of registered items.
 * Each item wishing to publish its state on the Tangle has a seed associated to its UID.
 * A custom seed may be given to some item. This way, any ESH instance is able to select
 * which item's state to share on which channel.
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaItemStateChangeListener implements StateChangeListener {

    private final Logger logger = LoggerFactory.getLogger(IotaItemStateChangeListener.class);
    private IotaAPI bridge;
    private final HashMap<String, JsonObject> jsonObjectBySeed = new HashMap<>();
    private final HashMap<String, String> seedByUID = new HashMap<>();
    private final HashMap<String, Debouncer> debouncerBySeed = new HashMap<>();
    private final HashMap<String, IotaUtils> utilsBySeed = new HashMap<>();
    private final HashMap<String, String> privateKeyBySeed = new HashMap<>();
    private final HashMap<String, Boolean> paymentReceivedBySeed = new HashMap<>();
    private final HashMap<String, Boolean> handshakeBySeed = new HashMap<>();
    private final HashMap<String, String> seedByWallet = new HashMap<>();
    private final HashMap<String, Double> paymentAmountByWallet = new HashMap<>();
    private final HashMap<String, BigInteger[]> rsaKeysBySeed = new HashMap<>();
    private IotaService service;

    @Override
    public void stateChanged(@NonNull Item item, @NonNull State oldState, @NonNull State newState) {
        service.getMetadataRegistry().getAll().forEach(metadata -> {
            if (metadata.getUID().getItemName().equals(item.getName())) {
                String seed = seedByUID.get(item.getUID());

                if (jsonObjectBySeed.containsKey(seed)) {
                    /**
                     * Entries already exists. Updating the value in the json array
                     */
                    JsonObject oldEntries = jsonObjectBySeed.get(seed);
                    JsonObject newEntries = addToStates(item, newState, oldEntries);
                    jsonObjectBySeed.put(seed, newEntries);
                } else {
                    /**
                     * New entry, creating a new json object
                     */
                    jsonObjectBySeed.put(seed,
                            addToStates(item, newState, new Gson().fromJson("{\"Items\":[]}", JsonObject.class)));
                }

                String mode = metadata.getConfiguration().get("mode").toString();

                double p = 0.0;
                if (metadata.getConfiguration().get("price") instanceof BigDecimal) {
                    p = ((BigDecimal) metadata.getConfiguration().get("price")).doubleValue();
                } else {
                    p = (double) metadata.getConfiguration().get("price");
                }

                final double price = p;

                Debouncer debouncer = debouncerBySeed.get(seed);

                if (debouncer != null) {
                    /**
                     * If several items publish on the same channel, the debounce mechanism bellow
                     * makes sure the data are published only once if no item has requested an update
                     * within 1 second
                     */
                    debouncer.debounce(IotaItemStateChangeListener.class, new Runnable() {
                        @Override
                        public void run() {
                            if (mode.equals("restricted")) {
                                if (price == 0.0) {
                                    // Normal restricted mode with publisher-chosen password
                                    utilsBySeed.get(seed).publishState(jsonObjectBySeed.get(seed).get("Items"), mode,
                                            privateKeyBySeed.get(seed));
                                } else {
                                    // Auto-compensation mechanism
                                    if (handshakeBySeed.get(seed) == false) {
                                        // Sending handshake packet
                                        if (!metadata.getConfiguration().get("wallet").toString().isEmpty()) {

                                            /**
                                             * Constructing the handshake packet and start the handshake protocol
                                             */

                                            JsonObject handshakeJson = new JsonObject();
                                            String wallet = metadata.getConfiguration().get("wallet").toString();
                                            handshakeJson.addProperty("Type", "handshake");
                                            handshakeJson.addProperty("Wallet", wallet);
                                            handshakeJson.addProperty("Price", price);
                                            JsonObject rsaPublic = new JsonObject();
                                            rsaPublic.addProperty("Modulus", rsaKeysBySeed.get(seed)[0].toString());
                                            rsaPublic.addProperty("Exponent", rsaKeysBySeed.get(seed)[1].toString());
                                            handshakeJson.add("RSA", rsaPublic);
                                            utilsBySeed.get(seed).startHandshake(handshakeJson,
                                                    privateKeyBySeed.get(seed));
                                            handshakeBySeed.put(seed, true); // indicating that hanshake is completed

                                        } else {
                                            logger.warn("Wallet address cannot be empty. Aborting.");
                                        }
                                    }

                                    if (paymentReceivedBySeed.get(seed) == true) {
                                        // Data have been paid. Processing normally
                                        utilsBySeed.get(seed).publishState(jsonObjectBySeed.get(seed).get("Items"),
                                                mode, privateKeyBySeed.get(seed));
                                    }
                                }
                            } else {
                                utilsBySeed.get(seed).publishState(jsonObjectBySeed.get(seed).get("Items"), mode, null);
                            }
                        }
                    }, 1000, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    @Override
    public void stateUpdated(@NonNull Item item, @NonNull State state) {
        /**
         * Not needed. State has been updated but is no different from the latest push on the Tangle.
         * Note: for testing purpose, you can call stateChanged(item, state, state) to re-publish
         * data to the Tangle.
         */
        stateChanged(item, state, state);
    }

    public void setBridge(IotaAPI bridge) {
        this.bridge = bridge;
        if (this.bridge != null) {
            GetNodeInfoResponse getNodeInfoResponse = this.bridge.getNodeInfo();
            logger.debug("Iota connexion success: {}", getNodeInfoResponse);
        }
    }

    public void setService(IotaService service) {
        this.service = service;
    }

    /**
     * Constructs a JSON object with all item names and states that will be published on the Tangle
     *
     * @param item
     * @param state
     */
    public synchronized JsonObject addToStates(@NonNull Item item, @NonNull State state, JsonObject json) {

        JsonObject itemName = new JsonObject();
        JsonObject itemState = new JsonObject();

        if (json.get("Items").getAsJsonArray().size() == 0) {
            if (item.getCategory() != null) {
                itemState.addProperty("Topic", item.getCategory().toString());
            }
            itemState.addProperty("State", state.toFullString());
            itemState.addProperty("Time", Instant.now().toString());

            itemName.addProperty("Name", item.getName().toString());
            itemName.add("Status", itemState);

            json.get("Items").getAsJsonArray().add(itemName);
        } else {
            for (Iterator<JsonElement> it = json.get("Items").getAsJsonArray().iterator(); it.hasNext();) {
                JsonElement el = it.next();

                String name = el.getAsJsonObject().get("Name").toString().replace("\"", "");
                if (name.equals(item.getName().toString())) {
                    // Item already tracked. Removing it to update value
                    it.remove();
                }
            }
            if (item.getCategory() != null) {
                itemState.addProperty("Topic", item.getCategory().toString());
            }
            itemState.addProperty("State", state.toFullString());
            itemState.addProperty("Time", Instant.now().toString());

            itemName.addProperty("Name", item.getName().toString());
            itemName.add("Status", itemState);

            json.get("Items").getAsJsonArray().add(itemName);
        }

        return json;

    }

    /**
     * Cleaning json struct: an item has been removed in the Paper UI, therefore its state
     * will not be published to the Tangle anymore
     *
     * @param item
     */
    public void removeItemFromJson(@NonNull Item item) {

        String seed = seedByUID.get(item.getUID());
        if (seed != null && !seed.isEmpty()) {
            for (Iterator<JsonElement> it = jsonObjectBySeed.get(seed).get("Items").getAsJsonArray().iterator(); it
                    .hasNext();) {
                JsonElement el = it.next();
                String name = el.getAsJsonObject().get("Name").toString().replace("\"", "");
                if (name.equals(item.getName().toString())) {
                    it.remove();
                }
            }
        }
    }

    public HashMap<String, JsonObject> getJsonObjectBySeed() {
        return jsonObjectBySeed;
    }

    public HashMap<String, String> getSeedByUID() {
        return seedByUID;
    }

    public HashMap<String, Debouncer> getDebouncerBySeed() {
        return debouncerBySeed;
    }

    public HashMap<String, IotaUtils> getUtilsBySeed() {
        return utilsBySeed;
    }

    public HashMap<String, String> getPrivateKeyBySeed() {
        return privateKeyBySeed;
    }

    public HashMap<String, Boolean> getPaymentReceivedBySeed() {
        return paymentReceivedBySeed;
    }

    public HashMap<String, Boolean> getHandshakeBySeed() {
        return handshakeBySeed;
    }

    public HashMap<String, String> getSeedByWallet() {
        return seedByWallet;
    }

    public HashMap<String, Double> getPaymentAmountByWallet() {
        return paymentAmountByWallet;
    }

    public HashMap<String, BigInteger[]> getRsaKeysBySeed() {
        return rsaKeysBySeed;
    }

}
