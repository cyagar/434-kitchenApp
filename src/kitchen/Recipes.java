package kitchen;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/** One-screen recipe search and instructions, using the app's own API configuration. */
public class Recipes extends JPanel {
    private static final long serialVersionUID = 1L;

    private AppData data;
    private final DefaultComboBoxModel<RecipeService.Recipe> model = new DefaultComboBoxModel<>();
    private final JComboBox<RecipeService.Recipe> results = new JComboBox<>(model);
    private final JTextArea details = new JTextArea();
    private final WrapText pantryLabel = new WrapText("Add food in Food to find recipes.");
    private final WrapText statusLabel = new WrapText("Find meals that use ingredients you already have.");
    private final JButton find = new JButton("Find Recipes");
    private final JButton shop = new JButton("Add Missing to Shopping");
    private final JButton browser = new JButton("Open Recipe Website");
    private final Map<Integer, RecipeService.RecipeDetails> detailsCache = new HashMap<>();
    private RecipeService.RecipeDetails displayedDetails;
    private int selectionRequest = 0;

    public Recipes() {
        setLayout(new BorderLayout(9, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout(5, 7));
        JLabel heading = new JLabel("Discover recipes");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 17f));
        top.add(heading, BorderLayout.NORTH);
        top.add(pantryLabel, BorderLayout.CENTER);
        JPanel search = new JPanel(new BorderLayout(8, 4));
        search.add(find, BorderLayout.WEST);
        search.add(statusLabel, BorderLayout.CENTER);
        top.add(search, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // One unified page: a compact recipe picker and automatically populated
        // instructions. No second screen, split-pane or "View Instructions" step.
        JPanel content = new JPanel(new BorderLayout(7, 9));
        JPanel matches = new JPanel(new BorderLayout(3, 5));
        matches.add(new JLabel("Choose a recipe"), BorderLayout.NORTH);
        matches.add(results, BorderLayout.CENTER);
        content.add(matches, BorderLayout.NORTH);

        JPanel selectedRecipe = new JPanel(new BorderLayout(6, 7));
        selectedRecipe.add(new JLabel("Recipe details and instructions"), BorderLayout.NORTH);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setText("Search for recipes, then choose one above. Ingredients and "
                + "full instructions will appear here automatically.");
        details.setMargin(new java.awt.Insets(12, 12, 12, 12));
        selectedRecipe.add(new JScrollPane(details), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.add(shop);
        actions.add(browser);
        selectedRecipe.add(actions, BorderLayout.SOUTH);
        content.add(selectedRecipe, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        WrapText caution = new WrapText("Recipe matches are not allergy-screened; check ingredient labels.");
        caution.setFont(caution.getFont().deriveFont(11f));
        add(caution, BorderLayout.SOUTH);

        setRecipeActionsEnabled(false);
        results.setEnabled(false);
        find.addActionListener(event -> searchRecipes());
        shop.addActionListener(event -> addMissing());
        browser.addActionListener(event -> openWebsite());
        results.addActionListener(event -> showSelected());
    }

    public void setAppData(AppData data) {
        this.data = data;
        refreshData();
    }

    public void refreshData() {
        List<String> ingredients = availableIngredients();
        String names = ingredients.isEmpty() ? "None yet" : String.join(", ", ingredients);
        pantryLabel.setText("Your ingredients (" + ingredients.size() + "): " + names);
        pantryLabel.setToolTipText(names);
    }

    private List<String> availableIngredients() {
        List<String> names = new ArrayList<>();
        if (data != null) {
            for (String item : data.foods) {
                String[] fields = Food.parts(item);
                String name = fields[0].trim();
                if (!fields[2].equalsIgnoreCase("Empty") && !name.isEmpty()
                        && names.stream().noneMatch(existing -> existing.equalsIgnoreCase(name))) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private void searchRecipes() {
        List<String> ingredients = availableIngredients();
        if (ingredients.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add available foods in the Food tab first.");
            return;
        }
        // No user-facing API key field. The developer configures the shared key once.
        if (!RecipeConfig.isConfigured()) {
            statusLabel.setText("Recipe search unavailable: service not configured in this build.");
            details.setText("Recipe search has not been configured by the app developer. "
                    + "The household user does not need an API key.\n\n"
                    + "Developer: set SPOONACULAR_API_KEY in RecipeConfig.java "
                    + "or in the build environment, then restart the app.");
            return;
        }

        ++selectionRequest;
        model.removeAllElements();
        results.setEnabled(false);
        detailsCache.clear();
        displayedDetails = null;
        setRecipeActionsEnabled(false);
        find.setEnabled(false);
        find.setText("Searching...");
        statusLabel.setText("Searching available recipes...");
        details.setText("Searching for recipes using your ingredients...");

        RecipeService service = new RecipeService(RecipeConfig.apiKey());
        new SwingWorker<List<RecipeService.Recipe>, Void>() {
            @Override protected List<RecipeService.Recipe> doInBackground() throws Exception {
                return service.findByIngredients(ingredients);
            }

            @Override protected void done() {
                find.setEnabled(true);
                find.setText("Find Recipes");
                try {
                    List<RecipeService.Recipe> recipes = get();
                    for (RecipeService.Recipe recipe : recipes) model.addElement(recipe);
                    statusLabel.setText(recipes.size() + " recipe(s) found");
                    results.setEnabled(!recipes.isEmpty());
                    if (recipes.isEmpty()) {
                        details.setText("No recipes found. Try adding other foods to your kitchen.");
                    } else {
                        results.setSelectedIndex(0); // Loads first recipe automatically.
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Recipe search failed");
                    details.setText("Could not load recipes: " + errorMessage(ex));
                }
            }
        }.execute();
    }

    private void showSelected() {
        int request = ++selectionRequest;
        RecipeService.Recipe recipe = (RecipeService.Recipe) results.getSelectedItem();
        displayedDetails = null;
        setRecipeActionsEnabled(recipe != null);
        if (recipe == null) return;

        String overview = recipe.title + "\n\nIngredients you have:\n  "
                + (recipe.used.isEmpty() ? "None reported" : String.join(", ", recipe.used))
                + "\n\nIngredients missing:\n  "
                + (recipe.missing.isEmpty() ? "None reported" : String.join(", ", recipe.missing));
        details.setText(overview + "\n\nLoading full instructions...");
        details.setCaretPosition(0);

        RecipeService.RecipeDetails cached = detailsCache.get(recipe.id);
        if (cached != null) {
            displayDetails(recipe, cached, overview);
            return;
        }
        RecipeService service = new RecipeService(RecipeConfig.apiKey());
        new SwingWorker<RecipeService.RecipeDetails, Void>() {
            @Override protected RecipeService.RecipeDetails doInBackground() throws Exception {
                return service.getDetails(recipe.id);
            }

            @Override protected void done() {
                try {
                    RecipeService.RecipeDetails full = get();
                    detailsCache.put(recipe.id, full);
                    if (request == selectionRequest && results.getSelectedItem() == recipe)
                        displayDetails(recipe, full, overview);
                } catch (Exception ex) {
                    if (request == selectionRequest && results.getSelectedItem() == recipe) {
                        details.setText(overview + "\n\nCould not load instructions: " + errorMessage(ex)
                                + "\nYou can still open the recipe website.");
                    }
                }
            }
        }.execute();
    }

    private void displayDetails(RecipeService.Recipe recipe,
                                RecipeService.RecipeDetails full, String overview) {
        displayedDetails = full;
        String instructions = full.instructions == null ? "" : full.instructions
                .replaceAll("<[^>]*>", " ").replace("&nbsp;", " ")
                .replace("&amp;", "&").replace("&quot;", "\"").trim();
        details.setText(overview + "\n\nReady in: " + full.minutes + " minutes"
                + "    Servings: " + full.servings
                + "\n\nAll ingredients:\n - " + String.join("\n - ", full.ingredients)
                + "\n\nInstructions:\n" + (instructions.isEmpty()
                ? "See the recipe website for instructions." : instructions)
                + "\n\nSource: " + (full.sourceUrl == null ? "" : full.sourceUrl));
        details.setCaretPosition(0);
    }

    private static String errorMessage(Exception error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    private void setRecipeActionsEnabled(boolean enabled) {
        shop.setEnabled(enabled);
        browser.setEnabled(enabled);
    }

    private void addMissing() {
        RecipeService.Recipe recipe = (RecipeService.Recipe) results.getSelectedItem();
        if (recipe == null || data == null) {
            JOptionPane.showMessageDialog(this, "Choose a recipe first.");
            return;
        }
        int added = 0;
        for (String raw : recipe.missing) {
            String name = raw.trim();
            if (name.isEmpty() || name.contains("|")) continue;
            boolean exists = data.shoppingItems.stream()
                    .anyMatch(item -> Food.shoppingName(item).equalsIgnoreCase(name));
            if (!exists) {
                data.shoppingItems.add(name);
                added++;
            }
        }
        JOptionPane.showMessageDialog(this, added + " missing ingredient(s) added to Shopping.");
    }

    private void openWebsite() {
        RecipeService.Recipe recipe = (RecipeService.Recipe) results.getSelectedItem();
        if (recipe == null) {
            JOptionPane.showMessageDialog(this, "Choose a recipe first.");
            return;
        }
        String url = displayedDetails != null ? displayedDetails.sourceUrl : null;
        if (url == null || url.isEmpty()) {
            url = "https://spoonacular.com/recipes/"
                    + recipe.title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("(^-|-$)", "") + "-" + recipe.id;
        }
        try {
            URI uri = URI.create(url);
            if (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme()))
                throw new IllegalArgumentException("Unsupported URL");
            Desktop.getDesktop().browse(uri);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot open recipe website: " + ex.getMessage());
        }
    }
}
