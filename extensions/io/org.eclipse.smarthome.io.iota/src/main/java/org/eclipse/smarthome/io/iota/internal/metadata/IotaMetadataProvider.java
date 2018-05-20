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
package org.eclipse.smarthome.io.iota.internal.metadata;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.config.core.metadata.MetadataConfigDescriptionProvider;
import org.osgi.service.component.annotations.Component;

/**
 * Describes the metadata for the "iota" namespace.
 *
 * @author Theo Giovanna - initial contribution
 *
 */
@Component
@NonNullByDefault
public class IotaMetadataProvider implements MetadataConfigDescriptionProvider {

    @Override
    public String getNamespace() {
        return "iota";
    }

    @Override
    public @Nullable String getDescription(@Nullable Locale locale) {
        return "Share item state to the iota Tangle";
    }

    @Override
    public @Nullable List<ParameterOption> getParameterOptions(@Nullable Locale locale) {
        return Stream.of( //
                new ParameterOption("yes", "Share the item's state with the Tangle"),
                new ParameterOption("no", "Don't share the item's state with the Tangle")).collect(toList());
    }

    @Override
    public @Nullable List<ConfigDescriptionParameter> getParameters(String value, @Nullable Locale locale) {
        return null;
    }

}