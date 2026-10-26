package kitchen;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;

// Member: ?
public class Profiles extends JPanel {
    private static final long serialVersionUID = 1L;
    private AppData data;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    public Profiles() {
        // Add profile selection and allergy/dietary restriction editing.
    }

    public void setAppData(AppData data) {
        this.data = data;
        refreshData();
    }

    // Refresh shared data when returning to this tab.
    public void refreshData() {
        listModel.clear();
        if (data != null) {
            for (String item : data.profiles) {
                listModel.addElement(item);
            }
        }
    }
}
