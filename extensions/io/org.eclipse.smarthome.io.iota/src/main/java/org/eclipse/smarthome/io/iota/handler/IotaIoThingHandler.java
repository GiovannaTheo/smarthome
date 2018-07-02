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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.iota.IotaIoBindingConstants;
import org.eclipse.smarthome.io.iota.internal.NumberValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;
import jota.dto.response.GetBalancesResponse;

/**
 * The {@link IotaIoThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaIoThingHandler extends BaseThingHandler implements ChannelStateUpdateListener {

    // TODO: add tests
    // TODO: start MAM stream if change in balance is detected

    private final Logger logger = LoggerFactory.getLogger(IotaIoThingHandler.class);

    private final Map<ChannelUID, ChannelConfig> channelDataByChannelUID = new HashMap<>();
    private final Map<ChannelUID, Double> channelUIDtoBalanceMap = new HashMap<>();
    private int refresh = 0;
    private int port = 443;
    private String protocol = "https";
    private String host = "nodes.iota.cafe";
    ScheduledFuture<?> refreshJob;

    public IotaIoThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        ChannelConfig data = channelDataByChannelUID.get(channelUID);

        if (data == null) {
            logger.warn("Channel {} not supported", channelUID.getId());
            return;
        }

        if (command instanceof RefreshType) {
            if (data.value.getValue() != null) {
                updateState(channelUID.getId(), data.value.getValue());
                return;
            }
        }

    }

    @Override
    public void initialize() {

        logger.debug("Initializing Iota Wallet Handler.");

        Configuration configuration = getThing().getConfiguration();

        setRefresh(getOrDefault(configuration.get(IotaIoConfiguration.REFRESH_INTERVAL), getRefresh()));
        setHost(getOrDefault(configuration.get(IotaIoConfiguration.HOST), getHost()));
        setProtocol(getOrDefault(configuration.get(IotaIoConfiguration.PROTOCOL), getProtocol()));
        setPort(getOrDefault(configuration.get(IotaIoConfiguration.PORT), getPort()));

        for (Channel channel : thing.getChannels()) {
            if (!channelDataByChannelUID.containsKey(channel.getUID())) {
                ChannelConfig config = channel.getConfiguration().as(ChannelConfig.class);
                config.channelUID = channel.getUID();
                config.channelStateUpdateListener = this;

                switch (channel.getChannelTypeUID().getId()) {
                    case IotaIoBindingConstants.CHANNEL_BALANCE:
                        config.value = new NumberValue(true, config.step);
                        break;
                    default:
                        throw new IllegalArgumentException("ThingTypeUID not recognised");
                }

                channelDataByChannelUID.put(channel.getUID(), config);
            }
        }

        updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
        if (this.getThing().getChannels().size() != 0) {
            startAutomaticRefresh();
        }
    }

    @Override
    public void dispose() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
        }
        for (ChannelConfig channelConfig : channelDataByChannelUID.values()) {
            channelConfig.dispose();
        }
        channelDataByChannelUID.clear();
    }

    /**
     *
     * Fetch balance from the Tangle
     */
    private synchronized void startAutomaticRefresh() {
        logger.debug("Balance will refresh every {} seconds", refresh);
        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                getBalance();
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
            }
        }, 0, refresh, TimeUnit.SECONDS);
    }

    /**
     * Fetch the balance of an IOTA address
     *
     * @param address
     * @param bridge
     */
    private void getBalance() {
        IotaAPI bridge = new IotaAPI.Builder().protocol(getProtocol()).host(getHost()).port(String.valueOf(getPort()))
                .build();
        double balance = 0.;
        for (Channel channel : thing.getChannels()) {
            ChannelConfig config = channelDataByChannelUID.get(channel.getUID());
            logger.debug("Refreshing balance for wallet address: {}", config.address);
            GetBalancesResponse balanceAPI = bridge.getBalances(100, Arrays.asList(new String[] { config.address }));
            if (balanceAPI != null) {
                balance = Double.parseDouble(balanceAPI.getBalances()[0]) / 1000000;
            }
            config.processMessage(String.valueOf(balance));
            if (channelUIDtoBalanceMap.get(channel.getUID()) != balance) {
                // Balance has updated, need to link it with MAM
            } else {
                // Balance is identical
            }
            channelUIDtoBalanceMap.put(channel.getUID(), balance);
        }

    }

    private static String getOrDefault(Object value, String defaultValue) {
        return value != null ? (String) value : defaultValue;
    }

    private static int getOrDefault(Object value, int defaultValue) {
        return value instanceof BigDecimal ? ((BigDecimal) value).intValue() : defaultValue;
    }

    public int getRefresh() {
        return refresh;
    }

    public void setRefresh(int refresh) {
        this.refresh = refresh;
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

    @Override
    public void channelStateUpdated(ChannelUID channelUID, State value) {
        updateState(channelUID.getId(), value);
    }
}
