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

import org.apache.commons.collections.CollectionUtils;
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
            if (CollectionUtils.isNotEmpty(itemListener.getItemRegistry().getAll())) {
                item = itemListener.getItemRegistry().getItem(element.getUID().getItemName());
                if (item instanceof GenericItem) {
                    if (element.getValue().equals("yes")) {
                        logger.debug("Iota state listener added for item: {}", item.getName());
                        ((GenericItem) item).addStateChangeListener(stateListener);
                        // publish the new item's state
                        stateListener.stateUpdated(item, item.getState());
                    }
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
        logger.debug("Iota metadata updated from {} to {}", oldElement.getValue(), element.getValue());
        if (element.getValue().equals("yes")) {
            added(element);
        } else {
            try {
                Item item = itemListener.getItemRegistry().getItem(oldElement.getUID().getItemName());
                itemListener.removed(item);
            } catch (ItemNotFoundException e) {
                e.printStackTrace();
            }
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
        if (metadataRegistry != null) {
            metadataRegistry.getAll().forEach(metadata -> removed(metadata));
            metadataRegistry.removeRegistryChangeListener(this);
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
