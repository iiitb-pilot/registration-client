package io.mosip.registration.controller.docpreview;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JavaBridge {

    private static final Logger LOGGER = AppConfig.getLogger(JavaBridge.class);

    private String packetId;
    private String docsFolderPath;
    private Stage activeStage;

    public JavaBridge() {}

    public JavaBridge(String packetId, String docsFolderPath) {
        this.packetId = packetId;
        this.docsFolderPath = docsFolderPath;
    }

    public void showPopup(String fieldName) {
        if (packetId == null || packetId.isEmpty() || docsFolderPath == null || docsFolderPath.isEmpty()) {
            LOGGER.warn("Required parameters missing: packetId or docsFolderPath is null/empty.");
            return;
        }

        Platform.runLater(() -> {
            try {
                List<File> matchingFiles = findMatchingImages(packetId, fieldName);
                if (matchingFiles.isEmpty()) {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("No Documents Found");
                    alert.setHeaderText(null);
                    alert.setContentText("No scanned documents found for " + fieldName);
                    alert.showAndWait();
                    LOGGER.warn("No matching images found for field: {} and packetId: {}", fieldName, packetId);
                    return;
                }

                if (activeStage != null) {
                    activeStage.close();
                    activeStage = null;
                }

                final int[] currentIndex = {0};
                final double[] currentZoom = {0.7};
                final List<Image> cachedImages = new ArrayList<>();

                ImageView imageView = new ImageView();
                ScrollPane imagePane = new ScrollPane(imageView);
                imagePane.setFitToWidth(true);
                imagePane.setFitToHeight(true);
                imagePane.setPannable(true);
                imagePane.setStyle("-fx-background-color: white;");

                Label lblPage = new Label();
                Button btnPrev = new Button("⏮ Prev");
                Button btnNext = new Button("Next ⏭");
                Button btnZoomIn = new Button("➕ Zoom In");
                Button btnZoomOut = new Button("➖ Zoom Out");
                Button btnReset = new Button("🔄 Reset Zoom");

                Runnable updateImage = () -> {
                    if (cachedImages.size() <= currentIndex[0]) {
                        File file = matchingFiles.get(currentIndex[0]);
                        Image image = new Image(file.toURI().toString(), 1200, 1000, true, true, false);
                        cachedImages.add(image);
                        LOGGER.info("Image loaded and cached: {} ({}x{})"+file.getName(), image.getWidth(), image.getHeight());
                    }

                    Image image = cachedImages.get(currentIndex[0]);
                    imageView.setImage(image);
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(800 * currentZoom[0]);
                    imageView.setFitHeight(600 * currentZoom[0]);
                    lblPage.setText("Page " + (currentIndex[0] + 1) + " of " + matchingFiles.size());
                };

                updateImage.run();

                btnPrev.setOnAction(e -> {
                    if (currentIndex[0] > 0) {
                        currentIndex[0]--;
                        updateImage.run();
                    }
                });

                btnNext.setOnAction(e -> {
                    if (currentIndex[0] < matchingFiles.size() - 1) {
                        currentIndex[0]++;
                        updateImage.run();
                    }
                });

                btnZoomIn.setOnAction(e -> {
                    currentZoom[0] = Math.min(3.0, currentZoom[0] + 0.1);
                    updateImage.run();
                });

                btnZoomOut.setOnAction(e -> {
                    currentZoom[0] = Math.max(0.1, currentZoom[0] - 0.1);
                    updateImage.run();
                });

                btnReset.setOnAction(e -> {
                    currentZoom[0] = 0.7;
                    updateImage.run();
                });

                HBox controls = new HBox(10, btnPrev, btnNext, btnZoomIn, btnZoomOut, btnReset);
                controls.setAlignment(Pos.CENTER);

                VBox bottomBar = new VBox(5, lblPage, controls);
                bottomBar.setAlignment(Pos.CENTER);
                bottomBar.setPadding(new Insets(10));

                BorderPane layout = new BorderPane();
                layout.setCenter(imagePane);
                layout.setBottom(bottomBar);
                layout.setPadding(new Insets(10));

                Scene scene = new Scene(layout, 800, 600);
                Stage stage = new Stage();
                stage.setTitle("Scanned Document: " + fieldName + " [" + packetId + "]");
                stage.setScene(scene);
                stage.setResizable(false);
                stage.centerOnScreen();
                stage.setOnHidden(e -> activeStage = null);
                stage.show();

                activeStage = stage;

                LOGGER.info("Popup opened for field [{}] with {} page(s). PacketID: {}"+fieldName, matchingFiles.size(), packetId);

            } catch (Exception e) {
                LOGGER.error("Exception occurred while showing popup for field: {}", fieldName, e);
            }
        });
    }

    private List<File> findMatchingImages(String packetId, String fieldName) {
        List<File> matchingFiles = new ArrayList<>();
        try {
            File folder = new File(docsFolderPath);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles((dir, name) ->
                        name.startsWith(packetId) &&
                                name.contains(fieldName) &&
                                name.toLowerCase().endsWith(RegistrationConstants.DOCUMENT_IMAGE_EXTENSION)
                );
                if (files != null) {
                    for (File file : files) {
                        matchingFiles.add(file);
                    }
                }
            } else {
                LOGGER.warn("Docs folder not found or is not a directory: {}", docsFolderPath);
            }
        } catch (Exception e) {
            LOGGER.error("Error while finding images for packet [{}] and field [{}]", packetId + " " + fieldName, e);
        }
        return matchingFiles;
    }
}
