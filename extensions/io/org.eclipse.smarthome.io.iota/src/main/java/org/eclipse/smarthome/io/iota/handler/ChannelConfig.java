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
package org.eclipse.smarthome.io.iota.handler;

import java.math.BigDecimal;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.io.iota.internal.AbstractIotaIoThingValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains parsed channel configuration and runtime fields like the channel value.
 *
 * @author David Graeff - Initial contribution
 */
public class ChannelConfig {
    private final Logger logger = LoggerFactory.getLogger(ChannelConfig.class);

    String address;
    BigDecimal step = BigDecimal.valueOf(1);

    AbstractIotaIoThingValue value;
    ChannelStateUpdateListener channelStateUpdateListener = null;

    ChannelUID channelUID;

    public ChannelConfig() {

    }

    void processMessage(String payload) {
        try {
            if (channelStateUpdateListener != null) {
                channelStateUpdateListener.channelStateUpdated(channelUID, value.update(payload));
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Incoming payload '{}' not supported", value);
        }
    }

    void dispose() {
        channelStateUpdateListener = null;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setChannelUID(ChannelUID channelUID) {
        this.channelUID = channelUID;
    }

    public void setChannelStateUpdateListener(ChannelStateUpdateListener channelStateUpdateListener) {
        this.channelStateUpdateListener = channelStateUpdateListener;
    }

    public AbstractIotaIoThingValue getValue() {
        return value;
    }

    public void setValue(AbstractIotaIoThingValue value) {
        this.value = value;
    }

}
