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

    // TODO Get the hash of the publicated state on the Tangle

    private final Logger logger = LoggerFactory.getLogger(IotaItemStateChangeListener.class);
    IotaAPI bridge;
    private final IotaUtils utils = new IotaUtils();

    @Override
    public void stateChanged(@NonNull Item item, @NonNull State oldState, @NonNull State newState) {
        logger.debug("I am item {} and my state changed from {} to {}", item.getName(), oldState, newState);
        // For testing only:
        // utils.publishState(this.bridge, item, newState, "", "");
    }

    @Override
    public void stateUpdated(@NonNull Item item, @NonNull State state) {
        // not needed: the state has been updated but is identical to the oldState, therefore it is not necessary to
        // re-upload the state to the Tangle
        logger.debug("------------------- thing: {}, item: {}, state: {}", item.getName(), item.getCategory(), state);
        // For testing only:
        // logger.debug("Decoded state: {}", utils.getStateFromTransaction(new String[] { "" }, this.bridge));
    }

    public void setBridge(IotaAPI bridge) {
        this.bridge = bridge;
        if (this.bridge != null) {
            GetNodeInfoResponse getNodeInfoResponse = bridge.getNodeInfo();
            logger.debug("IOTA CONNECTION SUCCESS: {}", getNodeInfoResponse);
        }
    }

}
