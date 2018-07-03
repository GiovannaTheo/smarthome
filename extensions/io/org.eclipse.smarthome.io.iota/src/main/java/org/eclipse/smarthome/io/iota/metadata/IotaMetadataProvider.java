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
package org.eclipse.smarthome.io.iota.metadata;

import static java.util.stream.Collectors.toList;
import static org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder.create;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
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
                new ParameterOption("yes", "Share the item's state on the Tangle"), //
                new ParameterOption("no", "Don't share the item's state") //
        ).collect(toList());
    }

    @Override
    public @Nullable List<ConfigDescriptionParameter> getParameters(String value, @Nullable Locale locale) {
        switch (value) {
            case "yes":
                return Stream.of( //
                        create("mode", Type.TEXT).withLabel("Mode").withLimitToOptions(true).withOptions( //
                                Stream.of( //
                                        new ParameterOption("public", "Public"), //
                                        new ParameterOption("private", "Private"), //
                                        new ParameterOption("restricted", "Restricted") //
                                ).collect(toList())).build(), //
                        create("key", Type.TEXT).withLabel("Private Key").withDescription(
                                "Leave blank for non-restricted mode, otherwise enter the private key you want to use. If using the auto-compensation mechanism, the password will be overwritten by the one chosen by the client")
                                .build(), //
                        create("seed", Type.TEXT).withLabel("Existing Seed Address").withDescription(
                                "Leave blank to publish on a new root. Insert an existing root address to publish on an existing stream")
                                .build(), //
                        create("price", Type.DECIMAL).withLabel("Price in Miota for this stream").withDescription(
                                "If you want this stream to be accessible only in exchange of IOTA's, indicate its price here. Use it with Restricted mode only")
                                .withDefault("0.0").build(), //
                        create("wallet", Type.TEXT).withLabel("Wallet address on which to receive payment").build())
                        .collect(toList());
        }
        return null;
    }

}