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
package org.eclipse.smarthome.binding.iota;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link IotaBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Theo Giovanna - Initial contribution
 */
@NonNullByDefault
public class IotaBindingConstants {

    private static final String BINDING_ID = "iota";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_IOTA = new ThingTypeUID(BINDING_ID, "topic");
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");

    // Bridge config properties
    public static final String PROTOCOL = "protocol";
    public static final String HOST = "host";
    public static final String PORT = "port";

    // Channels
    public static final String TEXT_CHANNEL = "text";
    public static final String NUMBER_CHANNEL = "number";
    public static final String PERCENTAGE_CHANNEL = "percentage";
    public static final String ONOFF_CHANNEL = "onoff";

}
