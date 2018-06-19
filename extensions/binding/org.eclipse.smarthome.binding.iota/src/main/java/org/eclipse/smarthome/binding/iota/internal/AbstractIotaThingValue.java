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
package org.eclipse.smarthome.binding.iota.internal;

import org.eclipse.smarthome.binding.iota.handler.IotaThingHandler;
import org.eclipse.smarthome.core.types.State;

/**
 * Represents the value of the {@link IotaThingHandler}.
 *
 * @author Theo Giovanna - Initial contribution
 */
public interface AbstractIotaThingValue {
    /**
     * Returns the current value state.
     */
    public State getValue();

    /**
     * Updates the internal value with the received Iota value.
     *
     * @param updatedValue
     */
    public State update(String updatedValue) throws IllegalArgumentException;
}
