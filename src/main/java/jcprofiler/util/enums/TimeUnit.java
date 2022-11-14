package jcprofiler.util.enums;

public enum TimeUnit {
    nano,
    micro,
    milli,
    sec;

    @Override
    public String toString() {
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
