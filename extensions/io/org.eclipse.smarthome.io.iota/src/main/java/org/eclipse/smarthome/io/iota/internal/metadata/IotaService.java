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
import org.eclipse.smarthome.io.iota.internal.Debouncer;
import org.eclipse.smarthome.io.iota.internal.IotaItemRegistryListener;
import org.eclipse.smarthome.io.iota.internal.IotaItemStateChangeListener;
import org.eclipse.smarthome.io.iota.internal.IotaSeedGenerator;
import org.eclipse.smarthome.io.iota.internal.IotaSettings;
import org.eclipse.smarthome.io.iota.internal.IotaUtils;
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
    private final IotaSeedGenerator seedGenerator = new IotaSeedGenerator();
    private IotaAPI bridge;

    @SuppressWarnings("unused")
    @Override
    public void added(Metadata element) {
        /**
         * Adds a state listener to the item if it contains the right metadata
         */
        Item item;
        try {
            stateListener.setService(this);
            if (CollectionUtils.isNotEmpty(itemListener.getItemRegistry().getAll())) {
                item = itemListener.getItemRegistry().getItem(element.getUID().getItemName());
                if (item instanceof GenericItem) {
                    if (element.getValue() != null) {
                        if (element.getValue().equals("yes")) {
                            if (!element.getConfiguration().isEmpty()) {

                                /**
                                 * Adds a new entry in the hashmap: maps the item UID to a specific seed on
                                 * which messages will be broadcasted. Either a new one is created, or an
                                 * existing one is used. If a new seed is used, a corresponding debouncing instance
                                 * and utils instance are created.
                                 */

                                String seed;
                                if (element.getConfiguration().get("seed") == null) {
                                    logger.debug("A new seed will be generated for item {}", item.getUID());
                                    seed = seedGenerator.getNewSeed();
                                    updateMaps(item, seed);
                                    element.getConfiguration().put("seed", seed);
                                } else {
                                    /**
                                     * Uses an existing seed to publish this item states
                                     */

                                    seed = element.getConfiguration().get("seed").toString();
                                    if (seed != null && !seed.isEmpty() && seed.length() == 81) {
                                        logger.debug("An existing seed will be used for item {}", item.getUID());
                                        stateListener.getUidToSeedMap().put(item.getUID(), seed.toString());
                                    } else {
                                        logger.debug("Invalid seed for item {}. Generating a new one", item.getUID());
                                        seed = seedGenerator.getNewSeed();
                                        updateMaps(item, seed);
                                        element.getConfiguration().put("seed", seed);
                                    }
                                }

                                /**
                                 * If restricted mode was selected, the private key is saved, otherwise generated.
                                 */

                                if (element.getConfiguration().get("mode").equals("restricted") && !seed.isEmpty()) {
                                    if (element.getConfiguration().get("key") != null) {
                                        logger.debug("An existing key will be used for item {}", item.getUID());
                                        String inputKey = element.getConfiguration().get("key").toString();
                                        if (inputKey != null && !inputKey.isEmpty()) {
                                            stateListener.getSeedToPrivateKeyMap().put(seed, inputKey);
                                        }
                                    } else {
                                        logger.debug("Invalid key for item {}. Generating a new one", item.getUID());
                                        String key = seedGenerator.getNewPrivateKey();
                                        stateListener.getSeedToPrivateKeyMap().put(seed, key);
                                        element.getConfiguration().put("key", key);
                                    }
                                }

                                logger.debug("Iota state listener added for item: {}", item.getName());
                                ((GenericItem) item).addStateChangeListener(stateListener);

                                /**
                                 * Publish the state
                                 */
                                stateListener.stateChanged(item, item.getState(), item.getState());
                            }
                        }
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
     * Updates the hashmaps in the {@link IotaStateListener} class.
     *
     * @param item
     * @param seed
     */
    public void updateMaps(Item item, String seed) {
        stateListener.getUidToSeedMap().put(item.getUID(), seed);
        stateListener.getSeedToDebouncerMap().put(seed, new Debouncer());
        stateListener.getSeedToUtilsMap().put(seed,
                new IotaUtils(bridge.getProtocol(), bridge.getHost(), Integer.parseInt(bridge.getPort()), seed, 0));
    }

    public void setMetadataRegistry(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
        this.metadataRegistry.addRegistryChangeListener(this);
    }

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
        this.bridge = bridge;
    }
}
