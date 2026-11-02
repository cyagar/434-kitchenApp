package kitchen;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/** Real, locally saved display and interaction preferences. */
public final class AppSettings {
    private final Preferences prefs = Preferences.userNodeForPackage(AppSettings.class);
    private final List<Runnable> listeners = new ArrayList<>();

    public boolean alphabetizeFood() { return prefs.getBoolean("alphabetizeFood", false); }
    public boolean showFoodDetails() { return prefs.getBoolean("showFoodDetails", true); }
    public boolean compactFoodRows() { return prefs.getBoolean("compactFoodRows", false); }
    public boolean largerListText() { return prefs.getBoolean("largerListText", false); }
    public boolean confirmStockChanges() { return prefs.getBoolean("confirmStockChanges", true); }

    public void setAlphabetizeFood(boolean value) { prefs.putBoolean("alphabetizeFood", value); notifyChanged(); }
    public void setShowFoodDetails(boolean value) { prefs.putBoolean("showFoodDetails", value); notifyChanged(); }
    public void setCompactFoodRows(boolean value) { prefs.putBoolean("compactFoodRows", value); notifyChanged(); }
    public void setLargerListText(boolean value) { prefs.putBoolean("largerListText", value); notifyChanged(); }
    public void setConfirmStockChanges(boolean value) { prefs.putBoolean("confirmStockChanges", value); notifyChanged(); }

    public void onChange(Runnable listener) { listeners.add(listener); }

    public void reset() {
        setAlphabetizeFood(false);
        setShowFoodDetails(true);
        setCompactFoodRows(false);
        setLargerListText(false);
        setConfirmStockChanges(true);
    }

    private void notifyChanged() {
        for (Runnable listener : new ArrayList<>(listeners)) listener.run();
    }
}
