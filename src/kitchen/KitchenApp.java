package kitchen;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

// Shared setup/navigation)
public class KitchenApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppData data = new AppData();
            Food food = new Food();
            Shopping shopping = new Shopping();
            Recipes recipes = new Recipes();
            Profiles profiles = new Profiles();

            food.setAppData(data);
            shopping.setAppData(data);
            recipes.setAppData(data);
            profiles.setAppData(data);

            JTabbedPane tabs = new JTabbedPane(JTabbedPane.BOTTOM);
            tabs.addTab("Food", food);
            tabs.addTab("Shopping", shopping);
            tabs.addTab("Recipes", recipes);
            tabs.addTab("Profiles", profiles);
            tabs.addChangeListener(event -> {
                switch (tabs.getSelectedIndex()) {
                    case 0: food.refreshData(); break;
                    case 1: shopping.refreshData(); break;
                    case 2: recipes.refreshData(); break;
                    case 3: profiles.refreshData(); break;
                    default: break;
                }
            });

            JFrame frame = new JFrame("Kitchen App");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(tabs);
            frame.setSize(640, 960);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
