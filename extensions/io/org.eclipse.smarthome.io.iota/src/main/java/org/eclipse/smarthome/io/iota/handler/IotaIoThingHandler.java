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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
import org.eclipse.smarthome.io.iota.internal.IotaItemStateChangeListener;
import org.eclipse.smarthome.io.iota.internal.NumberValue;
import org.eclipse.smarthome.io.iota.security.RSAUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;
import jota.dto.response.GetBalancesResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;
import jota.utils.TrytesConverter;

/**
 * The {@link IotaIoThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaIoThingHandler extends BaseThingHandler implements ChannelStateUpdateListener {

    // TODO: add tests

    private final Logger logger = LoggerFactory.getLogger(IotaIoThingHandler.class);

    private final Map<ChannelUID, ChannelConfig> channelDataByChannelUID = new HashMap<>();
    private final Map<ChannelUID, Double> balanceByChannelUID = new HashMap<>();
    private int refresh = 0;
    private int port = 14700;
    private String protocol = "http";
    private String host = "localhost";
    ScheduledFuture<?> refreshJob;
    IotaItemStateChangeListener stateListener;

    public IotaIoThingHandler(Thing thing, IotaItemStateChangeListener stateListener) {
        super(thing);
        this.stateListener = stateListener;
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
            GetBalancesResponse balanceAPI = null;
            try {
                balanceAPI = bridge.getBalances(100, Arrays.asList(new String[] { config.address }));
            } catch (IllegalAccessError | ArgumentException e) {
                logger.debug("Error: invalid or empty wallet: {}", e.getMessage());
            }
            if (balanceAPI != null) {
                balance = Double.parseDouble(balanceAPI.getBalances()[0]) / 1000000;
                logger.debug("Balance detected: value is {} Miota", balance);
            }
            config.processMessage(String.valueOf(balance));

            if (balanceByChannelUID.containsKey(channel.getUID())) {
                if (balanceByChannelUID.get(channel.getUID()) != balance) {
                    List<Transaction> transactions = null;
                    try {
                        transactions = bridge.findTransactionObjectsByAddresses(new String[] { config.address });

                        for (Transaction t : transactions) {

                            /**
                             * In the transaction response, the last 9 char of the address are not shown, thus
                             * we need to substract them
                             */

                            String add = config.address.substring(0, config.address.length() - 9);

                            if (add.equals(t.getAddress())) {

                                if (t.getValue() != 0) {

                                    String seed = stateListener.getSeedByWallet(config.address);

                                    if (Math.abs(balance - balanceByChannelUID.get(channel.getUID())) == stateListener
                                            .getPaymentAmountByWallet(config.address)) {

                                        logger.debug("Payment received. Processing MAM stream...");

                                        /**
                                         * Converts the trytes RSA encoded password into bytes and decodes it, if
                                         * existing
                                         */

                                        String encryptedPasswordTrytes = t.getSignatureFragments();

                                        int j = 0;
                                        for (int i = encryptedPasswordTrytes.length() - 1; i > 0; i--) {
                                            if (encryptedPasswordTrytes.charAt(i) == '9') {
                                                j++;
                                            } else {
                                                break;
                                            }
                                        }
                                        // Removing padded 9's and converting to string
                                        String encryptedPassword = TrytesConverter.toString(encryptedPasswordTrytes
                                                .substring(0, encryptedPasswordTrytes.length() - j));

                                        if (encryptedPassword != null && !encryptedPassword.isEmpty()) {
                                            try {
                                                RSAUtils rsa = new RSAUtils();
                                                logger.debug("Decrypting: {}", encryptedPassword);
                                                String password = rsa.decrypt(encryptedPassword,
                                                        stateListener.getRsaKeysBySeed(seed, 2),
                                                        stateListener.getRsaKeysBySeed(seed, 3));
                                                logger.debug("Decrpyted password is: {}", password);
                                                // Updating password for the next push
                                                if (password != null && !password.isEmpty()) {
                                                    stateListener.addPrivateKeyBySeed(seed, password);
                                                }
                                            } catch (NoSuchAlgorithmException | InvalidKeyException
                                                    | NoSuchPaddingException | BadPaddingException
                                                    | IllegalBlockSizeException e) {
                                                logger.debug(
                                                        "Exception happened: {}. Password will remain the one chosen by the provider",
                                                        e.toString());
                                            }
                                        }

                                        stateListener.addPaymentReceivedBySeed(seed, true);

                                        /**
                                         * Releasing data
                                         */

                                        stateListener.getDebouncerBySeed(seed)
                                                .debounce(IotaItemStateChangeListener.class, new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        stateListener.getUtilsBySeed(seed).publishState(
                                                                stateListener.getJsonObjectBySeed(seed).get("Items"),
                                                                "restricted", stateListener.getPrivateKeyBySeed(seed));
                                                    }
                                                }, 5000, TimeUnit.MILLISECONDS);
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (ArgumentException e) {
                        logger.debug("Error: invalid or empty wallet: {}", e.getMessage());
                    }

                } else {
                    // Balance is identical
                }
            }
            // Updating new balance value
            balanceByChannelUID.put(channel.getUID(), balance);
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
