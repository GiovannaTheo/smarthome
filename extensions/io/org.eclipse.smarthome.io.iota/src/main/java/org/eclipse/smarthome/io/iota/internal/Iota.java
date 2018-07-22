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

import java.util.Map;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.eclipse.smarthome.io.iota.metadata.IotaMetadataRegistryChangeListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;

/**
 * The {@link Iota} sets up the IOTA API, the metadata and item registry change listener
 *
 * @author Theo Giovanna - Initial Contribution
 */
@Component(service = Iota.class, configurationPid = "org.eclipse.smarthome.iota", immediate = true)
public class Iota {

    private final Logger logger = LoggerFactory.getLogger(Iota.class);
    private final IotaMetadataRegistryChangeListener metadataRegistryChangeListener = new IotaMetadataRegistryChangeListener();
    private final IotaSettings settings = new IotaSettings();
    private final IotaItemRegistryChangeListener itemRegistryChangeListener = new IotaItemRegistryChangeListener();

    @Reference
    public void setItemRegistry(ItemRegistry itemRegistry) {
        itemRegistryChangeListener.setItemRegistry(itemRegistry);
        itemRegistryChangeListener.setMetadataRegistryChangeListener(metadataRegistryChangeListener);
        metadataRegistryChangeListener.setItemRegistryChangeListener(itemRegistryChangeListener);
        metadataRegistryChangeListener.setSettings(settings);
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        itemRegistryChangeListener.setItemRegistry(null);
    }

    @Reference
    public void setMetadataRegistry(MetadataRegistry metadataRegistry) {
        metadataRegistryChangeListener.setMetadataRegistry(metadataRegistry);
    }

    public void unsetMetadataRegistry(MetadataRegistry metadataRegistry) {
        metadataRegistryChangeListener.setMetadataRegistry(null);
    }

    /*
     * This function is called every time that the user updates the API parameters in Paper UI.
     * The Iota API instance is then updated accordingly
     */
    @Activate
    public synchronized void activate(Map<String, Object> data) {
        modified(new Configuration(data).as(IotaApiConfiguration.class));
    }

    protected synchronized void modified(IotaApiConfiguration config) {
        try {
            // Updates the API config
            settings.fill(config);
            metadataRegistryChangeListener.setSettings(settings);
            metadataRegistryChangeListener.setBridge(new IotaAPI.Builder().protocol(settings.getProtocol())
                    .host(settings.getHost()).port(String.valueOf(settings.getPort())).build());
        } catch (Exception e) {
            logger.debug("Could not initialize IOTA API: {}", e.getMessage());
        }
    }

    @Deactivate
    protected void deactivate() {
        metadataRegistryChangeListener.setBridge(null);
        metadataRegistryChangeListener.stop();
    }

    /**
     * Used for OSGi tests
     */
    public IotaMetadataRegistryChangeListener getMetadataChangeListener() {
        return metadataRegistryChangeListener;
    }
}
