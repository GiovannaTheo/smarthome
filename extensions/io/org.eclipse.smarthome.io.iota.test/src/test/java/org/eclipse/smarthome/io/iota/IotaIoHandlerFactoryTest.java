/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http:www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.iota;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.io.iota.handler.IotaIoThingHandler;
import org.eclipse.smarthome.io.iota.internal.IotaIoThingHandlerFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * The {@link IotaIoHandlerFactoryTest} provides test cases for {@link IotaIoThingHandlerFactory}. The tests provide
 * mocks for supporting entities using Mockito.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaIoHandlerFactoryTest {

    private IotaIoThingHandlerFactory factory;

    @Before
    public void setup() {
        factory = new IotaIoThingHandlerFactory();
    }

    @Test
    public void shouldReturnNullForUnknownThingTypeUID() {
        Thing thing = mock(Thing.class);
        when(thing.getThingTypeUID()).thenReturn(new ThingTypeUID("anyBinding:someThingType"));
        assertEquals(factory.createHandler(thing), null);
    }

    @Test
    public void shouldReturnIotaThingHandler() {
        Thing thing = mock(Thing.class);
        when(thing.getThingTypeUID()).thenReturn(IotaIoBindingConstants.THING_TYPE_IOTA_IO);
        assertThat(factory.createHandler(thing), is(instanceOf(IotaIoThingHandler.class)));
    }

}
