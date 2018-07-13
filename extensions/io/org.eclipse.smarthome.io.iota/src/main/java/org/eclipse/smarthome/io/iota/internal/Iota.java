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
import org.eclipse.smarthome.io.iota.metadata.IotaService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;

/**
 * Set up the IOTA API and the metadata listener
 *
 * @author Theo Giovanna - Initial Contribution
 */
@Component(service = Iota.class, configurationPid = "org.eclipse.smarthome.iota", immediate = true)
public class Iota {

    private final Logger logger = LoggerFactory.getLogger(Iota.class);
    private final IotaService service = new IotaService();
    private final IotaSettings settings = new IotaSettings();
    private final IotaItemRegistryListener itemListener = new IotaItemRegistryListener();

    @Reference
    public void setItemRegistry(ItemRegistry itemRegistry) {
        itemListener.setItemRegistry(itemRegistry);
        itemListener.setService(service);
        service.setItemListener(itemListener);
        service.setSettings(settings);
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        itemListener.setItemRegistry(null);
    }

    @Reference
    protected void setMetadataRegistry(MetadataRegistry metadataRegistry) {
        service.setMetadataRegistry(metadataRegistry);
    }

    protected void unsetMetadataRegistry(MetadataRegistry metadataRegistry) {
        service.setMetadataRegistry(null);
    }

    /*
     * This function is called every time that the user updates the API parameters in Paper UI.
     * The Iota API instance is then updated accordingly
     */
    @Activate
    protected synchronized void activate(Map<String, Object> data) {
        modified(new Configuration(data).as(IotaApiConfiguration.class));
    }

    protected synchronized void modified(IotaApiConfiguration config) {
        try {
            // Updates the API config
            settings.fill(config);
            service.setSettings(settings);
            service.setBridge(new IotaAPI.Builder().protocol(settings.getProtocol()).host(settings.getHost())
                    .port(String.valueOf(settings.getPort())).build());
        } catch (Exception e) {
            logger.debug("Could not initialize IOTA API: {}", e.getMessage());
            return;
        }
    }

    @Deactivate
    protected void deactivate() {
        service.setBridge(null);
        service.stop();
    }
}
