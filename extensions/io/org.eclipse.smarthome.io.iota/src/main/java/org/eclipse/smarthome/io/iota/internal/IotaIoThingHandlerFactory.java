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
import org.eclipse.smarthome.io.iota.metadata.IotaMetadataRegistryChangeListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;

/**
 * The {@link IotaIoThingHandlerFactory} sets up the IOTA API, the metadata and item registry change listener
 *
 * @author Theo Giovanna - Initial Contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "org.eclipse.smarthome.iota", immediate = true)
public class IotaIoThingHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(IotaIoThingHandlerFactory.class);
    private final IotaMetadataRegistryChangeListener metadataRegistryChangeListener = new IotaMetadataRegistryChangeListener();
    private final IotaSettings settings = new IotaSettings();
    private final IotaItemRegistryChangeListener itemRegistryChangeListener = new IotaItemRegistryChangeListener();
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .singleton(IotaIoBindingConstants.THING_TYPE_IOTA_IO);

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

    @Activate
    public synchronized void activate(ComponentContext componentContext, Map<String, Object> data) {
        super.activate(componentContext);
        modified(new Configuration(data).as(IotaApiConfiguration.class));
    }

    public synchronized void modified(IotaApiConfiguration config) {
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

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    public @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (IotaIoBindingConstants.THING_TYPE_IOTA_IO.equals(thingTypeUID)) {
            return new IotaIoThingHandler(thing);
        }
        return null;
    }

    /**
     * Used for OSGi tests
     */
    public IotaMetadataRegistryChangeListener getMetadataChangeListener() {
        return metadataRegistryChangeListener;
    }
}
