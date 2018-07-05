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
package org.eclipse.smarthome.binding.iota.handler;

import static org.eclipse.smarthome.binding.iota.handler.IotaConfiguration.REFRESH_INTERVAL;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.eclipse.smarthome.binding.iota.IotaBindingConstants;
import org.eclipse.smarthome.binding.iota.internal.TextValue;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.iota.internal.IotaUtils;
import org.eclipse.smarthome.io.iota.security.RSAUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jota.IotaAPI;
import jota.dto.response.SendTransferResponse;
import jota.model.Transfer;
import jota.utils.TrytesConverter;

/**
 * The {@link IotaPaymentThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaPaymentThingHandler extends BaseThingHandler implements ChannelStateUpdateListener {

    private final Logger logger = LoggerFactory.getLogger(IotaPaymentThingHandler.class);
    private final Map<ChannelUID, ChannelConfig> channelDataByChannelUID = new HashMap<>();
    private final Map<ChannelUID, String> walletByChannelUID = new HashMap<>();
    private final Map<ChannelUID, Boolean> paymentSentByChannelUID = new HashMap<>();
    private JsonObject data = new JsonObject();
    private int refresh;
    private String nextroot;
    private final String mode = "restricted";
    private IotaUtils utils;
    ScheduledFuture<?> refreshJob;
    private final ThingRegistry thingRegistry;

    public IotaPaymentThingHandler(Thing thing, ThingRegistry thingRegistry) {
        super(thing);
        this.thingRegistry = thingRegistry;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // not needed
    }

    @Override
    public void initialize() {

        logger.debug("Initializing Iota Payment handler.");

        Configuration configuration = getThing().getConfiguration();

        setRefresh(getOrDefault(configuration.get(REFRESH_INTERVAL), getRefresh()));

        Bridge bridge = this.getBridge();
        if (bridge != null) {
            IotaBridgeHandler bridgeHandler = (IotaBridgeHandler) bridge.getHandler();
            if (bridgeHandler != null) {
                logger.debug("Bridge initialized");
                this.utils = bridgeHandler.getUtils();

            } else {
                logger.debug("Could not initialize Iota Utils for thing {}", this.getThing().getUID());
            }
        } else {
            logger.debug("Could not initialize Iota Utils for thing {}", this.getThing().getUID());
        }

        for (Channel channel : thing.getChannels()) {
            if (!channelDataByChannelUID.containsKey(channel.getUID())) {
                ChannelConfig config = channel.getConfiguration().as(ChannelConfig.class);
                config.channelUID = channel.getUID();
                config.channelStateUpdateListener = this;

                switch (channel.getChannelTypeUID().getId()) {
                    case IotaBindingConstants.PAYMENT_CHANNEL:
                        config.value = new TextValue();
                        paymentSentByChannelUID.put(channel.getUID(), false);
                        startAutomaticRefresh(channel.getUID());
                        break;
                    default:
                        throw new IllegalArgumentException("ThingTypeUID not recognised");
                }

                channelDataByChannelUID.put(channel.getUID(), config);
            }
        }
        updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
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
     * listens on the tangle until data is retrieved
     */
    private synchronized void startAutomaticRefresh(ChannelUID channelUID) {
        logger.debug("Payment info will refresh every {} seconds", refresh);
        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (channelDataByChannelUID.containsKey(channelUID)) {
                    if (!paymentSentByChannelUID.get(channelUID)) {
                        boolean success = sendPaymentToWallet(channelUID);
                        if (success) {
                            /**
                             * Payment successfully sent
                             */
                            paymentSentByChannelUID.put(channelUID, true);
                        }
                    }

                    checkPaymentStatus(channelUID);
                }
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
            }
        }, 0, refresh, TimeUnit.SECONDS);
    }

    /**
     *
     * @return success if any data is found in the MAM transaction
     */
    private synchronized boolean sendPaymentToWallet(ChannelUID channelUID) {
        boolean success = false;
        ChannelConfig config = channelDataByChannelUID.get(channelUID);
        if (config.root != null && !config.root.isEmpty()) {
            JsonParser parser = new JsonParser();
            if (this.utils != null) {
                /**
                 * FOR TESTING ONLY WITH TESTNET
                 */
                IotaUtils mamUtils = new IotaUtils("https", "nodes.testnet.iota.org", 443);
                /**
                 * TODO: REMOVE
                 */
                JsonObject resp = null;
                try {
                    resp = parser.parse(mamUtils.fetchFromTangle(refresh, config.root, mode, config.key))
                            .getAsJsonObject();
                } catch (IllegalStateException e) {
                    logger.debug("Exception happened: {}", e.toString());
                }

                if (resp != null) {

                    nextroot = resp.get("NEXTROOT").getAsString();
                    data = resp.entrySet().iterator().next().getValue().getAsJsonObject();

                    if (data != null) {
                        double price = data.get("PRICE").getAsDouble();
                        if (price <= config.threshold) {

                            /**
                             * Price is smaller than threshold. Authorization granted for processing the payment
                             */

                            String wallet = data.get("WALLET").getAsString();
                            String encryptedPassword = "";

                            if (config.ownkey != null && !config.ownkey.isEmpty()) {
                                JsonObject rsaJson = data.get("RSA").getAsJsonObject();
                                try {
                                    RSAUtils rsa = new RSAUtils();
                                    encryptedPassword = rsa.encrypt(config.ownkey,
                                            new BigInteger(rsaJson.get("MODULUS").getAsString()),
                                            new BigInteger(rsaJson.get("EXPONENT").getAsString()));
                                } catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException
                                        | IllegalBlockSizeException | NoSuchPaddingException e) {
                                    logger.debug("Exception happened: {}", e.toString());
                                }
                            }

                            if (walletByChannelUID.containsKey(channelUID)) {

                                /**
                                 * Payment has already been sent once
                                 */

                                success = false;
                            } else {

                                /**
                                 * Processing payment on the Tangle
                                 */

                                logger.debug("Payment on wallet: {}", wallet);
                                logger.debug("Encrypted password: {}", encryptedPassword);
                                logger.debug("Trytes encrypted password: {}",
                                        jota.utils.TrytesConverter.toTrytes(encryptedPassword));

                                /**
                                 * price needs to be indicated in iota, not Miota
                                 */

                                List<Transfer> transfers = new ArrayList<>();
                                Transfer t = new Transfer(wallet, (int) (price * 1000000),
                                        TrytesConverter.toTrytes(encryptedPassword), "999999999999999999999999999");
                                transfers.add(t);
                                IotaBridgeHandler handler = null;
                                Bridge bridge = this.getBridge();
                                if (bridge != null) {
                                    handler = (IotaBridgeHandler) bridge.getHandler();
                                }
                                SendTransferResponse transfer = null;
                                try {
                                    /**
                                     * TODO: remove this and use the bridge
                                     */
                                    IotaAPI api = new IotaAPI.Builder().protocol("http").host("localhost").port("14700")
                                            .build();
                                    if (handler != null) {
                                        transfer = api.sendTransfer(handler.getSeed(), 2, 9, 9, transfers, null, null,
                                                true, true);
                                        logger.debug("IOTA transfer was successfull: {}", transfer);
                                        walletByChannelUID.put(channelUID, wallet);
                                        success = true;
                                    }
                                } catch (Exception e) {
                                    logger.debug("Exception happened: {}", e.toString());
                                }
                            }
                        }
                    }
                }
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "Could not fetch data");
        }
        return success;
    }

    /**
     * Updates the payment status
     *
     */
    private synchronized void updatePaymentStatus(ChannelUID channelUID, String status) {
        ChannelConfig config = channelDataByChannelUID.get(channelUID);
        config.processMessage(status);
    }

    /**
     * Checks the payment status for a wallet address on the Tangle
     *
     * @param channelUID
     */
    private synchronized void checkPaymentStatus(ChannelUID channelUID) {

        String wallet = walletByChannelUID.get(channelUID);
        boolean status = utils.checkTransactionStatus(wallet);

        if (status) {

            updatePaymentStatus(channelUID, "success");

            ChannelConfig config = channelDataByChannelUID.get(channelUID);
            ThingTypeUID thingTypeUID = new ThingTypeUID("iota", "topic");
            ThingUID thingUIDObject = new ThingUID("iota", "topic", channelUID.getId());
            ThingUID bridgeUID = this.getThing().getBridgeUID();
            String label = "Topic";
            Map<String, Object> properties = new HashMap<>();
            properties.put("root", nextroot);
            properties.put("refresh", 60);
            properties.put("mode", mode);
            properties.put("key", config.ownkey);
            Configuration configuration = new Configuration(properties);

            /**
             * Payment is successfull, create a topic thing
             */

            Thing newThing = this.thingRegistry.createThingOfType(thingTypeUID, thingUIDObject, bridgeUID, label,
                    configuration);
            this.thingRegistry.add(newThing);

            /**
             * Payment is successfull, removing channel
             */

            ThingBuilder thingBuilder = editThing();

            List<Channel> channels = new ArrayList<>(this.getThing().getChannels());
            for (Iterator<Channel> it = channels.iterator(); it.hasNext();) {
                Channel channel = it.next();
                if (channelUID.toString().equals(channel.getUID().toString())) {
                    logger.debug("Payment received for channel: {}, removing it.", channelUID);
                    it.remove();
                    channelDataByChannelUID.remove(channelUID);
                    walletByChannelUID.remove(channelUID);
                }
            }

            thingBuilder = thingBuilder.withChannels(channels);
            updateThing(thingBuilder.build());

        } else {
            updatePaymentStatus(channelUID, "processing...");
        }
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

    @Override
    public void channelStateUpdated(ChannelUID channelUID, State value) {
        updateState(channelUID.getId(), value);
    }

}
