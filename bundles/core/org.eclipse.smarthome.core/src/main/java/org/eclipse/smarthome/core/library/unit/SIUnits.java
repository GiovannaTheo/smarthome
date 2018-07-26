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
package org.eclipse.smarthome.core.library.unit;

import javax.measure.Unit;
import javax.measure.quantity.Area;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Volume;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;

import tec.uom.se.format.SimpleUnitFormat;
import tec.uom.se.unit.Units;

/**
 * Delegate SI units to {@link Units} to hide this dependency from the rest of ESH.
 * See members of {@link Units} for a detailed description.
 *
 * @author Henning Treu - initial contribution
 *
 */
@NonNullByDefault
public class SIUnits extends SmartHomeUnits {

    private static final SIUnits INSTANCE = new SIUnits();

    private SIUnits() {
        // avoid external instantiation
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Returns the unique instance of this class.
     *
     * @return the Units instance.
     */
    public static SystemOfUnits getInstance() {
        return INSTANCE;
    }

    public static final Unit<Temperature> CELSIUS = addUnit(Units.CELSIUS);
    public static final Unit<Speed> KILOMETRE_PER_HOUR = addUnit(Units.KILOMETRE_PER_HOUR);
    public static final Unit<Length> METRE = addUnit(Units.METRE);
    public static final Unit<Mass> KILOGRAM = addUnit(Units.KILOGRAM);
    public static final Unit<Mass> GRAM = addUnit(Units.GRAM);
    public static final Unit<Area> SQUARE_METRE = addUnit(Units.SQUARE_METRE);
    public static final Unit<Volume> CUBIC_METRE = addUnit(Units.CUBIC_METRE);
    public static final Unit<Pressure> PASCAL = addUnit(Units.PASCAL);

    /**
     * Adds a new unit not mapped to any specified quantity type.
     *
     * @param unit the unit being added.
     * @return <code>unit</code>.
     */
    private static <U extends Unit<?>> U addUnit(U unit) {
        INSTANCE.units.add(unit);
        addPrefixUnit(unit);
        return unit;
    }

    static {
        // Override the default unit symbol ℃ to better support TTS and UIs:
        SimpleUnitFormat.getInstance().label(CELSIUS, "°C");
    }

    private static void addPrefixUnit(Unit<?> unit) {
        if (unit.getSymbol() != null) {
            SimpleUnitFormat.getInstance().label(MetricPrefix.YOTTA(unit), "Y" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.ZETTA(unit), "Z" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.EXA(unit), "E" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.PETA(unit), "P" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.TERA(unit), "T" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.GIGA(unit), "G" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.MEGA(unit), "M" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.KILO(unit), "k" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.HECTO(unit), "h" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.DEKA(unit), "da" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.DECI(unit), "d" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.CENTI(unit), "c" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.MILLI(unit), "m" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.MICRO(unit), "µ" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.NANO(unit), "n" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.PICO(unit), "p" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.FEMTO(unit), "f" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.ATTO(unit), "a" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.ZEPTO(unit), "z" + unit.getSymbol());
            SimpleUnitFormat.getInstance().label(MetricPrefix.YOCTO(unit), "y" + unit.getSymbol());
        }
    }
}
