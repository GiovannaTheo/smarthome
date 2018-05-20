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
import org.eclipse.smarthome.io.iota.internal.metadata.IotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for changes to the item registry.
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaItemRegistryListener implements ItemRegistryChangeListener {

    private ItemRegistry itemRegistry;
    private IotaService service;
    private final Logger logger = LoggerFactory.getLogger(IotaItemRegistryListener.class);

    public IotaItemRegistryListener() {

    }

    public synchronized void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        this.itemRegistry.addRegistryChangeListener(this);
    }

    public ItemRegistry getItemRegistry() {
        return this.itemRegistry;
    }

    public void setService(IotaService service) {
        this.service = service;
    }

    @Override
    public void added(Item element) {
        this.service.getMetadataRegistry().getAll().forEach(metadata -> {
            if (metadata.getUID().getItemName().equals(element.getName())) {
                this.service.added(metadata);
            }
        });
    }

    @Override
    public void removed(Item element) {
        ((GenericItem) element).removeStateChangeListener(service.getStateListener());
        this.service.getMetadataRegistry().getAll().forEach(metadata -> {
            if (metadata.getUID().getItemName().equals(element.getName())) {
                this.service.getMetadataRegistry().remove(metadata.getUID());
                logger.debug("----------------------- ITEM, METADATA, STATE LISTENER REMOVED ---------------");
            }
        });
    }

    @Override
    public void updated(Item oldElement, Item element) {
        // not needed
    }

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        // not needed
    }

}
