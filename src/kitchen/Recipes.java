package kitchen;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;

// Member: ?
//public class Recipes extends JPanel {
//    private static final long serialVersionUID = 1L;
//    private AppData data;
//    private final DefaultListModel<String> listModel = new DefaultListModel<>();

//    public Recipes() {
        // Add recipe details and send missing ingredients to Shopping.
//    }

//    public void setAppData(AppData data) {
//        this.data = data;
//        refreshData();
//    }

    // Refresh shared data when returning to this tab.
//    public void refreshData() {
//        listModel.clear();
//        if (data != null) {
//            for (String item : data.recipes) {
//                listModel.addElement(item);
//            }
//        }
//    }
//}



import java.awt.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Live recipe suggestions using non-empty items in the Food tab. */
public class Recipes extends JPanel {
    private static final long serialVersionUID=1L;
    private AppData data;
    private final DefaultListModel<RecipeService.Recipe> model=new DefaultListModel<>();
    private final JList<RecipeService.Recipe> results=new JList<>(model);
    private final JTextArea details=new JTextArea();
    private final JLabel pantryLabel=new JLabel("Add food in the Food tab to begin.");
    private final JTextField keyField=new JTextField(20);
    private final JButton find=new JButton("Find Recipes");
    private final JButton show=new JButton("View Instructions");
    private final JButton shop=new JButton("Add Missing to Shopping");
    private final JButton browser=new JButton("Open Recipe Website");
    private RecipeService.RecipeDetails displayedDetails;

