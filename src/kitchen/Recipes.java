package kitchen;

import java.awt.*;
import java.awt.event.*;
import java.net.URI;
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
    private final JList<RecipeService.Recipe> results = new JList<>(model);
    private final JLabel message = new JLabel("Find meals using your food at home.", SwingConstants.CENTER);
    private final JButton find = new RoundedButton("Find Recipes");
    private final JLabel recipeTitle = new JLabel();
    private final JLabel recipeCounts = new JLabel();
    private final JTextArea instructions = new JTextArea();
    private final JButton shop = new RoundedButton("Add Missing to Shopping");
    private final JButton browser = new RoundedButton("Open Recipe Website");
    private final Map<Integer, RecipeService.RecipeDetails> cache = new HashMap<>();
    private RecipeService.Recipe current;
    private RecipeService.RecipeDetails currentDetails;
    private int requestSerial = 0;

    public Recipes() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel browserPage = new JPanel(new BorderLayout(10, 10));
        results.setFixedCellHeight(-1);
        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        results.setCellRenderer((list, recipe, index, selected, focused) -> {
            JPanel row = new JPanel(new BorderLayout(6, 4));
            row.setOpaque(true);
            row.setBorder(new EmptyBorder(12, 13, 12, 13));
            row.setBackground(selected ? new Color(223, 242, 229) : index % 2 == 0 ? Color.WHITE : new Color(248, 250, 248));
            JTextArea name = new JTextArea(recipe.title);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            name.setEditable(false); name.setLineWrap(true); name.setWrapStyleWord(true);
            name.setOpaque(false); name.setFocusable(false);
            int textWidth = Math.max(110, results.getWidth() - 45);
            name.setSize(textWidth, 10000);
            row.add(name, BorderLayout.NORTH);
            JLabel counts = new JLabel("<html><span style='color:#48745c'>Have " + recipe.usedCount + "</span>"
                    + "&nbsp;&nbsp; <span style='color:#b33333'>Missing " + recipe.missing.size() + "</span></html>");
            row.add(counts, BorderLayout.CENTER);
            row.setPreferredSize(new Dimension(Math.max(190, results.getWidth() - 4),
                    Math.max(64, name.getPreferredSize().height + 42)));
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
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        browserPage.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        message.setFont(message.getFont().deriveFont(12f));
        bottom.add(message, BorderLayout.NORTH);
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        find.setPreferredSize(new Dimension(165, 43));
        center.add(find);
        bottom.add(center, BorderLayout.SOUTH);
        browserPage.add(bottom, BorderLayout.SOUTH);

        JPanel detail = new JPanel(new BorderLayout(10, 12));
        JButton back = new RoundedButton("← Back");
        back.addActionListener(e -> { ++requestSerial; screens.show(content, "results"); });
        JPanel top = new JPanel(new BorderLayout(0, 12));
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backRow.add(back);
        top.add(backRow, BorderLayout.NORTH);
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        recipeTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        recipeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        recipeCounts.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(recipeTitle);
        header.add(Box.createVerticalStrut(9));
        header.add(recipeCounts);
        top.add(header, BorderLayout.CENTER);
        detail.add(top, BorderLayout.NORTH);
        instructions.setEditable(false);
        instructions.setLineWrap(true); instructions.setWrapStyleWord(true);
        instructions.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        instructions.setMargin(new Insets(14, 14, 14, 14));
        detail.add(new JScrollPane(instructions), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(2, 1, 0, 8));
        shop.addActionListener(e -> addMissing());
        browser.addActionListener(e -> openWebsite());
        actions.add(shop); actions.add(browser);
        detail.add(actions, BorderLayout.SOUTH);
        content.add(browserPage, "results");
        content.add(detail, "detail");
        add(content, BorderLayout.CENTER);
        screens.show(content, "results");
        find.addActionListener(e -> searchRecipes());
        shop.setEnabled(false); browser.setEnabled(false);
    }

    public void setAppData(AppData data) { this.data = data; refreshData(); }
    public void refreshData() { /* Results are refreshed only when Find Recipes is pressed. */ }

    private List<String> availableIngredients() {
        List<String> names = new ArrayList<>();
        if (data != null) for (String item : data.foods) {
            String[] fields = Food.parts(item);
            if (!Food.displayStatus(fields[2]).equals("Empty") && !fields[0].trim().isEmpty()
                    && names.stream().noneMatch(x -> x.equalsIgnoreCase(fields[0]))) names.add(fields[0].trim());
        }
        return names;
    }

    private void searchRecipes() {
        List<String> ingredients = availableIngredients();
        if (ingredients.isEmpty()) { message.setText("Add food before searching."); return; }
        if (!RecipeConfig.isConfigured()) {
            message.setText("Recipe service is not configured. Ask the app developer.");
            return;
        }
        ++requestSerial;
        model.clear(); cache.clear(); current = null; currentDetails = null;
        find.setEnabled(false); find.setText("Searching...");
        message.setText("Searching...");
        RecipeService service = new RecipeService(RecipeConfig.apiKey());
        new SwingWorker<List<RecipeService.Recipe>, Void>() {
            @Override protected List<RecipeService.Recipe> doInBackground() throws Exception {
                return service.findByIngredients(ingredients);
            }
            @Override protected void done() {
                find.setEnabled(true); find.setText("Find Recipes");
                try {
                    List<RecipeService.Recipe> found = get();
                    found.sort(Comparator.comparingInt((RecipeService.Recipe r) -> r.missing.size())
                            .thenComparing(Comparator.comparingInt((RecipeService.Recipe r) -> r.usedCount).reversed())
                            .thenComparing(r -> r.title.toLowerCase(Locale.ROOT)));
                    for (RecipeService.Recipe recipe : found) model.addElement(recipe);
                    message.setText(found.isEmpty() ? "No recipes found. Try different foods." : "Tap a recipe to view it.");
                } catch (Exception e) { message.setText("Search failed: " + errorMessage(e)); }
            }
        }.execute();
    }

    private void openRecipe(RecipeService.Recipe recipe) {
        if (recipe == null) return;
        int serial = ++requestSerial;
        current = recipe; currentDetails = null;
        recipeTitle.setText("<html><body style='width:230px'>" + html(recipe.title) + "</body></html>");
        recipeCounts.setText("<html><span style='color:#48745c'>Have " + recipe.usedCount + "</span>"
                + "&nbsp;&nbsp; <span style='color:#b33333'>Missing " + recipe.missing.size() + "</span></html>");
        instructions.setText("Loading instructions...");
        shop.setEnabled(true); browser.setEnabled(true);
        screens.show(content, "detail");
        RecipeService.RecipeDetails cached = cache.get(recipe.id);
        if (cached != null) { displayRecipe(cached); return; }
        new SwingWorker<RecipeService.RecipeDetails, Void>() {
            @Override protected RecipeService.RecipeDetails doInBackground() throws Exception {
                return new RecipeService(RecipeConfig.apiKey()).getDetails(recipe.id);
            }
            @Override protected void done() {
                if (requestSerial != serial || current != recipe) return;
                try {
                    RecipeService.RecipeDetails full = get(); cache.put(recipe.id, full); displayRecipe(full);
                } catch (Exception e) { instructions.setText("Instructions unavailable: " + errorMessage(e)
                        + "\n\nYou can still open the recipe website."); }
            }
        }.execute();
    }

    private void displayRecipe(RecipeService.RecipeDetails full) {
        currentDetails = full;
        String text = full.instructions == null ? "" : full.instructions
                .replaceAll("<[^>]*>", " ").replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&quot;", "\"").trim();
        instructions.setText("Ready in " + full.minutes + " min     •     Serves " + full.servings
                + "\n\n" + (text.isEmpty() ? "Visit the recipe website for instructions." : text)
                + "\n\nNote: Recipes are not screened for household allergies. Verify the ingredients before cooking.");
        instructions.setCaretPosition(0);
    }

    private static String html(String input) { return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private static String errorMessage(Exception error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }
    private void addMissing() {
        if (current == null || data == null) return;
        int added = 0;
        for (String raw : current.missing) {
            String name = raw.trim();
            if (name.isEmpty() || name.contains("|")) continue;
            if (data.shoppingItems.stream().noneMatch(x -> Food.shoppingName(x).equalsIgnoreCase(name))) {
                data.shoppingItems.add(name); added++;
            }
        }
        JOptionPane.showMessageDialog(this, added + " item(s) added to Shopping.");
    }
    private void openWebsite() {
        if (current == null) return;
        String url = currentDetails != null ? currentDetails.sourceUrl : null;
        if (url == null || url.isEmpty()) {
            url = "https://spoonacular.com/recipes/" + current.title.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") + "-" + current.id;
        }
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                throw new IllegalArgumentException("Unsupported website URL");
            Desktop.getDesktop().browse(uri);
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Couldn't open website: " + e.getMessage()); }
    }
}
