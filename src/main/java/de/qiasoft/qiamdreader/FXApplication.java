package de.qiasoft.qiamdreader;

import java.io.File;
import java.io.IOException;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class FXApplication extends Application {
    private static Stage mainStage;
    private static final ObservableSet<String> mainFileAbsPaths = FXCollections.observableSet();

    public static void updateFileQueue(ConcurrentLinkedQueue<File> fileQueue_) {
        QiaLogging.logger.log(Level.FINE, "FXApplication::static.updateFileQueue: BEGIN");

        while (!fileQueue_.isEmpty()) {
            final File file = fileQueue_.poll();
            if (FileHelper.isSupportedType(file)) {
                mainFileAbsPaths.add(file.getAbsolutePath());
            }
        }

        QiaLogging.logger.log(Level.FINE, "FXApplication::static.updateFileQueue: END");
    }

    public static void init(String[] args) {
        QiaLogging.logger.log(Level.FINE, "FXApplication::static.init: BEGIN");

        // PLAN: when necessary, load file paths in args into  mainFileAbsPaths

        if (mainStage == null) {
            new Thread(() -> Application.launch(FXApplication.class)).start();
        } else {
            Platform.runLater(() -> mainStage.show()); // bring the window to the front
        }

        QiaLogging.logger.log(Level.FINE, "FXApplication::static.init: END");
    }

    private final Button chooseDirectoryButton = new Button("Open...");
    private final TextField filePathTextField = new TextField();
    private final Label filePathLabel = new Label();

    private final ListView<String> filePathListView = new ListView<>();

    private final Set<String> fileAbsPathSet = new HashSet<>();
    private String directoryAbsPath;

    @Override
    public void start(Stage stage) throws IOException {
        if (mainStage == null) mainStage = stage;

        final TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setText(String.join("\n", mainFileAbsPaths));

        chooseDirectoryButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(final ActionEvent e) {
                final DirectoryChooser directoryChooser = new DirectoryChooser();
                final File selectedDirectory = directoryChooser.showDialog(mainStage);
                if (selectedDirectory != null) {
                    directoryAbsPath = selectedDirectory.getAbsolutePath();
                    Set<String> filePathSet = FileHelper.getFilePathSet(directoryAbsPath);
                    mainFileAbsPaths.addAll(filePathSet);
                    // textArea.setText(String.join("\n", mainFileAbsPaths));
                    fileAbsPathSet.clear();
                    for (String path : filePathSet) {
                        String fileRelPath = FileHelper.stripLeadingFileSeparator(path.substring(directoryAbsPath.length()).stripLeading());
                        fileAbsPathSet.add(fileRelPath);
                    }
                    filePathListView.setItems(FXCollections.observableArrayList(fileAbsPathSet));
                }
            }
        });

        filePathTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            filePathListView.getItems().stream()
                    .filter(path -> path.toLowerCase().contains(newValue.toLowerCase()))
                    .forEach(path -> filePathListView.getSelectionModel().select(path));

            String firstMatchedPath = fileAbsPathSet.stream()
                    .filter(path -> path.toLowerCase().contains(newValue.toLowerCase()))
                    .toList().getFirst();
            filePathLabel.setText(firstMatchedPath);
        });

        filePathLabel.setOnMouseEntered(event -> filePathLabel.setCursor(Cursor.HAND));
        filePathLabel.setOnMouseExited(event -> filePathLabel.setCursor(Cursor.DEFAULT));
        filePathLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                String fileRelPath = filePathLabel.getText();
                String fileContent = FileHelper.getFileText(FileHelper.joinPaths(directoryAbsPath, fileRelPath));
                textArea.setText(fileContent);
            }
        });

        filePathListView.setPrefHeight(128);
        filePathListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    String fileRelPath = filePathListView.getSelectionModel().getSelectedItem();
                    if (fileRelPath != null) {
                        String fileContent = FileHelper.getFileText(FileHelper.joinPaths(directoryAbsPath, fileRelPath));
                        textArea.setText(fileContent);
                    }
                }
            }
        });

        HBox buttonHBox = new HBox(10, chooseDirectoryButton, filePathTextField, filePathLabel);
        buttonHBox.setPadding(new Insets(10));

        VBox fileSelectVBox = new VBox(10, buttonHBox, filePathListView);

        final Pane rootVBox = new VBox(12);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        rootVBox.getChildren().addAll(fileSelectVBox, textArea);
        rootVBox.setPadding(new Insets(12, 12, 12, 12));

        stage.setTitle("Qia md Reader");
        stage.setScene(new Scene(rootVBox, 720, 960));
        stage.show();
    }
}
