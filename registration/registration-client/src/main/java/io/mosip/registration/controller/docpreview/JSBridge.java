package io.mosip.registration.controller.docpreview;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.mosip.registration.config.AppConfig;
import javafx.application.Platform;
import io.mosip.kernel.core.logger.spi.Logger;

import java.util.List;

public class JSBridge {

    private static final Logger LOGGER = AppConfig.getLogger(JSBridge.class);

    public void showImagePopupFromJava(String imagesJson, String appId) {
        LOGGER.info("Received request to show image popup for Application ID: {}", appId);
        Platform.runLater(() -> {
            try {
                List<String> images = new Gson().fromJson(imagesJson, new TypeToken<List<String>>() {}.getType());
                LOGGER.info("Parsed {} images for Application ID: {}", images.size(), appId);
                new ImagePopupViewer(images, appId).show();
            } catch (Exception e) {
                LOGGER.error("Error showing image popup for Application ID: {}", appId, e);
            }
        });
    }
}
