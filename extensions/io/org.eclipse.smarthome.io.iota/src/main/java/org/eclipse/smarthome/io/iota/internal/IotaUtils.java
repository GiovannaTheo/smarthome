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
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jota.IotaAPI;

/**
 * Provides utils methods to work with IOTA transactions
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaUtils {

    // TODO: cleanup code + retrieve states

    private final Logger logger = LoggerFactory.getLogger(IotaUtils.class);
    private static final String PATH = "../../extensions/io/org.eclipse.smarthome.io.iota/src/main/java/org/eclipse/smarthome/io/iota/mam.client.js/example/";
    private int start = 0;
    private String seed = "";
    private String root = "";

    public IotaUtils() {

    }

    /**
     * Attach an item state on the Tangle, through MAM
     *
     * @param bridge the IOTA API endpoint
     * @param item the item for which we want to publish data
     * @param state the item's state
     */
    protected synchronized void publishState(@NonNull IotaAPI bridge, JsonElement jsonElement) {

        // Format the states in a json understandable by the official mam explorer
        // https://mam-explorer.firebaseapp.com/
        String publish = "{ \n" + jsonElement.toString() + " \n }";

        JsonParser parser = new JsonParser();

        try {
            logger.debug("Doing proof of work to attach data to the Tangle....");
            String[] cmd = this.start == 0 ? new String[] { "/usr/local/bin/node", PATH + "publishPublic.js",
                    bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(), publish }
                    : new String[] { "/usr/local/bin/node", PATH + "publishPublic.js",
                            bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(),
                            publish, this.seed, String.valueOf(this.start) };
            Process p = Runtime.getRuntime().exec(cmd);
            String result = IOUtils.toString(p.getInputStream(), "UTF-8");
            if (result != null && !result.isEmpty()) {
                JsonObject json = (JsonObject) parser.parse(result);
                this.start = json.getAsJsonPrimitive("START").getAsInt();
                this.seed = json.getAsJsonPrimitive("SEED").getAsString();
                this.root = json.getAsJsonPrimitive("ROOT").getAsString();
                logger.debug("Sent: {}", json);
            }
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve an item state on the Tangle, through MAM
     *
     * @param bridge the IOTA API endpoint
     */
    protected synchronized JsonObject getItemState(@NonNull IotaAPI bridge) {
        JsonParser parser = new JsonParser();
        JsonObject json = new JsonObject();
        String[] cmd = new String[] { "/usr/local/bin/node", PATH + "fetchAsync.js",
                bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(), this.root };
        Process p;
        try {
            p = Runtime.getRuntime().exec(cmd);
            String result = IOUtils.toString(p.getInputStream(), "UTF-8");
            if (result != null && !result.isEmpty()) {
                json = (JsonObject) parser.parse(result);
                logger.debug("Retrieved: {}", json);
            }
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return json;
    }
}
