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
package org.eclipse.smarthome.binding.iota.handler;

import java.math.BigDecimal;

import org.eclipse.smarthome.binding.iota.internal.AbstractIotaThingValue;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ChannelConfig} contains parsed channel configuration and runtime fields like the channel value.
 *
 * @author David Graeff - Initial contribution
 */
public class ChannelConfig {

    private final Logger logger = LoggerFactory.getLogger(ChannelConfig.class);

    String stateTopic;
    String transformationPattern;
    BigDecimal min = BigDecimal.valueOf(0);
    BigDecimal max = BigDecimal.valueOf(100);
    BigDecimal step = BigDecimal.valueOf(1);
    Boolean isFloat = false;
    Boolean inverse = false;
    String on;
    String off;
    String transformationServiceName;

    AbstractIotaThingValue value;
    ChannelUID channelUID;
    TransformationServiceProvider transformationServiceProvider;
    ChannelStateUpdateListener channelStateUpdateListener = null;

    public ChannelConfig() {

    }

    public void processMessage(String payload) {
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

}