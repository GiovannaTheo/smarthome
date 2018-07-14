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

import static org.mockito.MockitoAnnotations.initMocks;

import org.eclipse.smarthome.io.iota.utils.IotaUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Tests cases for {@link IotaIoHandle}. The tests provide mocks for supporting entities using Mockito.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaUtilsTest {

    @Mock
    private IotaUtils utils;

    @Before
    public void setUp() {
        initMocks(this);
        utils = new IotaUtils("https", "nodes.testnet.iota.org", 443);
    }

    @Test
    public void apiShouldConnect() {
        // assertThat(utils.checkAPI(), is(equalTo(true)));
    }

//    @Test
//    public void stateShouldPublish() {
//        JsonObject state = new JsonObject();
//        state.addProperty("TEST", true);
//        assertThat(utils.publishState(state, "public", ""), is(equalTo(true)));
//    }

}
