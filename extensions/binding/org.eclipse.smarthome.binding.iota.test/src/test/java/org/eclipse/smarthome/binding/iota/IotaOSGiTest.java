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

import org.eclipse.smarthome.test.java.JavaOSGiTest;

/**
 * Test cases for {@link IotaHandler}.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaOSGiTest extends JavaOSGiTest {

//    private static final ThingTypeUID BRIDGE_THING_TYPE_UID = new ThingTypeUID("iota", "bridge");
//
//    private ManagedThingProvider managedThingProvider;
//    private final VolatileStorageService volatileStorageService = new VolatileStorageService();
//    private Bridge bridge;
//
//    @Before
//    public void setUp() {
//        registerService(volatileStorageService);
//        managedThingProvider = getService(ThingProvider.class, ManagedThingProvider.class);
//        bridge = BridgeBuilder.create(BRIDGE_THING_TYPE_UID, "1").withLabel("My Bridge").build();
//    }
//
//    @After
//    public void tearDown() {
//        managedThingProvider.remove(bridge.getUID());
//        unregisterService(volatileStorageService);
//    }
//
//    @Test
//    public void creationOfIotaHandler() {
//        assertThat(bridge.getHandler(), is(nullValue()));
//        managedThingProvider.add(bridge);
//        waitForAssert(() -> assertThat(bridge.getHandler(), is(notNullValue())));
//    }
}
