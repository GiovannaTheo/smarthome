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

import java.net.UnknownHostException;
import java.util.Map;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;

/**
 * Set up the IOTA API and the registry listener
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class Iota {

    // TODO Deactivate function
    // TODO Clear the previous listener when the IOTA API config changes

    private final Logger logger = LoggerFactory.getLogger(Iota.class);
    private final IotaRegistryChangeListener changeListener = new IotaRegistryChangeListener();
    private final IotaSettings settings = new IotaSettings();

    public void setItemRegistry(ItemRegistry itemRegistry) {
        changeListener.setSettings(settings);
        changeListener.setItemRegistry(itemRegistry);
    }

    /*
     * This function is called every time that the user updates the API parameters in Paper UI.
     * The Iota API instance is then updated accordingly
     */
    protected synchronized void activate(Map<String, Object> data) {
        modified(new Configuration(data).as(IotaApiConfiguration.class));
    }

    protected synchronized void modified(IotaApiConfiguration config) {
        try {
            // Updates the API config
            settings.fill(config);
            // Updates the listener with the new settings
            changeListener.setSettings(settings);
        } catch (UnknownHostException e) {
            logger.debug("Could not initialize IOTA API: {}", e.getMessage(), e);
            return;
        }
        try {
            start();
        } catch (Exception e) {
            logger.error("Could not initialize IOTA API: {}", e.getMessage(), e);
        }
    }

    private void start() {
        // Set the bridge with the config of the Paper UI
        changeListener.setBridge(new IotaAPI.Builder().protocol(settings.getProtocol()).host(settings.getHost())
                .port(String.valueOf(settings.getPort())).build());
    }
}
