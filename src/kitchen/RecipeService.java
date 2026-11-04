package kitchen;

import java.io.IOException;
import java.util.*;

// Bundled and user recipes, matched entirely in memory.
public class RecipeService {
    private final List<String> customRecipes;
    public RecipeService() { this(Collections.emptyList()); }
    public RecipeService(List<String> customRecipes) {
        this.customRecipes = new ArrayList<>(customRecipes);
    }

    public List<Recipe> findByIngredients(List<String> pantry) {
        List<Recipe> result = new ArrayList<>();
        Set<String> available = new HashSet<>();
        if (pantry != null) for (String name : pantry)
            if (name != null && !name.trim().isEmpty()) available.add(normalize(name));
        List<Integer> ids = new ArrayList<>();
        for (int id = 1; id <= 5; id++) ids.add(id);
        for (int i = 0; i < customRecipes.size(); i++) ids.add(1000 + i);
        for (int id : ids) {
            RecipeDetails details;
            try { details = getDetails(id); }
            catch (IOException invalidSavedRecipe) { continue; }
            Recipe recipe = new Recipe(id, details.title);
            for (String ingredient : details.ingredients) {
                boolean present = available.contains(normalize(ingredient));
                (present ? recipe.used : recipe.missing).add(ingredient);
            }
            recipe.usedCount = recipe.used.size();
            result.add(recipe);
        }
        return result;
    }

    private static String normalize(String name) {
        String value = name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (value.endsWith("ies") && value.length() > 3) return value.substring(0, value.length() - 3) + "y";
        if (value.endsWith("oes")) return value.substring(0, value.length() - 2);
        if (value.endsWith("s") && !value.endsWith("ss")) return value.substring(0, value.length() - 1);
        return value;
    }

    // Every detail is bundled locally, new objects prevent mutable result sharing.
    public RecipeDetails getDetails(int id) throws IOException {
        RecipeDetails d = new RecipeDetails();
        if (id >= 1000) {
            int index = id - 1000;
            if (index >= customRecipes.size()) throw new IOException("Recipe not found.");
            String[] fields = AppData.fields(customRecipes.get(index));
            if (fields.length != 5) throw new IOException("Recipe details are incomplete.");
            try { d.minutes = Integer.parseInt(fields[1]); d.servings = Integer.parseInt(fields[2]); }
            catch (NumberFormatException error) { throw new IOException("Recipe timing is invalid.", error); }
            if (d.minutes <= 0 || d.servings <= 0 || fields[0].trim().isEmpty() || fields[4].trim().isEmpty())
                throw new IOException("Recipe details are incomplete.");
            d.title = fields[0]; d.instructions = fields[4];
            for (String ingredient : fields[3].split("\\R"))
                if (!ingredient.trim().isEmpty()) d.ingredients.add(ingredient.trim());
            if (d.ingredients.isEmpty()) throw new IOException("Recipe has no ingredients.");
            return d;
        }
        switch (id) {
            case 1:
                d.title = "Tomato and spinach pasta"; d.minutes = 25; d.servings = 2;
                d.ingredients.addAll(Arrays.asList("Pasta", "Tomato", "Spinach", "Olive oil"));
                d.instructions = "1. Cook 200 g pasta according to the package.\n\n2. Chop 2 tomatoes. Heat 1 tbsp olive oil in a pan; cook tomatoes for 5 minutes.\n\n3. Add 2 cups spinach and cook until wilted. Toss with drained pasta and serve.";
                break;
            case 2:
                d.title = "Spinach omelet"; d.minutes = 10; d.servings = 1;
                d.ingredients.addAll(Arrays.asList("Eggs", "Spinach", "Butter"));
                d.instructions = "1. Beat 2 eggs.\n\n2. Melt 1 tsp butter in a nonstick pan and wilt 1 cup spinach.\n\n3. Add the eggs and cook over medium-low heat until set throughout. Fold and serve.";
                break;
            case 3:
                d.title = "Vegetable rice bowl"; d.minutes = 25; d.servings = 2;
                d.ingredients.addAll(Arrays.asList("Rice", "Tomato", "Spinach", "Olive oil"));
                d.instructions = "1. Cook 1 cup rice according to its package.\n\n2. Warm 1 tbsp olive oil in a pan. Add 2 chopped tomatoes and cook until soft.\n\n3. Add 1 cup spinach and cook until wilted. Serve over the rice.";
                break;
            case 4:
                d.title = "Banana oatmeal"; d.minutes = 10; d.servings = 1;
                d.ingredients.addAll(Arrays.asList("Oats", "Milk", "Banana"));
                d.instructions = "1. Combine half a cup oats and 1 cup milk in a saucepan.\n\n2. Simmer, stirring, until soft.\n\n3. Slice 1 banana over the oatmeal and serve.";
                break;
            case 5:
                d.title = "Chicken rice bowl"; d.minutes = 30; d.servings = 2;
                d.ingredients.addAll(Arrays.asList("Chicken", "Rice", "Tomato", "Spinach"));
                d.instructions = "1. Cook 1 cup rice according to its package.\n\n2. Cut 1 lb chicken into pieces and cook in a pan until the center reaches 165 F.\n\n3. Add 2 chopped tomatoes and 1 cup spinach. Cook until wilted and serve over rice.";
                break;
            default: throw new IOException("This recipe is not in the bundled catalogue.");
        }
        return d;
    }

    public static final class Recipe {
        public final int id;
        public final String title;
        public int usedCount;
        public final List<String> used=new ArrayList<>(),missing=new ArrayList<>();
        public Recipe(int id,String title){this.id=id;this.title=title;}
        @Override public String toString(){return title+"  (have "+usedCount+", missing "+missing.size()+")";}
    }
    public static final class RecipeDetails {
        public String title,instructions;
        public int minutes,servings;
        public final List<String> ingredients=new ArrayList<>();
    }
}