    public Recipes(){
        setLayout(new BorderLayout(10,10));
        setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel top=new JPanel(new BorderLayout(5,5));
        JPanel auth=new JPanel(new BorderLayout(5,5));
        auth.add(new JLabel("Spoonacular API key:"),BorderLayout.WEST);
        auth.add(keyField,BorderLayout.CENTER);
        top.add(auth,BorderLayout.NORTH);
        top.add(pantryLabel,BorderLayout.CENTER);
        top.add(find,BorderLayout.SOUTH);
        add(top,BorderLayout.NORTH);

        details.setEditable(false);details.setLineWrap(true);details.setWrapStyleWord(true);
        details.setText("Select a recipe to see which ingredients you have and which are missing.");
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(results),new JScrollPane(details));
        split.setResizeWeight(0.45);
        add(split,BorderLayout.CENTER);
        JPanel actions=new JPanel(new GridLayout(3,1,5,5));
        actions.add(show);actions.add(shop);actions.add(browser);
        add(actions,BorderLayout.SOUTH);
        find.addActionListener(e->searchRecipes());
        show.addActionListener(e->loadDetails());
        shop.addActionListener(e->addMissing());
        browser.addActionListener(e->openWebsite());
        results.addListSelectionListener(e->{if(!e.getValueIsAdjusting())showSelected();});
    }
    public void setAppData(AppData data){this.data=data;refreshData();}
    public void refreshData(){
        List<String> foods=availableIngredients();
        pantryLabel.setText("Available ingredients ("+foods.size()+"): "+(foods.isEmpty()?"None":String.join(", ",foods)));
        pantryLabel.setToolTipText(String.join(", ",foods));
    }
    private List<String> availableIngredients(){
        List<String> names=new ArrayList<>();
        if(data!=null)for(String item:data.foods){
            String[] p=Food.parts(item);
            if(!p[2].equalsIgnoreCase("Empty") && !p[0].trim().isEmpty() &&
                    names.stream().noneMatch(n->n.equalsIgnoreCase(p[0].trim())))names.add(p[0].trim());
        }
        return names;
    }
    private void searchRecipes(){
        List<String> ingredients=availableIngredients();
        if(ingredients.isEmpty()){JOptionPane.showMessageDialog(this,"Add available food in the Food tab first.");return;}
        if(keyField.getText().trim().isEmpty()){JOptionPane.showMessageDialog(this,"Enter your Spoonacular API key.");return;}
        find.setEnabled(false);find.setText("Searching...");model.clear();details.setText("Searching recipes...");displayedDetails=null;
        RecipeService service=new RecipeService(keyField.getText());
        new SwingWorker<List<RecipeService.Recipe>,Void>(){
            @Override protected List<RecipeService.Recipe> doInBackground() throws Exception{return service.findByIngredients(ingredients);}
            @Override protected void done(){
                find.setEnabled(true);find.setText("Find Recipes");
                try{
                    List<RecipeService.Recipe> found=get();
                    for(RecipeService.Recipe recipe:found)model.addElement(recipe);
                    details.setText(found.isEmpty()?"No recipes found. Try adding different ingredients.":
                        "Found "+found.size()+" recipes. Select one to see ingredient matches.");
                }catch(Exception ex){details.setText("Recipe search failed: "+errorMessage(ex));}
            }
        }.execute();
    }
    private static String errorMessage(Exception ex){
        Throwable t=ex;
        while(t.getCause()!=null)t=t.getCause();
        return t.getMessage()==null?t.toString():t.getMessage();
    }
    private void showSelected(){
        displayedDetails=null;
        RecipeService.Recipe r=results.getSelectedValue();if(r==null)return;
        details.setText(r.title+"\n\nAvailable:\n  "+String.join(", ",r.used)+
            "\n\nMissing:\n  "+(r.missing.isEmpty()?"Nothing reported":String.join(", ",r.missing))+
            "\n\nClick View Instructions for full details.");
        details.setCaretPosition(0);
    }
    private void loadDetails(){
        RecipeService.Recipe r=results.getSelectedValue();
        if(r==null){JOptionPane.showMessageDialog(this,"Select a recipe first.");return;}
        show.setEnabled(false);details.setText("Loading recipe details...");
        new SwingWorker<RecipeService.RecipeDetails,Void>(){
            @Override protected RecipeService.RecipeDetails doInBackground()throws Exception{
                return new RecipeService(keyField.getText()).getDetails(r.id);
            }
            @Override protected void done(){
                show.setEnabled(true);
                try{
                    RecipeService.RecipeDetails d=get();displayedDetails=d;
                    details.setText(d.title+"\nReady in: "+d.minutes+" min | Servings: "+d.servings+
                        "\n\nIngredients:\n"+String.join("\n",d.ingredients)+
                        "\n\nInstructions:\n"+(d.instructions.isEmpty()?"See recipe website for instructions.":
                            d.instructions.replaceAll("<[^>]*>"," ").replace("&nbsp;"," ").replace("&amp;","&"))+
                        "\n\nSource: "+d.sourceUrl);
                    details.setCaretPosition(0);
                }catch(Exception ex){details.setText("Could not load recipe: "+errorMessage(ex));}
            }
        }.execute();
    }
    private void addMissing(){
        RecipeService.Recipe r=results.getSelectedValue();
        if(r==null || data==null){JOptionPane.showMessageDialog(this,"Select a recipe first.");return;}
        int added=0;
        for(String raw:r.missing){
            String name=raw.trim();
            if(name.isEmpty() || name.contains("|"))continue;
            boolean exists=data.shoppingItems.stream().anyMatch(s->Food.shoppingName(s).equalsIgnoreCase(name));
            if(!exists){data.shoppingItems.add(name);added++;}
        }
        JOptionPane.showMessageDialog(this,added+" missing ingredient(s) added to Shopping.");
    }
    private void openWebsite(){
        RecipeService.Recipe r=results.getSelectedValue();
        if(r==null){JOptionPane.showMessageDialog(this,"Select a recipe first.");return;}
        String url=displayedDetails!=null?displayedDetails.sourceUrl:"";
        if(url.isEmpty())url="https://spoonacular.com/recipes/"+r.title.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","")+"-"+r.id;
        try{
            URI uri=URI.create(url);
            if(!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                throw new IllegalArgumentException("Unsupported URL");
            Desktop.getDesktop().browse(uri);
        }catch(Exception ex){JOptionPane.showMessageDialog(this,"Cannot open recipe URL: "+ex.getMessage());}
    }
}
