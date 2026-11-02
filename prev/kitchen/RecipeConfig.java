package kitchen;

/**
 * Application-owned recipe service configuration.
 *
 * Set SPOONACULAR_API_KEY once when building the app, or provide it through
 * the SPOONACULAR_API_KEY environment variable. Household users never enter a key.
 *
 * IMPORTANT: A key embedded in a distributed Java desktop app can be extracted.
 * For a publicly distributed product, route recipe requests through a backend
 * which keeps the API key private instead.
 */
public final class RecipeConfig {
    private RecipeConfig() { }

    // Developer: put your shared Spoonacular key between the quotes for a local/demo build.
    private static final String SPOONACULAR_API_KEY = "78d5f72d560142e580794d9ab141fa91";

    public static String apiKey() {
        String override = System.getenv("SPOONACULAR_API_KEY");
        return override != null && !override.trim().isEmpty()
                ? override.trim() : SPOONACULAR_API_KEY.trim();
    }

    public static boolean isConfigured() {
        return !apiKey().isEmpty();
    }
}
