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
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.eclipse.smarthome.io.iota.internal.metadata.IotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;

/**
 * Set up the IOTA API and the metadata listener
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class Iota {

    private final Logger logger = LoggerFactory.getLogger(Iota.class);
    private final IotaService service = new IotaService();
    private final IotaSettings settings = new IotaSettings();
    private final IotaItemRegistryListener itemListener = new IotaItemRegistryListener();

    public void setItemRegistry(ItemRegistry itemRegistry) {
        itemListener.setItemRegistry(itemRegistry);
        itemListener.setService(service);
        service.setItemListener(itemListener);
        service.setSettings(settings);
    }

    protected void setMetadataRegistry(MetadataRegistry metadataRegistry) {
        service.setMetadataRegistry(metadataRegistry);
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
            service.setSettings(settings);
        } catch (UnknownHostException e) {
            logger.debug("Could not initialize IOTA API: {}", e.getMessage());
            return;
        }
        try {
            start();
        } catch (Exception e) {
            logger.error("Could not initialize IOTA API: {}", e.getMessage());
        }
    }

    protected void deactivate() {
        service.setBridge(null);
        service.stop();
    }

    private void start() {
        // Set the bridge with the config of the Paper UI
        service.setBridge(new IotaAPI.Builder().protocol(settings.getProtocol()).host(settings.getHost())
                .port(String.valueOf(settings.getPort())).build());
    }

}
