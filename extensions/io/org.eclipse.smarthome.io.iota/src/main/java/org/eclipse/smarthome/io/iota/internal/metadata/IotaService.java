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
package org.eclipse.smarthome.io.iota.internal.metadata;

import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.Metadata;
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.eclipse.smarthome.io.iota.internal.IotaItemRegistryListener;
import org.eclipse.smarthome.io.iota.internal.IotaItemStateChangeListener;
import org.eclipse.smarthome.io.iota.internal.IotaSettings;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;

/**
 * Listens for changes to the metadata registry.
 * This class will allow items to be listened to through the IotaItemStateChangeListener class if they
 * contain the metadata IOTA, when created.
 *
 * @author Theo Giovanna - Initial Contribution
 */
@Component(immediate = true)
public class IotaService implements RegistryChangeListener<Metadata> {

    // TODO: clean metadata when channels are unlinked
    // TODO: find a way to add the listener at startup (need to wait for this.itemRegistry to return items

    private MetadataRegistry metadataRegistry;
    private final Logger logger = LoggerFactory.getLogger(IotaService.class);
    private IotaItemRegistryListener itemListener;
    private IotaSettings settings;
    private final IotaItemStateChangeListener stateListener = new IotaItemStateChangeListener();

    /**
     *
     * ItemRegistryChangeListener impl
     *
     */
    @Override
    public void added(Metadata element) {
        /**
         * Adds a state listener to the item if it contains the right metadata
         */
        Item item;
        try {
            item = this.itemListener.getItemRegistry().getItem(element.getUID().getItemName());
            if (item instanceof GenericItem) {
                if (element.getValue().equals("yes")) {
                    logger.debug("----------------------- IOTA STATE LISTENER ADDED FOR THIS ITEM: {}", item.getName());
                    ((GenericItem) item).addStateChangeListener(stateListener);
                }
            }
        } catch (ItemNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removed(Metadata element) {
        // not needed
    }

    @Override
    public void updated(Metadata oldElement, Metadata element) {
        logger.debug("----------------------- IOTA METADATA UPDATED FROM {} TO {}", oldElement.getValue(),
                element.getValue());
        if (element.getValue().equals("yes")) {
            added(element);
        }
    }

    /**
     *
     * MetadataRegistry impl
     *
     */

    public void setMetadataRegistry(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
        this.metadataRegistry.addRegistryChangeListener(this);
    }

    /**
     *
     * IOTA Services
     *
     */
    public void stop() {
        if (this.metadataRegistry != null) {
            this.metadataRegistry.getAll().forEach(metadata -> removed(metadata));
            this.metadataRegistry.removeRegistryChangeListener(this);
        }
    }

    public IotaSettings getSettings() {
        return settings;
    }

    public IotaItemStateChangeListener getStateListener() {
        return stateListener;
    }

    public MetadataRegistry getMetadataRegistry() {
        return metadataRegistry;
    }

    public void setSettings(IotaSettings settings) {
        this.settings = settings;
    }

    public void setItemListener(IotaItemRegistryListener itemListener) {
        this.itemListener = itemListener;
    }

    public void setBridge(IotaAPI bridge) {
        stateListener.setBridge(bridge);
    }

}
