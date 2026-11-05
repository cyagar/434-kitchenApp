package kitchen;

// Quantity helpers only; Pantry availability is selected by the user.
public final class Stock {
    private Stock() { }

    public static Double parse(String text) {
        try {
            if (text == null || text.trim().isEmpty()) return null;
            double n = Double.parseDouble(text.trim());
            return Double.isFinite(n) && n >= 0 ? n : null;
        } catch (NumberFormatException e) { return null; }
    }

    public static String[] units() {
        return new String[]{"pieces", "g", "kg", "oz", "lb", "mL", "L", "cups", "tbsp", "tsp", "packs", "bottles", "cans", "gallons", "other / none"};
    }
    public static String unitLabel(String choice) {
        return choice == null || choice.equals("other / none") ? "" : choice;
    }
    // Unit names are stored in the plural; a quantity of exactly 1 reads as singular.
    public static String withUnit(String quantity, String unit) {
        String name = unit.trim();
        if (name.isEmpty()) return quantity;
        Double amount = parse(quantity);
        boolean one = amount != null && amount == 1 && name.endsWith("s");
        return quantity + " " + (one ? name.substring(0, name.length() - 1) : name);
    }

    public static String format(double value) {
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
