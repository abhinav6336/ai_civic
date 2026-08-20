package com.civic.reporting.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Computer Vision & Image Quality Validator for uploaded civic complaint photos.
 * Detects image dimensions, blank/pitch-black corruption, and computes visual validity score.
 */
@Component
public class ImageQualityValidator {

    private static final Logger log = LoggerFactory.getLogger(ImageQualityValidator.class);

    public ImageValidationResult validateAndAnalyze(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new ImageValidationResult(true, 1.0, 0, 0, "No image uploaded");
        }

        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                return new ImageValidationResult(false, 0.0, 0, 0, "Corrupted or unsupported image format");
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (width < 30 || height < 30) {
                return new ImageValidationResult(false, 0.20, width, height, "Image dimensions too small (< 30px)");
            }

            // Sample pixels for brightness & variance (to detect blank black/white images)
            long totalLuminance = 0;
            int sampleStep = Math.max(1, (width * height) / 1000);
            int samples = 0;
            long minLum = 255;
            long maxLum = 0;

            for (int y = 0; y < height; y += (int) Math.sqrt(sampleStep)) {
                for (int x = 0; x < width; x += (int) Math.sqrt(sampleStep)) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    int lum = (r + g + b) / 3;
                    totalLuminance += lum;
                    if (lum < minLum) minLum = lum;
                    if (lum > maxLum) maxLum = lum;
                    samples++;
                }
            }

            double avgLuminance = samples > 0 ? (double) totalLuminance / samples : 128.0;
            double contrastSpread = maxLum - minLum;

            if (contrastSpread < 10 && (avgLuminance < 15 || avgLuminance > 240)) {
                return new ImageValidationResult(false, 0.30, width, height, "Image appears blank or completely underexposed");
            }

            double qualityScore = Math.min(1.0, 0.80 + (contrastSpread / 500.0));
            qualityScore = Math.round(qualityScore * 100.0) / 100.0;

            return new ImageValidationResult(true, qualityScore, width, height, "Valid visual evidence (" + width + "x" + height + "px)");
        } catch (Exception e) {
            log.warn("Image quality analysis failed: {}", e.getMessage());
            return new ImageValidationResult(true, 0.70, 0, 0, "Uploaded with basic file validation");
        }
    }

    public static class ImageValidationResult {
        private final boolean valid;
        private final double qualityScore;
        private final int width;
        private final int height;
        private final String summary;

        public ImageValidationResult(boolean valid, double qualityScore, int width, int height, String summary) {
            this.valid = valid;
            this.qualityScore = qualityScore;
            this.width = width;
            this.height = height;
            this.summary = summary;
        }

        public boolean isValid() {
            return valid;
        }

        public double getQualityScore() {
            return qualityScore;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getSummary() {
            return summary;
        }
    }
}
