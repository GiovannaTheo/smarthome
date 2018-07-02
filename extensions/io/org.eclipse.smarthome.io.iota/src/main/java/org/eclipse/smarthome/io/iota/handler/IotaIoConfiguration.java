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

/**
 * The {@link IotaIoConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaIoConfiguration {

    public static final String REFRESH_INTERVAL = "refresh";
    public static final String PROTOCOL = "protocol";
    public static final String HOST = "host";
    public static final String PORT = "port";

    public int refresh;
    public String protocol;
    public String host;
    public int port;
}
