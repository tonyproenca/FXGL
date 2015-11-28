/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.almasb.fxgl.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.almasb.fxgl.GameApplication;
import com.almasb.fxgl.asset.SaveLoadManager;
import com.almasb.fxgl.event.InputBinding;
import com.almasb.fxgl.event.MenuEvent;
import com.almasb.fxgl.gameplay.Achievement;
import com.almasb.fxgl.settings.SceneSettings;
import com.almasb.fxgl.util.FXGLLogger;
import com.almasb.fxgl.util.Version;

import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * This is a base class for main/game menus. It provides several
 * convenience methods for those who just want to extend an existing menu.
 * It also allows for implementors to build menus from scratch. Freshly
 * build menus can interact with FXGL by calling fire* methods.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public abstract class FXGLMenu extends FXGLScene {

    /**
     * The logger
     */
    protected static final Logger log = FXGLLogger.getLogger("FXGLMenu");

    protected final GameApplication app;

    private List<String> credits = new ArrayList<>();

    public FXGLMenu(GameApplication app, SceneSettings settings) {
        super(settings);
        this.app = app;

        populateCredits();
    }

    private void populateCredits() {
        addCredit("Powered by FXGL " + Version.getAsString());
        addCredit("Graphics Framework: JavaFX " + Version.getJavaFXAsString());
        addCredit("Physics Engine: JBox2d (jbox2d.org) " + Version.getJBox2DAsString());
        addCredit("FXGL Author: Almas Baimagambetov (AlmasB)");
        addCredit("https://github.com/AlmasB/FXGL");
    }

    /**
     *
     * @return menu content containing list of save files and load/delete buttons
     */
    protected final MenuContent createContentLoad() {
        ListView<String> list = new ListView<>();
        SaveLoadManager.INSTANCE.loadFileNames().ifPresent(names -> list.getItems().setAll(names));
        list.prefHeightProperty().bind(Bindings.size(list.getItems()).multiply(36));

        if (list.getItems().size() > 0) {
            list.getSelectionModel().selectFirst();
        }

        Button btnLoad = UIFactory.newButton("LOAD");
        btnLoad.setOnAction(e -> {
            String fileName = list.getSelectionModel().getSelectedItem();
            if (fileName == null)
                return;

            fireLoad(fileName);
        });
        Button btnDelete = UIFactory.newButton("DELETE");
        btnDelete.setOnAction(e -> {
            String fileName = list.getSelectionModel().getSelectedItem();
            if (fileName == null)
                return;

            UIFactory.getDialogBox().showMessageBox(SaveLoadManager.INSTANCE.delete(fileName)
                    ? "File was deleted" : "File couldn't be deleted");

            list.getItems().remove(fileName);
        });

        HBox hbox = new HBox(50, btnLoad, btnDelete);
        hbox.setAlignment(Pos.CENTER);

        return new MenuContent(list, hbox);
    }

    /**
     *
     * @return menu content containing input mappings (action -> key/mouse)
     */
    protected final MenuContent createContentControls() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(50);
        grid.setUserData(0);

        // add listener for new ones
        app.getInputManager().getBindings().addListener((ListChangeListener.Change<? extends InputBinding> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(binding -> addNewInputBinding(binding, grid));
                }
            }
        });

        // register current ones
        app.getInputManager().getBindings().forEach(binding -> addNewInputBinding(binding, grid));

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setVbarPolicy(ScrollBarPolicy.ALWAYS);
        scroll.setMaxHeight(app.getHeight() / 2);
        scroll.setStyle("-fx-background: black;");

        HBox hbox = new HBox(scroll);
        hbox.setAlignment(Pos.CENTER);

        return new MenuContent(hbox);
    }

    private void addNewInputBinding(InputBinding binding, GridPane grid) {
        Text actionName = UIFactory.newText(binding.getAction().getName());

        Button triggerName = UIFactory.newButton("");
        triggerName.textProperty().bind(binding.triggerNameProperty());
        triggerName.setOnMouseClicked(event -> {
            Rectangle rect = new Rectangle(250, 100);
            rect.setStroke(Color.AZURE);

            Text text = UIFactory.newText("PRESS ANY KEY", 24);

            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getRoot().getScene().getWindow());

            Scene scene = new Scene(new StackPane(rect, text));
            scene.setOnKeyPressed(e -> {
                app.getInputManager().rebind(binding.getAction(), e.getCode());
                stage.close();
            });
            scene.setOnMouseClicked(e -> {
                app.getInputManager().rebind(binding.getAction(), e.getButton());
                stage.close();
            });

            stage.setScene(scene);
            stage.show();
        });

        int controlsRow = (int) grid.getUserData();
        grid.addRow(controlsRow++, actionName, triggerName);
        grid.setUserData(controlsRow);

        GridPane.setHalignment(actionName, HPos.RIGHT);
        GridPane.setHalignment(triggerName, HPos.LEFT);
    }

    /**
     *
     * @return menu content with video settings
     */
    protected final MenuContent createContentVideo() {
        Spinner<SceneSettings.SceneDimension> spinner =
                new Spinner<>(FXCollections.observableArrayList(app.getSceneManager().getSceneDimensions()));

        Button btnApply = UIFactory.newButton("Apply");
        btnApply.setOnAction(e -> {
            SceneSettings.SceneDimension dimension = spinner.getValue();
            app.getSceneManager().setSceneDimension(dimension);
        });

        return new MenuContent(new HBox(50, UIFactory.newText("Resolution"), spinner), btnApply);
    }

    /**
     *
     * @return menu content containing music and sound volume sliders
     */
    protected final MenuContent createContentAudio() {
        Slider sliderMusic = new Slider(0, 1, 1);
        sliderMusic.valueProperty().bindBidirectional(app.getAudioManager().globalMusicVolumeProperty());
        //app.getAudioManager().globalMusicVolumeProperty().bindBidirectional(sliderMusic.valueProperty());

        Text textMusic = UIFactory.newText("Music Volume: ");
        Text percentMusic = UIFactory.newText("");
        percentMusic.textProperty().bind(sliderMusic.valueProperty().multiply(100).asString("%.0f"));

        Slider sliderSound = new Slider(0, 1, 1);
        sliderSound.valueProperty().bindBidirectional(app.getAudioManager().globalSoundVolumeProperty());
        //app.getAudioManager().globalSoundVolumeProperty().bindBidirectional(sliderSound.valueProperty());

        Text textSound = UIFactory.newText("Sound Volume: ");
        Text percentSound = UIFactory.newText("");
        percentSound.textProperty().bind(sliderSound.valueProperty().multiply(100).asString("%.0f"));

        return new MenuContent(new HBox(textMusic, sliderMusic, percentMusic),
                new HBox(textSound, sliderSound, percentSound));
    }

    /**
     * Add a single line of credit text.
     *
     * @param text the text to append to credits list
     */
    protected final void addCredit(String text) {
        credits.add(text);
    }

    /**
     *
     * @return menu content containing a list of credits
     */
    protected final MenuContent createContentCredits() {
        return new MenuContent(credits.stream()
                .map(UIFactory::newText)
                .collect(Collectors.toList())
                .toArray(new Text[0]));
    }

    /**
     *
     * @return menu content containing a list of achievements
     */
    protected final MenuContent createContentAchievements() {
        MenuContent content = new MenuContent();

        for (Achievement a : app.getAchievementManager().getAchievements()) {
            CheckBox checkBox = new CheckBox();
            checkBox.setDisable(true);
            checkBox.selectedProperty().bind(a.achievedProperty());

            Text text = UIFactory.newText(a.getName());
            Tooltip.install(text, new Tooltip(a.getDescription()));

            HBox box = new HBox(50, text, checkBox);

            content.getChildren().add(box);
        }

        return content;
    }

    /**
     * A generic vertical box container for menu content
     * where each element is followed by a separator
     */
    protected class MenuContent extends VBox {
        public MenuContent(Node... items) {
            int maxW = Arrays.asList(items)
                    .stream()
                    .mapToInt(n -> (int)n.getLayoutBounds().getWidth())
                    .max()
                    .orElse(0);

            getChildren().add(createSeparator(maxW));

            for (Node item : items) {
                getChildren().addAll(item, createSeparator(maxW));
            }
        }

        private Line createSeparator(int width) {
            Line sep = new Line();
            sep.setEndX(width);
            sep.setStroke(Color.DARKGREY);
            return sep;
        }
    }

    /**
     * Adds a UI node.
     *
     * @param node the node to add
     */
    protected final void addUINode(Node node) {
        getRoot().getChildren().add(node);
    }

    /**
     * Fires {@link MenuEvent#NEW_GAME} event.
     * Can only be fired from main menu.
     * Starts new game.
     */
    protected final void fireNewGame() {
        fireEvent(new MenuEvent(MenuEvent.NEW_GAME));
    }

    /**
     * Fires {@link MenuEvent#LOAD} event.
     * Lads the game state from last modified save file.
     */
    protected final void fireContinue() {
        fireEvent(new MenuEvent(MenuEvent.LOAD));
    }

    /**
     * Fires {@link MenuEvent#LOAD} event.
     * Loads the game state from previously saved file.
     *
     * @param fileName  name of the saved file
     */
    protected final void fireLoad(String fileName) {
        fireEvent(new MenuEvent(MenuEvent.LOAD, fileName));
    }

    /**
     * Fires {@link MenuEvent#SAVE} event.
     * Can only be fired from game menu. Saves current state of the game with given file name.
     *
     * @param fileName  name of the save file
     */
    protected final void fireSave(String fileName) {
        fireEvent(new MenuEvent(MenuEvent.SAVE, fileName));
    }

    /**
     * Fires {@link MenuEvent#RESUME} event.
     * Can only be fired from game menu. Will close the menu and unpause the game.
     */
    protected final void fireResume() {
        fireEvent(new MenuEvent(MenuEvent.RESUME));
    }

    /**
     * Fire {@link MenuEvent#EXIT} event.
     * If fired from game menu, app will clean up and enter main menu.
     * If fired from main menu, app will close.
     */
    protected final void fireExit() {
        fireEvent(new MenuEvent(MenuEvent.EXIT));
    }
}