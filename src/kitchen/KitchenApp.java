package kitchen;

import java.awt.*;
import javax.swing.*;

public class KitchenApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getCrossPlatformLookAndFeelClassName()
                );
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            Font font = new Font("Arial", Font.PLAIN, 18);

            for (String key : new String[] {
                "Button.font", "Label.font", "List.font",
                "TextField.font", "ComboBox.font",
                "TabbedPane.font", "OptionPane.messageFont",
                "OptionPane.buttonFont"
            }) {
                UIManager.put(key, font);
            }

            // All tabs use the same data.
            AppData data = new AppData();

            Food food = new Food();
            Shopping shopping = new Shopping();
            Recipes recipes = new Recipes();
            Profiles profiles = new Profiles();

            food.setAppData(data);
            shopping.setAppData(data);
            recipes.setAppData(data);
            profiles.setAppData(data);

            JTabbedPane tabs = new JTabbedPane();

            tabs.addTab("Food", food);
            tabs.addTab("Shopping", shopping);
            tabs.addTab("Recipes", recipes);
            tabs.addTab("Profiles", profiles);
            tabs.addTab("Settings", new JPanel());

            // Refresh data when switching tabs.
            tabs.addChangeListener(event -> {
                food.refreshData();
                shopping.refreshData();
                recipes.refreshData();
                profiles.refreshData();
            });

            tabs.setPreferredSize(new Dimension(640, 800));

            JFrame frame = new JFrame("Kitchen Kompanion");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(tabs);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}