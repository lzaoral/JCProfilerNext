package jcprofiler.util.enums;

public enum InputDivision {
    effectiveBitLength,
    hammingWeight,
    none;

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
