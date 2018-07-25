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
package org.eclipse.smarthome.io.iota;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The IOTA IO bundle class defines common constants, which are
 * used across the whole binding.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaIoBindingConstants {

    private static final String BINDING_ID = "iotaio";

    // List all Thing Type UIDs, related to the IOTA IO Binding
    public static final ThingTypeUID THING_TYPE_IOTA_IO = new ThingTypeUID(BINDING_ID, "wallet");

    // List all channels
    public static final String CHANNEL_BALANCE = "balance";

}
