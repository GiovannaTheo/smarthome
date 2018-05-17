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

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.ItemRegistryChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;

/**
 * Listens for changes to the item registry.
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaChangeListener implements ItemRegistryChangeListener {

    private ItemRegistry itemRegistry;
    private final Logger logger = LoggerFactory.getLogger(IotaChangeListener.class);
    private IotaSettings settings;
    IotaAPI bridge;

    @Override
    public void added(Item element) {
        // TODO Auto-generated method stub
        logger.debug("---------------------------------- item ADDED: {}", element.getName());
    }

    @Override
    public void removed(Item element) {
        // TODO Auto-generated method stub
        logger.debug("---------------------------------- item REMOVED: {}", element.getName());
    }

    @Override
    public void updated(Item oldElement, Item element) {
        // TODO Auto-generated method stub
        logger.debug("---------------------------------- UPDATED: from {} to {}", oldElement.getName(),
                element.getName());
    }

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        // TODO Auto-generated method stub
        logger.debug("---------------------------------- ALLITEMS");
    }

    public synchronized void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        itemRegistry.addRegistryChangeListener(this);
        itemRegistry.getAll().forEach(item -> added(item));
    }

    public IotaSettings getSettings() {
        return settings;
    }

    public void setSettings(IotaSettings settings) {
        this.settings = settings;
    }

    public IotaAPI getBridge() {
        return bridge;
    }

    public void setBridge(IotaAPI bridge) {
        this.bridge = bridge;
        GetNodeInfoResponse getNodeInfoResponse = bridge.getNodeInfo();
        logger.debug("IOTA CONNECTION SUCCESS: {}", getNodeInfoResponse);
    }

}
