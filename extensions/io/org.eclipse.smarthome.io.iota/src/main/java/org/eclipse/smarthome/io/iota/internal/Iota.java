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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.iota.IotaIoBindingConstants;
import org.eclipse.smarthome.io.iota.handler.IotaIoThingHandler;
import org.eclipse.smarthome.io.iota.metadata.IotaService;
import org.osgi.service.component.ComponentContext;
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
@Component(service = ThingHandlerFactory.class, name = "Iota", configurationPid = "org.eclipse.smarthome.iota", immediate = true)
public class Iota extends BaseThingHandlerFactory {

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
    protected synchronized void activate(ComponentContext componentContext, Map<String, Object> data) {
        super.activate(componentContext);
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

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .singleton(IotaIoBindingConstants.THING_TYPE_IOTA_IO);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (IotaIoBindingConstants.THING_TYPE_IOTA_IO.equals(thingTypeUID)) {
            return new IotaIoThingHandler(thing, service.getStateListener());
        }

        return null;
    }
}
