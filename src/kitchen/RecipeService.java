package kitchen;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** Spoonacular API access, independent of Swing. Requires Java 11+. */
public class RecipeService {
    private static final String BASE = "https://api.spoonacular.com";
    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey;

    public RecipeService(String apiKey) { this.apiKey = apiKey == null ? "" : apiKey.trim(); }
    private static String enc(String s) {return URLEncoder.encode(s, StandardCharsets.UTF_8);}

    private Object get(String path) throws IOException, InterruptedException {
        if (apiKey.isEmpty()) throw new IOException("Add your Spoonacular API key first.");
        String separator = path.contains("?") ? "&" : "?";
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path + separator + "apiKey=" + enc(apiKey)))
            .timeout(Duration.ofSeconds(20)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if(resp.statusCode()!=200) {
            String message = SimpleJson.str(SimpleJson.obj(SimpleJson.parse(resp.body())).get("message"));
            throw new IOException("Spoonacular HTTP " + resp.statusCode() + (message.isEmpty()?"":": "+message));
        }
        return SimpleJson.parse(resp.body());
    }

    public List<Recipe> findByIngredients(List<String> pantry) throws IOException, InterruptedException {
        if(pantry.isEmpty()) return Collections.emptyList();
        Object json=get("/recipes/findByIngredients?ingredients=" + enc(String.join(",",pantry))
            + "&number=15&ranking=1&ignorePantry=true");
        List<Recipe> recipes = new ArrayList<>();
        for(Object row : SimpleJson.arr(json)) {
            Map<String,Object> item=SimpleJson.obj(row);
            Recipe recipe=new Recipe(SimpleJson.num(item.get("id")),SimpleJson.str(item.get("title")));
            recipe.usedCount=SimpleJson.num(item.get("usedIngredientCount"));
            for(Object ing: SimpleJson.arr(item.get("usedIngredients")))
                recipe.used.add(SimpleJson.str(SimpleJson.obj(ing).get("name")));
            for(Object ing: SimpleJson.arr(item.get("missedIngredients")))
                recipe.missing.add(SimpleJson.str(SimpleJson.obj(ing).get("name")));
            recipes.add(recipe);
        }
        return recipes;
    }

    public RecipeDetails getDetails(int id) throws IOException, InterruptedException {
        Map<String,Object> json=SimpleJson.obj(get("/recipes/"+id+"/information?includeNutrition=false"));
        RecipeDetails d = new RecipeDetails();
        d.title=SimpleJson.str(json.get("title"));
        d.instructions=SimpleJson.str(json.get("instructions"));
        d.sourceUrl=SimpleJson.str(json.get("sourceUrl"));
        d.minutes=SimpleJson.num(json.get("readyInMinutes"));
        d.servings=SimpleJson.num(json.get("servings"));
        for(Object ingredient:SimpleJson.arr(json.get("extendedIngredients"))) {
            Map<String,Object> ing=SimpleJson.obj(ingredient);
            d.ingredients.add(SimpleJson.str(ing.get("original")));
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
        public String title,instructions,sourceUrl;
        public int minutes,servings;
        public final List<String> ingredients=new ArrayList<>();
    }
}
