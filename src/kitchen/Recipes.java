package kitchen;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Two-stage recipe discovery: sorted, tappable results and a detail page. */
public class Recipes extends JPanel {
    private static final long serialVersionUID = 1L;
    private AppData data;
    private final CardLayout screens = new CardLayout();
    private final JPanel content = new JPanel(screens);
    private final DefaultListModel<RecipeService.Recipe> model = new DefaultListModel<>();
    private final JList<RecipeService.Recipe> results = new JList<RecipeService.Recipe>(model) {
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
    };
    private final WrapText message = new WrapText("Choose a recipe to get started.");
    private final JButton filterButton = new RoundedButton("Filter");
    private final JTextField queryField = new JTextField();
    private final WrapText recipeTitle = new WrapText("");
    private final JLabel recipeCounts = new JLabel();
    private final WrapText recipeNotice = new WrapText("");
    private final WrapText instructions = new WrapText("");
    private final JScrollPane detailScroll = new JScrollPane();
    private final JButton shop = new RoundedButton("Add missing to Shopping");
    private final JButton ingredientsButton = new RoundedButton("Check ingredients");
    private RecipeService.Recipe current;
    private RecipeService.RecipeDetails currentDetails;
    private boolean showingDetail;
    private boolean resettingSearch;
    private int maximumMinutes = Integer.MAX_VALUE;

