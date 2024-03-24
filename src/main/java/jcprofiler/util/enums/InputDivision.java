// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <lukaszaoral@outlook.com>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.util.enums;

/**
 * Enum with possible input division types
 */
public enum InputDivision {
    effectiveBitLength,
    hammingWeight,
    none;

    /**
     * Returns a pretty name for given enum value.
     *
     * @return a {@link String} with pretty printed name
     */
    public String prettyPrint() {
        switch (this) {
            case effectiveBitLength:
                return "effective bit length";
            case hammingWeight:
                return "Hamming weight";
            case none:
                return "none";
            default:
                throw new RuntimeException("Unreachable statement reached.");
        }
    }
}
