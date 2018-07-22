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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.binding.iota.IotaBindingConstants;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.iota.utils.IotaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link IotaBridgeHandler} is responsible for handling the
 * bridge over IOTA's distributed ledger, the Tangle.
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(IotaBridgeHandler.class);
    private int port = 443;
    private String protocol = "https";
    private String host = "nodes.testnet.iota.org";
    private IotaUtils utils;

    public IotaBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        // not needed
    }

    @Override
    public void initialize() {
        logger.debug("Initializing IOTA bridge");
        Configuration config = getThing().getConfiguration();
        setHost(getOrDefault(config.get(IotaBindingConstants.HOST), getHost()));
        setProtocol(getOrDefault(config.get(IotaBindingConstants.PROTOCOL), getProtocol()));
        setPort(getOrDefault(config.get(IotaBindingConstants.PORT), getPort()));
        setUtils(new IotaUtils(protocol, host, port));

        if (utils.checkAPI()) {
            logger.debug("IOTA API checked, connexion successfull: bridge can be set ONLINE");
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "Node unreachable");
        }
    }

    private static String getOrDefault(Object value, String defaultValue) {
        return value != null ? (String) value : defaultValue;
    }

    private static int getOrDefault(Object value, int defaultValue) {
        return value instanceof BigDecimal ? ((BigDecimal) value).intValue() : defaultValue;
    }

    protected int getPort() {
        return port;
    }

    protected void setPort(int port) {
        this.port = port;
    }

    protected String getProtocol() {
        return protocol;
    }

    protected void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    protected String getHost() {
        return host;
    }

    protected void setHost(String host) {
        this.host = host;
    }

    public IotaUtils getUtils() {
        return utils;
    }

    protected void setUtils(IotaUtils utils) {
        this.utils = utils;
    }
}
