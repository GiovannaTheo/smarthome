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

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jota.IotaAPI;
import jota.dto.response.FindTransactionResponse;
import jota.error.NoNodeInfoException;
import jota.utils.InputValidator;

/**
 * Provides utils methods to work with IOTA transactions
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaUtils {

    private final Logger logger = LoggerFactory.getLogger(IotaUtils.class);
    private static final String PATH = "../../extensions/io/org.eclipse.smarthome.io.iota/lib/mam/example/";
    private String seed = null;
    private int start = -1;
    private IotaAPI bridge;
    Process process;
    private String oldResult = "";

    public IotaUtils() {

    }

    public IotaUtils(String protocol, String host, int port) {
        this.bridge = new IotaAPI.Builder().protocol(protocol).host(host).port(String.valueOf(port)).build();
    }

    public IotaUtils(String protocol, String host, int port, String seed, int start) {
        this.bridge = new IotaAPI.Builder().protocol(protocol).host(host).port(String.valueOf(port)).build();
        this.seed = seed;
        this.start = start;
    }

    /**
     * Attach an item state on the Tangle, through MAM
     *
     * @param bridge the IOTA API endpoint
     * @param item   the item for which we want to publish data
     * @param state  the item's state
     */
    protected void publishState(JsonElement states, String mode, String key) {

        String payload = states.toString();
        String[] param = null;
        JsonParser parser = new JsonParser();

        switch (mode) {
            case "public":
            case "private":
                param = start == -1
                        ? new String[] { "/usr/local/bin/node", PATH + "publish.js",
                                bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(),
                                payload, mode, seed }
                        : new String[] { "/usr/local/bin/node", PATH + "publish.js",
                                bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(),
                                payload, mode, seed, String.valueOf(start) };
                break;
            case "restricted":
                if (key != null && !key.isEmpty()) {
                    param = start == -1
                            ? new String[] { "/usr/local/bin/node", PATH + "publish.js",
                                    bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(),
                                    payload, mode, key, seed }
                            : new String[] { "/usr/local/bin/node", PATH + "publish.js",
                                    bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(),
                                    payload, mode, key, seed, String.valueOf(start) };
                } else {
                    logger.warn("You must provide a key to use the restricted mode. Aborting");
                }
                break;
            default:
                logger.warn("This mode is not supported");
                break;
        }

        try {
            if (param != null) {
                logger.debug("Doing proof of work to attach data to the Tangle.... Processing in mode -- {} -- ", mode);
                process = Runtime.getRuntime().exec(param);
                String result = IOUtils.toString(process.getInputStream(), "UTF-8");
                if (result != null && !result.isEmpty()) {
                    JsonObject json = (JsonObject) parser.parse(result);
                    start = json.getAsJsonPrimitive("START").getAsInt();
                    seed = json.getAsJsonPrimitive("SEED").getAsString();
                    logger.debug("Sent: {}", json);
                }
                process.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            logger.debug("Exception happened: {}", e);
        }
    }

    /**
     * Retrieve item states from the Tangle, through MAM
     *
     * @param bridge the IOTA API endpoint
     */
    public String fetchFromTangle(int refresh, String root, String mode, String key) {
        String[] cmd;
        JsonParser parser = new JsonParser();
        JsonObject json = new JsonObject();
        if (key == null || key.isEmpty()) {
            cmd = refresh == 0 ? new String[] { "/usr/local/bin/node", PATH + "fetchSync.js",
                    bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(), root, mode }
                    : new String[] { "/usr/local/bin/node", PATH + "fetchAsync.js",
                            bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(), root,
                            mode };
        } else {
            cmd = refresh == 0
                    ? new String[] { "/usr/local/bin/node", PATH + "fetchSync.js",
                            bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(), root,
                            mode, key }
                    : new String[] { "/usr/local/bin/node", PATH + "fetchAsync.js",
                            bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(), root,
                            mode, key };
        }

        try {
            Process process = Runtime.getRuntime().exec(cmd);
            String result = IOUtils.toString(process.getInputStream(), "UTF-8");
            if (result != null && !result.isEmpty()) {
                json = (JsonObject) parser.parse(result);
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.debug("Exception happened: {}", e);
        }
        if (json.toString().equals("{}")) { // no new data fetched yet, empty json
            return oldResult;
        } else {
            oldResult = json.toString();
            return json.toString();

        }
    }

    public void startHandshake(JsonObject handshakePacket) {
        JsonParser parser = new JsonParser();
        String[] cmd = new String[] { "/usr/local/bin/node", PATH + "handshake.js",
                bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(),
                handshakePacket.toString() };
        try {
            logger.debug(
                    "Doing proof of work to attach data to the Tangle.... Handshake protocol for auto-compensation mechanism");
            process = Runtime.getRuntime().exec(cmd);
            String result = IOUtils.toString(process.getInputStream(), "UTF-8");
            if (result != null && !result.isEmpty()) {
                JsonObject json = (JsonObject) parser.parse(result);
                start = json.getAsJsonPrimitive("START").getAsInt();
                seed = json.getAsJsonPrimitive("SEED").getAsString();
                logger.debug("Sent: {}", json);
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.debug("Exception happened: {}", e);
        }
    }

    public boolean checkAPI() {
        if (bridge.getNodeInfo() != null) {
            return true;
        } else {
            return false;
        }
    }

    public boolean checkSeed(String seed) {
        return InputValidator.isValidSeed(seed);
    }

    public boolean checkTransactionStatus(String walletAddress, IotaAPI bridge) {
        boolean ans = false;
        if (bridge != null) {
            try {
                // TODO: find wtf this throws an error in here but not in intellij
                FindTransactionResponse response = bridge.findTransactionsByAddresses(new String[] { walletAddress });
                boolean[] status = bridge.getLatestInclusion(response.getHashes()).getStates();
                /**
                 * Search for any true value in the states array. If one is found, transaction is approved.
                 */
                for (boolean b : status) {
                    if (b == true) {
                        ans = true;
                    }
                }
            } catch (NoNodeInfoException | IllegalAccessError e) {
                logger.debug("Exception happened: {}", e.toString());
            }
        } else {
            logger.warn("No node detected. Aborting");
        }
        // TODO: return ans
        return true;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public IotaAPI getBridge() {
        return bridge;
    }
}