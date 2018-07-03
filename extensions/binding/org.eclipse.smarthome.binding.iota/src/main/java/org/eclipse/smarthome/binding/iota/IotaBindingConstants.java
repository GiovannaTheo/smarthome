package org.eclipse.smarthome.binding.iota;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.Sets;

/**
 * The {@link IotaBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Theo Giovanna - Initial contribution
 */
@NonNullByDefault
public class IotaBindingConstants {

    private static final String BINDING_ID = "iota";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_IOTA = new ThingTypeUID(BINDING_ID, "topic");
    public static final ThingTypeUID THING_TYPE_IOTA_PAYMENT = new ThingTypeUID(BINDING_ID, "automaticPayment");
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");

    public static final Set<ThingTypeUID> BRIDGE_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_BRIDGE);
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets.newHashSet(THING_TYPE_IOTA,
            THING_TYPE_IOTA_PAYMENT);

    // Bridge config properties
    public static final String PROTOCOL = "protocol";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String SEED = "seed";

    // Channels
    public static final String TEXT_CHANNEL = "text";
    public static final String NUMBER_CHANNEL = "number";
    public static final String PERCENTAGE_CHANNEL = "percentage";
    public static final String ONOFF_CHANNEL = "onoff";
    public static final String PAYMENT_CHANNEL = "textPayment";
}
