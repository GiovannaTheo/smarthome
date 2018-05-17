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
import org.eclipse.smarthome.io.iota.Iota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;

/**
 * Provides access to ESH items via the IOTA API
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaImpl implements Iota {

    private final Logger logger = LoggerFactory.getLogger(IotaImpl.class);
    private final IotaChangeListener changeListener = new IotaChangeListener();
    private final IotaSettings settings = new IotaSettings();

    @Override
    public void hello() {
        logger.debug("Hello, World!");
    }

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

    // TODO: deactivate function

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
        // Updates the bridge
        changeListener.setBridge(new IotaAPI.Builder().protocol(settings.getProtocol()).host(settings.getHost())
                .port(String.valueOf(settings.getPort())).build());
    }
}
