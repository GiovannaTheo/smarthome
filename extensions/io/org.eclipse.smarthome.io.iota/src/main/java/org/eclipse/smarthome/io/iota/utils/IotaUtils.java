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
package org.eclipse.smarthome.io.iota.utils;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jota.IotaAPI;
import jota.utils.InputValidator;

/**
 * The {@link IotaUtils} provides utils methods to work with IOTA transactions.
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaUtils {

    private final Logger logger = LoggerFactory.getLogger(IotaUtils.class);
    private static final String PATH = "../../extensions/io/org.eclipse.smarthome.io.iota/lib/mam/example/";
    private String seed = null;
    private int start = 0;
    private IotaAPI bridge;
    private Process process;
    private String oldResult = "";
    private final String npmPath = isWindows() ? "npm.cmd" : "/usr/local/bin/node";

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
     * Attach data to the Tangle, through MAM
     *
     * @param states the json struct
     * @param mode   the MAM mode
     * @param key    the key if using restricted mode
     */
    public void publishState(JsonElement states, String mode, String key) {

        String payload = states.toString();
        String[] param = null;
        JsonParser parser = new JsonParser();

        switch (mode) {
            case "public":
            case "private":
                param = start == -1 ? new String[] { npmPath, PATH + "publish.js",
                        bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort(), payload, mode, seed }
                        : new String[] { npmPath, PATH + "publish.js",
                                bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort(), payload, mode,
                                seed, String.valueOf(start) };
                break;
            case "restricted":
                if (key != null && !key.isEmpty()) {
                    param = start == -1
                            ? new String[] { npmPath, PATH + "publish.js",
                                    bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort(), payload,
                                    mode, key, seed }
                            : new String[] { npmPath, PATH + "publish.js",
                                    bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort(), payload,
                                    mode, key, seed, String.valueOf(start) };
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
     * Retrieve a message from the Tangle, through MAM
     *
     * @param refresh the refresh interval
     * @param root    the root address on which to listen to
     * @param mode    the mode (public, private, restricted) for MAM
     * @param key     the key if restricted mode is used
     * @return the fetched message
     */
    public String fetchFromTangle(int refresh, String root, String mode, String key) {

        String[] cmd;
        JsonParser parser = new JsonParser();
        JsonObject json = new JsonObject();

        if (key == null || key.isEmpty()) {
            cmd = refresh == 0
                    ? new String[] { npmPath, PATH + "fetchSync.js",
                            bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort(), root, mode }
                    : new String[] { npmPath, PATH + "fetchAsync.js",
                            bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort(), root, mode };
        } else {
            cmd = refresh == 0
                    ? new String[] { npmPath, PATH + "fetchSync.js",
                            bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort(), root, mode, key }
                    : new String[] { npmPath, PATH + "fetchAsync.js",
                            bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort(), root, mode, key };
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

    /**
     * Check the validity of the Iota API node
     *
     * @return true if connexion is successful
     */
    public boolean checkAPI() {
        return bridge.getNodeInfo() != null;
    }

    /**
     * Check the validity of a seed
     *
     * @param seed the seed to validate
     * @return true if the seed is valid
     */
    public boolean checkSeed(String seed) {
        return InputValidator.isValidSeed(seed);
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
