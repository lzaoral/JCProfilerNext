// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <x456487@fi.muni.cz>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.util.enums;

/**
 * Enum with possible time units
 */
public enum TimeUnit {
    nano,
    micro,
    milli,
    sec;

    /**
     * Returns a pretty name for given enum value.
     *
     * @return a {@link String} with pretty printed name
     */
    public String prettyPrint() {
        switch (this) {
            case nano:
                return "ns";
            case micro:
                return "μs";
            case milli:
                return "ms";
            case sec:
                return "s";
            default:
                throw new RuntimeException("Unreachable statement reached!");
        }
    }
}
