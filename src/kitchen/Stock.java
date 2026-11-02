package kitchen;

/** Keeps stock thresholds consistent across Food and Shopping. */
public final class Stock {
    private Stock() { }

    public static String status(double remaining, double fullAmount) {
        if (!Double.isFinite(remaining) || remaining < 0) throw new IllegalArgumentException("Invalid remaining quantity");
        if (!Double.isFinite(fullAmount) || fullAmount <= 0) throw new IllegalArgumentException("Invalid full quantity");
        if (remaining == 0) return "Empty";
        if (remaining <= 1 || remaining <= fullAmount / 2.0) return "Running low";
        return "Filled";
    }

    public static Double parse(String text) {
        try {
            if (text == null || text.trim().isEmpty()) return null;
            double n = Double.parseDouble(text.trim());
            return Double.isFinite(n) && n >= 0 ? n : null;
        } catch (NumberFormatException e) { return null; }
    }

    public static String format(double value) {
        if (value == Math.rint(value) && value <= Long.MAX_VALUE) return Long.toString((long) value);
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
