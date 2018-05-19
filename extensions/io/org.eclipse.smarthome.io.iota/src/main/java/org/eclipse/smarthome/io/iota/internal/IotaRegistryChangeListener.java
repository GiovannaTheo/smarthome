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

import java.util.Collection;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.ItemRegistryChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;

/**
 * Listens for changes to the item registry.
 * This class will allow items to be listened to through the IotaItemStateChangeListener class if they
 * contain the metadata IOTA, when created.
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaRegistryChangeListener implements ItemRegistryChangeListener {

    // TODO listen to item updates only if it contains the metadata "iota" (or tag?)
    // TODO implement the removed method

    private ItemRegistry itemRegistry;
    private final Logger logger = LoggerFactory.getLogger(IotaRegistryChangeListener.class);
    private IotaSettings settings;
    private final IotaItemStateChangeListener stateListener = new IotaItemStateChangeListener();

    @Override
    public void added(Item element) {
        if (element instanceof GenericItem) {
            ((GenericItem) element).addStateChangeListener(stateListener);
            logger.debug("IOTA STATE LISTENER ADDED FOR ITEM {}", element.getName());
        }
    }

    @Override
    public void removed(Item element) {
        logger.debug("---------------------------------- item REMOVED: {}", element.getName());
    }

    @Override
    public void updated(Item oldElement, Item element) {
        // not needed
    }

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        // not needed
    }

    public synchronized void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        this.itemRegistry.addRegistryChangeListener(this);
        this.itemRegistry.getAll().forEach(item -> added(item));
    }

    public IotaSettings getSettings() {
        return settings;
    }

    public void setSettings(IotaSettings settings) {
        this.settings = settings;
    }

    /**
     * Set the IOTA API bridge to the instance of IotaItemStateChangeLister that listens to state updates.
     * This will allow the class IotaItemStateChangeListener to publish states to the Tangle for registered items.
     */
    public void setBridge(IotaAPI bridge) {
        stateListener.setBridge(bridge);
    }

}
