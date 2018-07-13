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

import static org.eclipse.smarthome.binding.iota.handler.IotaConfiguration.*;

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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.smarthome.binding.iota.IotaBindingConstants;
import org.eclipse.smarthome.binding.iota.internal.NumberValue;
import org.eclipse.smarthome.binding.iota.internal.OnOffValue;
import org.eclipse.smarthome.binding.iota.internal.PercentValue;
import org.eclipse.smarthome.binding.iota.internal.TextValue;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.transform.TransformationException;
import org.eclipse.smarthome.core.transform.TransformationService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.iota.security.RSAUtils;
import org.eclipse.smarthome.io.iota.utils.IotaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jota.IotaAPI;
import jota.dto.response.SendTransferResponse;
import jota.model.Transfer;
import jota.utils.TrytesConverter;

/**
 * The {@link IotaThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Theo Giovanna - Initial contribution
 */
public class IotaThingHandler extends BaseThingHandler implements ChannelStateUpdateListener {

    // TODO: add tests
    // TODO: solve the JSONObject error at topic creation

    private final Logger logger = LoggerFactory.getLogger(IotaThingHandler.class);
    private TransformationServiceProvider transformationServiceProvider;
    private final Map<ChannelUID, ChannelConfig> channelDataByChannelUID = new HashMap<>();
    private final Map<ChannelUID, String> walletByChannelUID = new HashMap<>();
    private final Map<ChannelUID, Boolean> paymentSentByChannelUID = new HashMap<>();
    private String root;
    private JsonArray data = new JsonArray();
    private JsonObject dataPayment = new JsonObject();
    private int refresh = 0;
    private String mode;
    private String nextroot;
    private String key = null;
    private IotaUtils utils;
    ScheduledFuture<?> refreshJob;
    ScheduledFuture<?> refreshJobPayment;
    private ThingRegistry thingRegistry;

    public IotaThingHandler(Thing thing, TransformationServiceProvider transformationServiceProvider,
            ThingRegistry thingRegistry) {
        super(thing);
        this.transformationServiceProvider = transformationServiceProvider;
        this.thingRegistry = thingRegistry;
    }

    public IotaThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        if (!channelUID.getId().equals(IotaBindingConstants.PAYMENT_CHANNEL)) {
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
    }

    @Override
    public void initialize() {

        logger.debug("Initializing Iota handler.");

        Configuration configuration = getThing().getConfiguration();

        setRoot(getOrDefault(configuration.get(ROOT_ADDRESS), getRoot()));
        setRefresh(getOrDefault(configuration.get(REFRESH_INTERVAL), getRefresh()));
        setMode(getOrDefault(configuration.get(MODE), getMode()));
        setKey(getOrDefault(configuration.get(PRIVATE_KEY), getKey()));

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

        updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);

        for (Channel channel : thing.getChannels()) {
            if (!channelDataByChannelUID.containsKey(channel.getUID())) {
                ChannelConfig config = channel.getConfiguration().as(ChannelConfig.class);
                config.channelUID = channel.getUID();
                config.transformationServiceProvider = transformationServiceProvider;
                config.channelStateUpdateListener = this;

                if (StringUtils.isNotBlank(config.transformationPattern)) {
                    int index = config.transformationPattern.indexOf(':');
                    if (index == -1) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                "The transformation pattern must consist of the type and the pattern separated by a colon");
                        return;
                    }
                    String type = config.transformationPattern.substring(0, index).toUpperCase();
                    config.transformationPattern = config.transformationPattern.substring(index + 1);
                    config.transformationServiceName = type;
                }

                switch (channel.getChannelTypeUID().getId()) {
                    case IotaBindingConstants.TEXT_CHANNEL:
                        config.value = new TextValue();
                        startAutomaticRefresh();
                        break;
                    case IotaBindingConstants.NUMBER_CHANNEL:
                        config.value = new NumberValue(config.isFloat, config.step);
                        startAutomaticRefresh();
                        break;
                    case IotaBindingConstants.PERCENTAGE_CHANNEL:
                        config.value = new PercentValue(config.isFloat, config.min, config.max, config.step);
                        startAutomaticRefresh();
                        break;
                    case IotaBindingConstants.ONOFF_CHANNEL:
                        config.value = new OnOffValue(config.on, config.off, config.inverse);
                        startAutomaticRefresh();
                        break;
                    case IotaBindingConstants.PAYMENT_CHANNEL:
                        config.value = new TextValue();
                        paymentSentByChannelUID.put(channel.getUID(), false);
                        startAutomaticRefreshPayment(channel.getUID());
                        break;
                    default:
                        throw new IllegalArgumentException("ThingTypeUID not recognised");
                }

