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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.StateChangeListener;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;

/**
 * Listens for changes to the state of registered items.
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaItemStateChangeListener implements StateChangeListener {

    // TODO Publish the state to the Tangle

    private final Logger logger = LoggerFactory.getLogger(IotaItemStateChangeListener.class);
    IotaAPI bridge;

    @Override
    public void stateChanged(@NonNull Item item, @NonNull State oldState, @NonNull State newState) {
        logger.debug("--------------------------- I am item {} and my state changed from {} to {}", item.getName(),
                oldState, newState);
        // TODO Auto-generated method stub

    }

    @Override
    public void stateUpdated(@NonNull Item item, @NonNull State state) {
        logger.debug("--------------------------- I am item {} and my state {} updated", item.getName(), state);
        // TODO Auto-generated method stub

    }

    public void setBridge(IotaAPI bridge) {
        this.bridge = bridge;
        GetNodeInfoResponse getNodeInfoResponse = bridge.getNodeInfo();
        logger.debug("IOTA CONNECTION SUCCESS: {}", getNodeInfoResponse);
    }

}
