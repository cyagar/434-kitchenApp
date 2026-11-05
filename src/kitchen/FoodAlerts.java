package kitchen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

// Name based warning hints for the household's declared allergies and diets.
public final class FoodAlerts {
    private FoodAlerts() { }

    private static final List<String> PEANUTS = terms(
            "peanut", "peanuts", "groundnut", "groundnuts", "peanut butter");
    private static final List<String> TREE_NUTS = terms(
            "almond", "almonds", "cashew", "cashews", "walnut", "walnuts",
            "pecan", "pecans", "hazelnut", "hazelnuts", "pistachio", "pistachios",
            "macadamia", "brazil nut", "pine nut", "mixed nuts");
    private static final List<String> DAIRY = terms(
            "milk", "cheese", "cheddar", "mozzarella", "parmesan", "yogurt",
            "yoghurt", "butter", "cream", "ice cream", "whey", "casein", "ghee",
            "custard", "milkshake", "buttermilk", "kefir");
    private static final List<String> EGGS = terms(
            "egg", "eggs", "mayonnaise", "mayo", "meringue");
    private static final List<String> SOY = terms(
            "soy", "soybean", "soybeans", "tofu", "tempeh", "edamame", "miso");
    private static final List<String> GLUTEN = terms(
            "gluten", "wheat", "bread", "pasta", "flour", "couscous", "bulgur",
            "seitan", "cracker", "crackers", "bagel", "bagels", "barley", "rye",
            "tortilla", "tortillas", "cookies", "cookie", "semolina");
    private static final List<String> FISH = terms(
            "fish", "salmon", "tuna", "cod", "anchovy", "anchovies", "sardine",
            "sardines", "trout", "tilapia", "haddock", "halibut");
    private static final List<String> SHELLFISH = terms(
            "shellfish", "shrimp", "prawn", "prawns", "crab", "lobster", "clam",
            "clams", "oyster", "oysters", "mussel", "mussels", "scallop",
            "scallops", "crawfish", "crayfish");
    private static final List<String> SESAME = terms("sesame", "tahini", "halva");
    private static final List<String> MEAT = terms(
            "beef", "steak", "ground beef", "pork", "bacon", "ham", "sausage",
            "salami", "pepperoni", "lard", "chicken", "turkey", "lamb", "mutton",
            "duck", "gelatin", "gelatine", "prosciutto");
    private static final List<String> PORK = terms("pork", "bacon", "ham", "lard", "prosciutto", "pork sausage");
    private static final List<String> BEEF = terms("beef", "steak", "ground beef", "veal");

    private static List<String> terms(String... words) {
        return Collections.unmodifiableList(Arrays.asList(words));
    }

    // Returned strings identify the member and the possible reason for a warning.
    public static List<String> warningsFor(String foodName, List<String> rawProfiles) {
        if (foodName == null || rawProfiles == null) return Collections.emptyList();
        String food = normalize(foodName);
        if (food.isEmpty()) return Collections.emptyList();
        Set<String> warnings = new LinkedHashSet<>();
        for (String raw : rawProfiles) {
            String[] profile = AppData.fields(raw);
            String member = profile.length > 0 ? profile[0].trim() : "Member";
            if (member.isEmpty()) member = "Member";
            if (profile.length > 1) {
                for (String declared : splitPreferences(profile[1])) {
                    if (matchesAllergy(food, declared)) {
                        warnings.add(member + ": allergy - " + declared);
                    }
                }
            }
            if (profile.length > 2) {
                for (String declared : splitPreferences(profile[2])) {
                    if (matchesDiet(food, declared)) {
                        warnings.add(member + ": dietary restriction - " + declared);
                    }
                }
            }
        }
        return new ArrayList<>(warnings);
    }

    private static List<String> splitPreferences(String input) {
        if (input == null || input.trim().isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String preference : input.split("[,;\\n]+|(?i)\\s+and\\s+")) {
            String p = preference.trim();
            if (!p.isEmpty() && !p.equalsIgnoreCase("none") && !p.equalsIgnoreCase("n/a")) {
                result.add(p);
            }
        }
        return result;
    }

