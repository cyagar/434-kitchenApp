package kitchen;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

// Shared session lists. Example data is all here.
public class AppData {
    public final List<String> foods = new ArrayList<>();
    public final List<String> shoppingItems = new ArrayList<>();
    public final List<String> recipes = new ArrayList<>();
    public final List<String> profiles = new ArrayList<>();
    private static final String TEXT_PREFIX = "~kitchen-text~";

    public AppData() { this(true); }
    public AppData(boolean sampleData) {
        if (!sampleData) return;
        foods.add(item("Milk", "Staple", "1", "gallons", "Fridge", "", "Running low"));
        foods.add(item("Pasta", "Staple", "2", "packs", "Cabinet", ""));
        foods.add(item("Tomato", "Single use", "4", "pieces", "Fridge", ""));
        foods.add(item("Chicken", "Single use", "2", "lb", "Freezer", ""));
        shoppingItems.add(item("Milk", "Staple", "2", "gallons", "Fridge", ""));
        shoppingItems.add(item("Olive oil", "Staple", "1", "bottles", "Cabinet", ""));
        shoppingItems.add(item("Eggs", "Staple", "12", "pieces", "Fridge", ""));
        profiles.add(join("Alex", "peanuts", "vegetarian"));
        profiles.add(join("Sam", "milk", ""));
    }

    public static String item(String name, String type, String quantity, String unit, String storage, String full) {
        return item(name, type, quantity, unit, storage, full, "Filled");
    }

    public static String item(String name, String type, String quantity, String unit, String storage, String full, String status) {
        // Status is explicit. Quantity and the legacy full-amount field do not determine it.
        return join(name, "Uncategorized", status, type, quantity, unit, storage, full);
    }

    // Keep the team's delimited records, escaping only fields that could contain a delimiter.
    // Ordinary old records are unchanged. Text such as "Milk | oat" round-trips safely.
    public static String encode(String text) {
        if (!text.contains("|") && !text.startsWith(TEXT_PREFIX)) return text;
        return TEXT_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
    public static String decode(String text) {
        if (!text.startsWith(TEXT_PREFIX)) return text;
        try { return new String(Base64.getUrlDecoder().decode(text.substring(TEXT_PREFIX.length())), StandardCharsets.UTF_8); }
        catch (IllegalArgumentException error) { return text; }
    }
    public static String join(String... values) {
        String[] encoded = new String[values.length];
        for (int i = 0; i < values.length; i++) encoded[i] = encode(values[i]);
        return String.join(" | ", encoded);
    }
    public static String[] fields(String record) {
        if (record == null) return new String[]{""};
        String[] values = record.split(" \\| ", -1);
        for (int i = 0; i < values.length; i++) values[i] = decode(values[i]);
        return values;
    }
}