    public Recipes() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel browserPage = new JPanel(new BorderLayout(10, 10));
        JPanel filters = AppScreens.form();
        JPanel recipeHeader = new JPanel(new BorderLayout(12, 0));
        recipeHeader.setOpaque(false);
        recipeHeader.add(AppScreens.copy("Find your next meal."), BorderLayout.CENTER);
        JButton addRecipe = new RoundedButton("Add recipe");
        addRecipe.setIcon(AppScreens.icon(5, true));
        addRecipe.addActionListener(e -> addRecipe());
        recipeHeader.add(addRecipe, BorderLayout.EAST);
        recipeHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        filters.add(recipeHeader);
        filters.add(Box.createVerticalStrut(12));
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.add(queryField, BorderLayout.CENTER);
        filterButton.setPreferredSize(new Dimension(92, 44));
        searchRow.add(filterButton, BorderLayout.EAST);
        AppScreens.field(filters, "Search recipes or ingredients", searchRow);
        queryField.getAccessibleContext().setAccessibleName("Search recipes or ingredients");
        browserPage.add(filters, BorderLayout.NORTH);
        results.setFixedCellHeight(-1);
        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        results.setCellRenderer((list, recipe, index, selected, focused) -> {
            JPanel row = new JPanel(new BorderLayout(6, 4));
            row.setOpaque(true);
            row.setBorder(new EmptyBorder(8, 10, 8, 10));
            List<String> warnings = recipeWarnings(recipe);
            row.setBackground(!warnings.isEmpty() ? (selected ? new Color(255, 237, 200) : new Color(255, 248, 230))
                    : selected ? new Color(223, 242, 229) : index % 2 == 0 ? Color.WHITE : new Color(248, 250, 248));
            JTextArea name = new JTextArea(recipe.title);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            name.setEditable(false); name.setLineWrap(true); name.setWrapStyleWord(true);
            name.setOpaque(false); name.setFocusable(false);
            int textWidth = Math.max(110, results.getWidth() - 45);
            name.setSize(textWidth, 10000);
            row.add(name, BorderLayout.NORTH);
            JLabel counts = new JLabel("<html><span style='color:#48745c'>In pantry: " + recipe.usedCount + "</span>"
                    + "&nbsp;&nbsp; <span style='color:#b33333'>To buy: " + recipe.missing.size() + "</span></html>");
            counts.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            JPanel metadata = new JPanel(new BorderLayout(8, 0)); metadata.setOpaque(false);
            metadata.add(counts, BorderLayout.CENTER);
            if (!warnings.isEmpty()) {
                JLabel notice = new JLabel("User conflict");
                notice.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                notice.setForeground(new Color(151, 82, 13));
                metadata.add(notice, BorderLayout.EAST);
                row.setToolTipText(String.join("; ", warnings));
            }
            row.add(metadata, BorderLayout.CENTER);
            int rowWidth = Math.max(190, results.getWidth() - 4);
            row.setPreferredSize(new Dimension(rowWidth,
                    Math.max(60, name.getPreferredSize().height + metadata.getPreferredSize().height + 20)));
            row.getAccessibleContext().setAccessibleName(recipe.title + ", " + recipe.usedCount
                    + " ingredients in pantry, " + recipe.missing.size() + " to buy" + (warnings.isEmpty() ? "" : ", User conflict"));
            row.setSize(row.getPreferredSize());
            row.doLayout(); metadata.doLayout();
            return row;
        });
        results.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { results.setCellRenderer(results.getCellRenderer()); }
        });
        results.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int index = results.locationToIndex(e.getPoint());
                if (index >= 0 && results.getCellBounds(index,index).contains(e.getPoint())) {
                    results.setSelectedIndex(index); openRecipe(results.getSelectedValue());
                }
            }
        });
        results.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "openRecipe");
        results.getActionMap().put("openRecipe", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { openRecipe(results.getSelectedValue()); }
        });
        JScrollPane scroll = new JScrollPane(results);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        browserPage.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        message.setFont(message.getFont().deriveFont(13f));
        bottom.add(message, BorderLayout.NORTH);
        browserPage.add(bottom, BorderLayout.SOUTH);

        JPanel detail = new JPanel(new BorderLayout(10, 12));
        JButton back = new RoundedButton("← Back");
        back.addActionListener(e -> { showingDetail = false; screens.show(content, "results"); });
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backRow.add(back);
        detail.add(backRow, BorderLayout.NORTH);
        JPanel detailBody = AppScreens.form();
        recipeTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        recipeTitle.setForeground(new Color(32, 51, 44));
        detailBody.add(recipeTitle);
        detailBody.add(Box.createVerticalStrut(9));
        detailBody.add(recipeCounts);
        detailBody.add(Box.createVerticalStrut(8));
        recipeNotice.setForeground(new Color(151, 82, 13));
        recipeNotice.setBackground(new Color(255, 248, 230)); recipeNotice.setOpaque(true);
        recipeNotice.setBorder(new EmptyBorder(8, 8, 8, 8));
        recipeNotice.setVisible(false); detailBody.add(recipeNotice);
        detailBody.add(Box.createVerticalStrut(12));
        instructions.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        instructions.setForeground(new Color(32, 51, 44));
        detailBody.add(instructions);
        detailScroll.setViewportView(detailBody);
        detailScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.setBorder(BorderFactory.createEmptyBorder());
        detailScroll.getVerticalScrollBar().setUnitIncrement(20);
        detail.add(detailScroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(2, 1, 0, 8));
        shop.addActionListener(e -> addMissing());
        ingredientsButton.addActionListener(e -> showIngredients());
        actions.add(shop); actions.add(ingredientsButton);
        detail.add(actions, BorderLayout.SOUTH);
        content.add(browserPage, "results");
        content.add(detail, "detail");
        add(content, BorderLayout.CENTER);
        screens.show(content, "results");
        filterButton.addActionListener(e -> chooseFilter());
        queryField.addActionListener(e -> applyFilters());
        queryField.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() { if (!resettingSearch && data != null) applyFilters(); }
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        });
        shop.setEnabled(false); ingredientsButton.setEnabled(false);
    }

    public void setAppData(AppData data) { this.data = data; searchRecipes(false); }
    public void refreshData() { if (data != null) searchRecipes(true); }

    private RecipeService service() {
        return new RecipeService(data == null ? Collections.emptyList() : data.recipes);
    }

    private void addRecipe() {
        if (data == null) return;
        JTextField title = new JTextField();
        JTextField minutes = new JTextField(), servings = new JTextField();
        JTextArea ingredients = new JTextArea(4, 20), steps = new JTextArea(5, 20);
        for (JTextArea area : new JTextArea[]{ingredients, steps}) {
            area.setLineWrap(true); area.setWrapStyleWord(true);
            area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            area.setMargin(new Insets(8, 10, 8, 10));
            area.setBackground(new Color(250, 252, 250));
        }
        JPanel form = AppScreens.form();
        AppScreens.field(form, "Recipe name", title);
        AppScreens.field(form, "Preparation time (minutes)", minutes);
        AppScreens.field(form, "Servings", servings);
        JScrollPane ingredientInput = new JScrollPane(ingredients);
        ingredientInput.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        AppScreens.field(form, "Ingredients (one name per line)", ingredientInput);
        ingredientInput.setPreferredSize(new Dimension(280, 110));
        ingredientInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        form.add(AppScreens.copy("Use names such as Tomato or Rice for pantry matching."));
        form.add(Box.createVerticalStrut(12));
        JScrollPane stepInput = new JScrollPane(steps);
        stepInput.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        AppScreens.field(form, "Cooking steps (include quantities)", stepInput);
        stepInput.setPreferredSize(new Dimension(280, 140));
        stepInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        AppScreens.showForm(this, "Add recipe", form, "Save recipe", () -> {
            String name = title.getText().trim(), directions = steps.getText().trim();
            if (name.isEmpty()) return "Enter a recipe name.";
            int time, portions;
            try { time = Integer.parseInt(minutes.getText().trim()); }
            catch (NumberFormatException error) { return "Enter a whole number of minutes."; }
            if (time <= 0) return "Preparation time must be greater than 0.";
            try { portions = Integer.parseInt(servings.getText().trim()); }
            catch (NumberFormatException error) { return "Enter a whole number of servings."; }
            if (portions <= 0) return "Servings must be greater than 0.";
            LinkedHashMap<String, String> unique = new LinkedHashMap<>();
            for (String line : ingredients.getText().split("\\R")) {
                String ingredient = line.trim();
                if (!ingredient.isEmpty()) unique.putIfAbsent(ingredient.toLowerCase(Locale.ROOT), ingredient);
            }
            if (unique.isEmpty()) return "Add at least one ingredient.";
            if (directions.isEmpty()) return "Add the cooking steps.";
            try {
                for (RecipeService.Recipe recipe : service().findByIngredients(Collections.emptyList()))
                    if (recipe.title.equalsIgnoreCase(name)) return "A recipe with that name already exists.";
            } catch (Exception error) { return "Couldn't save the recipe. Try again."; }
            int id = 1000 + data.recipes.size();
            data.recipes.add(AppData.join(name, Integer.toString(time), Integer.toString(portions),
                    String.join("\n", unique.values()), directions));
            resettingSearch = true;
            queryField.setText("");
            resettingSearch = false;
            maximumMinutes = Integer.MAX_VALUE;
            filterButton.setText("Filter");
            searchRecipes(false);
            for (int i = 0; i < model.size(); i++) if (model.get(i).id == id) {
                results.setSelectedIndex(i); openRecipe(model.get(i)); break;
            }
            return null;
        });
    }

    private void applyFilters() { searchRecipes(false); }

    private void chooseFilter() {
        JComboBox<String> time = new JComboBox<>(new String[]{"Any time", "Up to 15 minutes", "Up to 30 minutes"});
        time.setSelectedIndex(maximumMinutes == 15 ? 1 : maximumMinutes == 30 ? 2 : 0);
        JPanel form = AppScreens.form();
        AppScreens.field(form, "Preparation time", time);
        AppScreens.showForm(this, "Filter recipes", form, "Apply filter", () -> {
            maximumMinutes = time.getSelectedIndex() == 1 ? 15 : time.getSelectedIndex() == 2 ? 30 : Integer.MAX_VALUE;
            filterButton.setText(maximumMinutes == Integer.MAX_VALUE ? "Filter" : "≤ " + maximumMinutes + " min");
            applyFilters();
            return null;
        });
    }

    private List<String> recipeWarnings(RecipeService.Recipe recipe) {
        Set<String> warnings = new LinkedHashSet<>();
        if (data == null) return new ArrayList<>();
        List<String> ingredients = new ArrayList<>(recipe.used); ingredients.addAll(recipe.missing);
        for (String ingredient : ingredients) warnings.addAll(FoodAlerts.warningsFor(ingredient, data.profiles));
        return new ArrayList<>(warnings);
    }
    private void updateRecipeNotice(RecipeService.Recipe recipe) {
        List<String> warnings = recipeWarnings(recipe);
        recipeNotice.setBackground(new Color(255, 248, 230));
        recipeNotice.setForeground(new Color(151, 82, 13));
        recipeNotice.setText(warnings.isEmpty() ? "" : "User conflict");
        recipeNotice.setVisible(!warnings.isEmpty());
    }

    private void updateCounts() {
        updateRecipeNotice(current);
        recipeCounts.setText("<html><span style='color:#48745c'>In pantry: " + current.usedCount + "</span>"
                + "&nbsp;&nbsp; <span style='color:#b33333'>To buy: " + current.missing.size() + "</span></html>");
        recipeTitle.getParent().invalidate();
    }

    private List<String> availableIngredients() {
        List<String> names = new ArrayList<>();
        if (data != null) for (String item : data.foods) {
            String[] fields = Food.parts(item);
            if (!Food.displayStatus(fields[2]).equals("Empty") && !fields[0].trim().isEmpty()
                    && names.stream().noneMatch(x -> x.equalsIgnoreCase(fields[0]))) names.add(fields[0].trim());
        }
        return names;
    }

    /** Small local catalogue: synchronous loading avoids stale callbacks and unnecessary caches. */
    private void searchRecipes(boolean preserveDetail) {
        if (data == null) return;
        int previousId = current == null ? -1 : current.id;
        boolean keepDetail = preserveDetail && showingDetail;
        RecipeService service = service();
        String query = queryField.getText().trim().toLowerCase(Locale.ROOT);
        try {
            List<RecipeService.Recipe> all = service.findByIngredients(availableIngredients());
            List<RecipeService.Recipe> filtered = new ArrayList<>();
            for (RecipeService.Recipe recipe : all) {
                RecipeService.RecipeDetails details = service.getDetails(recipe.id);
                if (details.minutes <= maximumMinutes && (recipe.title.toLowerCase(Locale.ROOT).contains(query)
                        || details.ingredients.stream().anyMatch(name -> name.toLowerCase(Locale.ROOT).contains(query))))
                    filtered.add(recipe);
            }
            filtered.sort(Comparator.comparingInt((RecipeService.Recipe recipe) -> recipe.missing.size())
                    .thenComparing(Comparator.comparingInt((RecipeService.Recipe recipe) -> recipe.usedCount).reversed())
                    .thenComparing(recipe -> recipe.title.toLowerCase(Locale.ROOT)));
            model.clear();
            for (RecipeService.Recipe recipe : filtered) model.addElement(recipe);
            results.setCellRenderer(results.getCellRenderer());
            message.setText(filtered.isEmpty() ? "No matches. Try another name or filter."
                    : filtered.size() + " recipes · tap one for ingredients and steps.");
            if (keepDetail) {
                for (RecipeService.Recipe recipe : all) if (recipe.id == previousId) { openRecipe(recipe); return; }
            }
            showingDetail = false; current = null; currentDetails = null;
            screens.show(content, "results");
        } catch (IOException invalidData) {
            message.setText("Couldn't load recipes. Try another search.");
        }
    }

    private void openRecipe(RecipeService.Recipe recipe) {
        if (recipe == null) return;
        try {
            RecipeService.RecipeDetails details = service().getDetails(recipe.id);
            current = recipe; showingDetail = true;
            recipeTitle.setText(recipe.title);
            updateCounts();
            displayRecipe(details);
            shop.setEnabled(!recipe.missing.isEmpty()); ingredientsButton.setEnabled(true);
            results.setSelectedValue(recipe, true);
            screens.show(content, "detail");
            detailScroll.getVerticalScrollBar().setValue(0);
        } catch (IOException invalidData) {
            AppScreens.message(this, "Recipe unavailable", "Return to the list and choose another recipe.");
        }
    }

    private void displayRecipe(RecipeService.RecipeDetails full) {
        currentDetails = full;
        String text = full.instructions == null ? "" : full.instructions.trim();
        List<String> conflicts = current == null ? Collections.emptyList() : recipeWarnings(current);
        instructions.setText("Ready in " + full.minutes + " min     •     Serves " + full.servings
                + (conflicts.isEmpty() ? "" : "\n\n" + String.join("\n", conflicts))
                + "\n\n" + (text.isEmpty() ? "No instructions are bundled for this recipe." : text)
                + "\n\nNote: Pantry matches use names. Check amounts and ingredient labels.");
        instructions.setCaretPosition(0);
    }

    private void addMissing() {
        if (current == null || data == null) return;
        int added = 0;
        for (String raw : current.missing) {
            String name = raw.trim();
            if (name.isEmpty()) continue;
            if (data.shoppingItems.stream().noneMatch(x -> Food.shoppingName(x).equalsIgnoreCase(name))) {
                data.shoppingItems.add(AppData.encode(name)); added++;
            }
        }
        AppScreens.message(this, "Shopping updated", added == 0 ? "These ingredients are already on your list."
                : added + (added == 1 ? " ingredient added to Shopping." : " ingredients added to Shopping."));
    }
    private void showIngredients() {
        if (currentDetails == null) {
            AppScreens.message(this, "Ingredients", "Open a recipe to view its ingredients.");
            return;
        }
        JPanel body = AppScreens.form();
        for (String name : currentDetails.ingredients) {
            body.add(AppScreens.copy(name + (current.used.contains(name) ? " — in pantry" : " — to buy")));
            body.add(Box.createVerticalStrut(10));
        }
        body.add(AppScreens.copy("Note: Check that you have enough. Quantities are listed in the cooking steps."));
        AppScreens.showForm(this, "Recipe ingredients", body, "Back to recipe", () -> null);
    }
}
