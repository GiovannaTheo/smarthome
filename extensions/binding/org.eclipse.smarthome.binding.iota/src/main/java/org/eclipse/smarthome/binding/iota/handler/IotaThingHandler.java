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
package org.eclipse.smarthome.binding.iota.handler;

import static org.eclipse.smarthome.binding.iota.handler.IotaConfiguration.*;

import java.math.BigDecimal;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.iota.internal.IotaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;

/**
 * The {@link IotaThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaThingHandler extends BaseThingHandler {

    // TODO: parse the json result to create channels like in the MQTT binding
    // TODO: add tests for both the io and this binding

    private final Logger logger = LoggerFactory.getLogger(IotaThingHandler.class);
    private String root;
    private final JsonArray data = new JsonArray();
    private int refresh = 0;
    private String mode;
    private IotaUtils utils;
    ScheduledFuture<?> refreshJob;

    public IotaThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if (command instanceof RefreshType) {
        // boolean success = fetchItemState();
        // if (success) {
        // updateAllStates(data);
        // }
        // } else {
        // logger.debug("Command {} is not supported for channel: {}", command, channelUID.getId());
        // }
    }

    @Override
    public void initialize() {

        logger.debug("Initializing Iota handler.");

        Configuration config = getThing().getConfiguration();

        setRoot(getOrDefault(config.get(ROOT_ADDRESS), getRoot()));
        setRefresh(getOrDefault(config.get(REFRESH_INTERVAL), getRefresh()));
        setMode(getOrDefault(config.get(MODE), getMode()));

        Bridge bridge = this.getBridge();
        if (bridge != null) {
            IotaBridgeHandler bridgeHandler = (IotaBridgeHandler) bridge.getHandler();
            if (bridgeHandler != null) {
                this.utils = bridgeHandler.getUtils();
                // startAutomaticRefresh();
            } else {
                logger.debug("Could not initialize Iota Utils for thing {}", this.getThing().getUID());
            }
        } else {
            logger.debug("Could not initialize Iota Utils for thing {}", this.getThing().getUID());
        }
    }

    @Override
    public void dispose() {
        refreshJob.cancel(true);
    }

    // @Override
    // public void handleRemoval() {
    // utils.getProcess().destroy();
    // refreshJob.cancel(true);
    // updateStatus(ThingStatus.REMOVED);
    // }

    /**
     *
     * listens on the tangle until data is retrieved
     */
//    private synchronized void startAutomaticRefresh() {
//        logger.debug("----------- Binding will refresh every {} seconds", refresh);
//        int interval = refresh == 0 ? 1 : refresh;
//        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
//            try {
//                boolean success = fetchItemState();
//                if (success) {
//                    updateAllStates(data);
//                }
//            } catch (Exception e) {
//                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
//                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
//            }
//        }, 0, interval, TimeUnit.SECONDS);
//    }

    /**
     *
     * @return success if any data is found in the MAM transaction
     */
//    private synchronized boolean fetchItemState() {
//        boolean success = false;
//        if (!root.isEmpty()) {
//            JsonParser parser = new JsonParser();
//            JsonObject resp = parser.parse(utils.fetchFromTangle(refresh, root, mode)).getAsJsonObject();
//            if (resp.size() != 0) {
//                root = resp.get("NEXTROOT").getAsString();
//                data = resp.entrySet().iterator().next().getValue().getAsJsonArray();
//                success = true;
//            }
//        } else {
//            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "Could not fetch data");
//        }
//        return success;
//    }

    /**
     * Updates all item states with the fetched data
     *
     * @param data
     */
//    private synchronized void updateAllStates(JsonArray data) {
//        logger.debug("Data were updated from the Tangle");
//        for (JsonElement el : data) {
//            logger.debug("----------------- data {}", el);
//            String state = el.getAsJsonObject().get("STATE").getAsString();
//            // TODO: add channel
//        }
//    }

    private static String getOrDefault(Object value, String defaultValue) {
        return value != null ? (String) value : defaultValue;
    }

    private static int getOrDefault(Object value, int defaultValue) {
        return value instanceof BigDecimal ? ((BigDecimal) value).intValue() : defaultValue;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public int getRefresh() {
        return refresh;
    }

    public void setRefresh(int refresh) {
        this.refresh = refresh;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
