package com.curator.ui.panels;

import com.curator.domain.GameMode;
import javafx.scene.Node;

public interface MenuNavigator {
    void showHomePanel();
    void showNewGamePanel();
    void showControlsPanel();
    void showSettingsPanel();
    void showAboutPanel();
    void showLibraryPanel();
    
    void requestNewGame();
    void requestAuthentication(Runnable onSuccess);
    
    void swapContent(Node node);
    void setPanelTitle(String title);
    
    GameMode getSelectedMode();
    void setSelectedMode(GameMode mode);
}