                channelDataByChannelUID.put(channel.getUID(), config);
            }
        }
    }

    @Override
    public void dispose() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
        }
        if (refreshJobPayment != null) {
            refreshJobPayment.cancel(true);
        }
        for (ChannelConfig channelConfig : channelDataByChannelUID.values()) {
            channelConfig.dispose();
        }
        channelDataByChannelUID.clear();
    }

    private synchronized void startAutomaticRefreshPayment(ChannelUID channelUID) {
        logger.debug("Payment info will refresh every {} seconds", refresh);
        refreshJobPayment = scheduler.scheduleWithFixedDelay(() -> {
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
     * listens on the tangle until data is retrieved
     */
    private synchronized void startAutomaticRefresh() {
        logger.debug("Binding will refresh every {} seconds", refresh);
        int interval = refresh == 0 ? 1 : refresh;
        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                boolean success = fetchItemState();
                if (success) {
                    updateAllStates(data);
                }
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
            }
        }, 0, interval, TimeUnit.SECONDS);
    }

    /**
     *
     * @return success if any data is found in the MAM transaction
     */
    private synchronized boolean fetchItemState() {
        boolean success = false;
        if (!root.isEmpty()) {
            JsonParser parser = new JsonParser();
            JsonObject resp = null;
            try {
                resp = parser.parse(utils.fetchFromTangle(refresh, root, mode, key)).getAsJsonObject();
            } catch (IllegalStateException e) {
                /**
                 * Sometimes the provided is slightly ahead, so we check if the next stream has already been published.
                 */
                try {
                    String nextRoot = utils.getNextRoot(root);
                    logger.debug("Trying next root....{}", nextRoot);
                    resp = parser.parse(utils.fetchFromTangle(refresh, nextRoot, mode, key)).getAsJsonObject();
                } catch (IllegalStateException e1) {
                    logger.debug(
                            "Exception happened: {}. The stream might not have been updated on the provider side, please wait...",
                            e1.toString());
                }
            }
            if (resp != null) {
                if (resp.size() != 0) {
                    root = resp.get("NEXTROOT").getAsString();
                    data = resp.entrySet().iterator().next().getValue().getAsJsonArray();
                    success = true;
                }
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "Could not fetch data");
        }
        return success;
    }

    /**
     * Updates all item states with the fetched data
     *
     * @param data
     */
    private synchronized void updateAllStates(JsonArray data) {
        String str = null;
        if (data.size() != 0) {
            for (Channel channel : thing.getChannels()) {
                ChannelConfig config = channelDataByChannelUID.get(channel.getUID());
                /**
                 * If retrieving data from other sources than ESH, uses the transformation pattern
                 * to retrieve the data
                 */
                if (config.transformationPattern != null) {
                    TransformationService service = transformationServiceProvider
                            .getTransformationService(config.transformationServiceName);
                    if (service == null) {
                        logger.warn("Transformation service '{}' not found", config.transformationServiceName);
                        return;
                    }
                    try {
                        /**
                         * A path to the value has been directly inserted by the user.
                         * For instance, for a json { "device": { "status": { "value": "73" } } },
                         * one can extract the state through $.device.status.value
                         */
                        str = service.transform(config.transformationPattern, data.toString());
                        if (str != null && !str.isEmpty()) {
                            config.processMessage(str);
                        }

                    } catch (TransformationException e) {
                        logger.error("Error executing the transformation {}", str);
                        return;
                    }
                } else {
                    /**
                     * Data were sent through the io.iota bundle
                     */
                    for (Iterator<JsonElement> it = data.iterator(); it.hasNext();) {
                        JsonElement json = it.next();
                        String value = json.getAsJsonObject().get("STATUS").getAsJsonObject().get("STATE")
                                .getAsString();
                        String topic = json.getAsJsonObject().get("STATUS").getAsJsonObject().get("TOPIC")
                                .getAsString();

                        if (!config.stateTopic.isEmpty()) {
                            if (topic.toLowerCase().equals(config.stateTopic.toLowerCase())) {
                                logger.debug("Assigning data {} to channel {}", json, channel.getUID());
                                config.processMessage(value);
                                it.remove(); // this json data is already assigned to some channel
                                break; // no need to loop over the rest of the data since we found a candidate
                            } else {
                                /**
                                 * One can use the "ANY" wildcard to associate to a channel any value
                                 * from the json data, independently of the topic
                                 */
                                if (config.stateTopic.toUpperCase().equals("ANY")) {
                                    logger.debug("Assigning data {} to channel {}", json, channel.getUID());
                                    config.processMessage(value);
                                    it.remove(); // this json data is already assigned to some channel
                                    break; // no need to loop over the rest of the data since we found a candidate
                                }
                            }
                        }
                    }
                }
            }

        }
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
                 * TODO: REMOVE and use the bridge
                 */
                JsonObject resp = null;
                try {
                    resp = parser.parse(mamUtils.fetchFromTangle(refresh, config.root, "restricted", config.key))
                            .getAsJsonObject();
                } catch (IllegalStateException e) {
                    logger.debug("Exception happened: {}", e.toString());
                }

                if (resp != null) {

                    nextroot = resp.get("NEXTROOT").getAsString();
                    dataPayment = resp.entrySet().iterator().next().getValue().getAsJsonObject();

                    if (data != null) {
                        double price = dataPayment.get("PRICE").getAsDouble();
                        if (price <= config.threshold) {

                            /**
                             * Price is smaller than threshold. Authorization granted for processing the payment
                             */

                            String wallet = dataPayment.get("WALLET").getAsString();
                            String encryptedPassword = "";

                            if (config.ownkey != null && !config.ownkey.isEmpty()) {
                                JsonObject rsaJson = dataPayment.get("RSA").getAsJsonObject();
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

            Map<String, Object> properties = new HashMap<>();
            properties.put("root", nextroot);
            properties.put("refresh", 60);
            properties.put("mode", mode);
            properties.put("key", config.ownkey);
            Configuration configuration = new Configuration(properties);

            /**
             * Payment is successfull, updating thing
             */

            ThingBuilder thingBuilder = editThing();

            thingBuilder = thingBuilder.withConfiguration(configuration);
            updateThing(thingBuilder.build());

            refreshJobPayment.cancel(true); // stoping the refresh process

        } else {
            updatePaymentStatus(channelUID, "processing...");
        }
    }

    private static String getOrDefault(Object value, String defaultValue) {
        return value != null ? (String) value : defaultValue;
    }

    private static int getOrDefault(Object value, int defaultValue) {
        return value instanceof BigDecimal ? ((BigDecimal) value).intValue() : defaultValue;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public int getRefresh() {
        return refresh;
    }

    public void setRefresh(int refresh) {
        this.refresh = refresh;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void channelStateUpdated(ChannelUID channelUID, State value) {
        updateState(channelUID.getId(), value);
    }
}
