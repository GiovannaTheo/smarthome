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
package org.eclipse.smarthome.io.iota.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jota.IotaAPI;
import jota.dto.response.SendTransferResponse;
import jota.error.InvalidAddressException;
import jota.error.InvalidSecurityLevelException;
import jota.error.InvalidTransferException;
import jota.error.InvalidTrytesException;
import jota.error.NotEnoughBalanceException;
import jota.model.Transaction;
import jota.model.Transfer;
import jota.utils.TrytesConverter;

/**
 * Provides utils methods to work with IOTA transactions
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class IotaUtils {

    private final Logger logger = LoggerFactory.getLogger(IotaUtils.class);
    private final String[] testHash = new String[] {
            "KSLBYEYSBWSARILO9CYIBQYMXXLCZAK9PIUCPNAEUYGHELWEOQEECGPYBOAIIJBUUMJFJEBDGBMB99999" };
    private final String testMessage = "XCHDTCADDBEACCTCADDDTCFDPCHDIDFDTCEAWCPCGDEAGDHDPCHDTCDBEAWA9BEAEA999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999";
    private final String testTo = "WHHVYHHLINKIDWFVCXMFAM9TZ9RKWTMTOOWJJBIDWGWVMFUUAAYDQMCXZPWEPZGLICZCFQGMOCMWSEP99RWZQHHDYY";
    private final String testSeed = "PQYDZVXCN9UALNNXFVSYGLBWXSHWGBUPKGCCLDDTBPN9WJWPZFVMBLHIEVILPYWEDDTGTNBBWMLMOKDCM";
    private final String emptyTag = "999999999999999999999999999";

    public IotaUtils() {

    }

    /**
     * Cut the fragment message contained in the IOTA transaction to remove the padded 9's
     *
     * @param trytes the trytes to cut
     * @return the broadcasted message, converted from trytes to String
     */
    protected String revealMessage(String trytes) {
        String tmp = "";
        Integer stop = 0;
        for (int i = trytes.length() - 1; i >= 0; i--) {
            if (trytes.charAt(i) != '9' && stop == 0) {
                stop = 1;
                tmp = trytes.charAt(i) + tmp;
            } else {
                if (stop == 1) {
                    tmp = trytes.charAt(i) + tmp;
                }
            }
        }

        logger.debug("Transaction message in trytes: {}", tmp);

        return TrytesConverter.toString(tmp);
    }

    /**
     * Attach an item state on the Tangle
     *
     * @param bridge the IOTA API endpoint
     * @param item the item for which we want to publish data
     * @param state the item's state
     * @param to the recepient address
     * @param seed the seed
     */
    protected void publishState(@NonNull IotaAPI bridge, @NonNull Item item, State state, String to, String seed) {
        List transfers = new ArrayList<>();
        logger.debug("Creating transfer for item {} of state {}", item.getName(), state.toString());
        Transfer t = new Transfer(this.testTo, 0,
                TrytesConverter.toTrytes("item: " + item.getName() + " has state: " + state.toFullString()), emptyTag);
        transfers.add(t);
        try {
            SendTransferResponse resp = bridge.sendTransfer(this.testSeed, 2, 9, 15, transfers, null, null);
            logger.debug("Transaction terminated. Status: {}", resp.toString());
            logger.debug("Now wait for the transaction to be confirmed by the network");
        } catch (NotEnoughBalanceException e) {
            e.printStackTrace();
        } catch (InvalidSecurityLevelException e) {
            e.printStackTrace();
        } catch (InvalidTrytesException e) {
            e.printStackTrace();
        } catch (InvalidAddressException e) {
            e.printStackTrace();
        } catch (InvalidTransferException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param transactions an array of transactions hash
     * @param bridge the IOTA API endpoint
     * @return the message contained in the latest transaction
     */

    protected String getStateFromTransaction(String[] transactions, @NonNull IotaAPI bridge) {
        List<Transaction> t = bridge.getTransactionsObjects(this.testHash);
        String message = t.get(0).getSignatureFragments(); // gets last transaction
        return this.revealMessage(message);
    }
}
