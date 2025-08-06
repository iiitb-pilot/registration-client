package io.mosip.registration.controller.docpreview;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

public class ImagePopupViewer {

    private static final Logger LOGGER = AppConfig.getLogger(ImagePopupViewer.class);

    private final List<String> images;
    private final String applicationId;
    private int currentIndex = 0;
    private final ImageView imageView = new ImageView();
    private final Label pageCountLabel = new Label();
    private final Label appIdLabel = new Label();

    public ImagePopupViewer(List<String> images, String applicationId) {
        this.images = images;
        this.applicationId = applicationId;
        LOGGER.info("ImagePopupViewer initialized with {} images for Application ID: {}", images.size(), applicationId);
    }

    public void show() {
        try {
            LOGGER.info("Opening Scanned Document Viewer popup for Application ID: {}", applicationId);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Scanned Document Viewer");

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double width = screenBounds.getWidth() * 0.7;
            double height = screenBounds.getHeight() * 0.7;
            stage.setWidth(width);
            stage.setHeight(height);
            stage.setX((screenBounds.getWidth() - width) / 2);
            stage.setY((screenBounds.getHeight() - height) / 2);

            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setStyle("-fx-background-color: #1e272e;");

            HBox toolbarContent = new HBox(
                    styledButton("⏪ Prev", "#3498db", e -> showPrev()),
                    styledButton("Next ⏩", "#3498db", e -> showNext()),
                    styledButton("➕ Zoom In", "#27ae60", e -> zoom(1.1)),
                    styledButton("➖ Zoom Out", "#f39c12", e -> zoom(0.9)),
                    styledButton("⟳ Reset", "#8e44ad", e -> resetZoom())
            );
            toolbarContent.setAlignment(Pos.CENTER);
            toolbarContent.setSpacing(15);
            toolbarContent.setPadding(new Insets(8));
            toolbarContent.setStyle("-fx-background-color: #2f3640;");

            appIdLabel.setText("Application ID: " + (applicationId != null ? applicationId : "N/A"));
            appIdLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
            pageCountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

            HBox bottomBar = new HBox(appIdLabel, pageCountLabel);
            bottomBar.setAlignment(Pos.CENTER);
            bottomBar.setSpacing(50);
            bottomBar.setPadding(new Insets(10));
            bottomBar.setStyle("-fx-background-color: #2f3640;");

            BorderPane root = new BorderPane();
            root.setTop(toolbarContent);
            root.setCenter(scrollPane);
            root.setBottom(bottomBar);
            root.setStyle("-fx-background-color: #1e272e;");

            Scene scene = new Scene(root);
            stage.setScene(scene);

            loadImage(currentIndex);
            stage.showAndWait();

            LOGGER.info("Document viewer closed for Application ID: {}", applicationId);
        } catch (Exception e) {
            LOGGER.error("Error showing image popup for Application ID: {}", applicationId, e);
        }
    }

    private Button styledButton(String text, String bgColor, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button(text);
        btn.setOnAction(action);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-padding: 8px 15px; -fx-background-radius: 5px;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: derive(" + bgColor + ", 20%); "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + bgColor + "; "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;"));
        return btn;
    }

    private void loadImage(int index) {
        try {
            LOGGER.debug("Loading page {} of {} for Application ID: {}", index + 1, images.size(), applicationId);

            String base64 = images.get(index);
            String cleanBase64 = base64.contains(",") ? base64.split(",")[1] : base64;
            byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64);
            imageView.setImage(new Image(new ByteArrayInputStream(decodedBytes)));
            imageView.setPreserveRatio(true);

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            imageView.setFitWidth(screenBounds.getWidth() * 0.65);
            imageView.setFitHeight(screenBounds.getHeight() * 0.65);

            pageCountLabel.setText("Page: " + (currentIndex + 1) + " of " + images.size());
        } catch (Exception e) {
            LOGGER.error("Error loading image at index {} for Application ID: {}", index, applicationId, e);
        }
    }

    private void showPrev() {
        if (currentIndex > 0) {
            currentIndex--;
            loadImage(currentIndex);
        } else {
            LOGGER.warn("Already at first page for Application ID: {}", applicationId);
        }
    }

    private void showNext() {
        if (currentIndex < images.size() - 1) {
            currentIndex++;
            loadImage(currentIndex);
        } else {
            LOGGER.warn("Already at last page for Application ID: {}", applicationId);
        }
    }

    private void zoom(double factor) {
        LOGGER.debug("Zooming by factor {} for Application ID: {}", factor, applicationId);
        imageView.setFitWidth(imageView.getFitWidth() * factor);
        imageView.setFitHeight(imageView.getFitHeight() * factor);
    }

    private void resetZoom() {
        LOGGER.debug("Resetting zoom for Application ID: {}", applicationId);
        loadImage(currentIndex);
    }
}
