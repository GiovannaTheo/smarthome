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
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jota.IotaAPI;
import jota.utils.TrytesConverter;

/**
 * Provides utils methods to work with IOTA transactions
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaUtils {

    private final Logger logger = LoggerFactory.getLogger(IotaUtils.class);
    private final String[] testHash = new String[] {
            "KSLBYEYSBWSARILO9CYIBQYMXXLCZAK9PIUCPNAEUYGHELWEOQEECGPYBOAIIJBUUMJFJEBDGBMB99999" };
    private static final String PATH = "../../extensions/io/org.eclipse.smarthome.io.iota/src/main/java/org/eclipse/smarthome/io/iota/mam.client.js/example/";
    private int start = 0;
    private String seed = "";

    public IotaUtils() {

    }

    /**
     * Attach an item state on the Tangle, through MAM
     *
     * @param bridge the IOTA API endpoint
     * @param item the item for which we want to publish data
     * @param state the item's state
     */
    protected void publishState(@NonNull IotaAPI bridge, @NonNull Item item, State state) {

        // Adding some slash so we know where to seperate the item name from its state
        String tryteItemName = TrytesConverter.toTrytes(item.getName().toString() + "/////");
        String tryteItemState = TrytesConverter.toTrytes(state.toFullString());

        JsonParser parser = new JsonParser();

        try {
            if (this.start == 0) {
                logger.debug("Doing proof of work to attach data to the Tangle....");
                String[] command = { "/usr/local/bin/node", PATH + "publishPublic.js",
                        bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(),
                        tryteItemName, tryteItemState };
                Process p = Runtime.getRuntime().exec(command);
                String result = IOUtils.toString(p.getInputStream(), "UTF-8");
                if (result != null && !result.isEmpty()) {
                    JsonObject json = (JsonObject) parser.parse(result);
                    this.start = json.getAsJsonPrimitive("Start").getAsInt();
                    this.seed = json.getAsJsonPrimitive("Seed").getAsString();
                    logger.debug("{}", json);
                }
                p.waitFor();
            } else {
                logger.debug("Doing proof of work to attach data to the Tangle....");
                String[] command = { "/usr/local/bin/node", PATH + "publishPublic.js",
                        bridge.getProtocol() + "://" + bridge.getHost() + ":" + bridge.getPort().toString(),
                        tryteItemName, tryteItemState, this.seed, String.valueOf(this.start) };
                Process p = Runtime.getRuntime().exec(command);
                String result = IOUtils.toString(p.getInputStream(), "UTF-8");
                if (result != null && !result.isEmpty()) {
                    JsonObject json = (JsonObject) parser.parse(result);
                    this.start = json.getAsJsonPrimitive("Start").getAsInt();
                    this.seed = json.getAsJsonPrimitive("Seed").getAsString();
                    logger.debug("{}", json);
                }
                p.waitFor();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
