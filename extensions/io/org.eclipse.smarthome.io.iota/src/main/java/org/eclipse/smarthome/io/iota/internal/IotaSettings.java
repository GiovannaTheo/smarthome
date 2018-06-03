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

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the configured and static settings for the IOTA addon
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaSettings {

    // Default node
    private int port = 443;
    private String protocol = "https";
    private String host = "nodes.testnet.iota.org";
    private final Logger logger = LoggerFactory.getLogger(IotaSettings.class);

    /**
     * Adapted from org.openhab.io.homekit.internal
     *
     * @author Andy Lintner
     */
    public void fill(IotaApiConfiguration config) throws UnknownHostException {
        logger.debug("---------------------------------- UPDATING SETTINGS FOR IOTA API");
        this.setHost(getOrDefault(config.getHost(), this.getHost()));
        this.setProtocol(getOrDefault(config.getProtocol(), this.getProtocol()));
        Object port = config.getPort();
        if (port instanceof Integer) {
            this.setPort((Integer) port);
        } else if (port instanceof String) {
            String portString = String.valueOf(config.getPort());
            if (portString != null) {
                this.setPort(Integer.parseInt(portString));
            }
        }
    }

    /**
     * Taken from org.openhab.io.homekit.internal
     *
     * @author Andy Lintner
     */
    private static String getOrDefault(Object value, String defaultValue) {
        return value != null ? (String) value : defaultValue;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

}