    private static boolean matchesAllergy(String food, String allergy) {
        String a = singular(normalize(allergy).replaceAll("^(allergic to|allergy to) ", ""));
        if (a.isEmpty()) return false;
        if (has(a, "peanut", "groundnut")) return containsAny(food, PEANUTS);
        if (has(a, "tree nut", "nut", "nuts", "nut allergy")) return containsAny(food, TREE_NUTS) || containsAny(food, PEANUTS);
        if (has(a, "milk", "dairy", "lactose")) return containsAny(food, DAIRY);
        if (has(a, "egg")) return containsAny(food, EGGS);
        if (has(a, "soy", "soya")) return containsAny(food, SOY);
        if (has(a, "wheat", "gluten")) return containsAny(food, GLUTEN);
        if (has(a, "shellfish", "crustacean")) return containsAny(food, SHELLFISH);
        if (has(a, "fish", "seafood")) return containsAny(food, FISH) || (has(a, "seafood") && containsAny(food, SHELLFISH));
        if (has(a, "sesame")) return containsAny(food, SESAME);
        return containsPhrase(food, a) || containsPhrase(food, singular(a))
                || containsPhrase(singular(food), a);
    }

    private static boolean matchesDiet(String food, String diet) {
        String d = normalize(diet);
        if (d.isEmpty()) return false;
        if (has(d, "vegan", "plant based", "plant-based")) {
            return containsAny(food, MEAT) || containsAny(food, FISH) || containsAny(food, SHELLFISH)
                    || containsAny(food, DAIRY) || containsAny(food, EGGS) || containsPhrase(food, "honey");
        }
        if (has(d, "vegetarian")) {
            return containsAny(food, MEAT) || containsAny(food, FISH) || containsAny(food, SHELLFISH);
        }
        if (has(d, "pescatarian")) return containsAny(food, MEAT);
        if (has(d, "halal", "kosher", "no pork", "pork free", "pork-free")) {
            return containsAny(food, PORK) || (has(d, "kosher") && containsAny(food, SHELLFISH));
        }
        if (has(d, "gluten free", "gluten-free", "celiac", "coeliac", "wheat free", "wheat-free")) {
            return containsAny(food, GLUTEN);
        }
        if (has(d, "dairy free", "dairy-free", "lactose free", "lactose-free", "lactose intolerant")) {
            return containsAny(food, DAIRY);
        }
        if (has(d, "egg free", "egg-free")) return containsAny(food, EGGS);
        if (has(d, "nut free", "nut-free", "no nuts")) return containsAny(food, PEANUTS) || containsAny(food, TREE_NUTS);
        if (has(d, "no seafood", "seafood free")) return containsAny(food, FISH) || containsAny(food, SHELLFISH);
        if (has(d, "no beef", "beef free")) return containsAny(food, BEEF);
        if (has(d, "no red meat")) return containsAny(food, BEEF) || containsAny(food, PORK) || containsPhrase(food, "lamb");
        return false; // Cannot safely deduce salt/sugar/etc. from a food name.
    }

    private static boolean containsAny(String food, List<String> terms) {
        for (String term : terms) if (containsPhrase(food, term)) return true;
        return false;
    }

    private static boolean has(String value, String... terms) {
        for (String term : terms) if (containsPhrase(value, normalize(term))) return true;
        return false;
    }

    private static boolean containsPhrase(String text, String phrase) {
        if (phrase.isEmpty()) return false;
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(phrase) + "(?![a-z0-9])")
                .matcher(text).find();
    }

    private static String singular(String term) {
        if (term.endsWith("ies") && term.length() > 3) return term.substring(0, term.length() - 3) + "y";
        if (term.endsWith("s") && !term.endsWith("ss") && term.length() > 3) return term.substring(0, term.length() - 1);
        return term;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('-', ' ').replaceAll("\\s+", " ").trim();
    }
}
