package com.civic.reporting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class CorsConfig {

    @Value("${file.upload-dir:uploads/issues}")
    private String uploadDir;

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // Serve frontend static assets from classpath:/static/
                registry.addResourceHandler("/**")
                        .addResourceLocations("classpath:/static/");

                // Serve uploaded media files directly from the server's uploads folder
                Path uploadPath = Paths.get("uploads").toAbsolutePath().normalize();
                String uploadResourceLocation = "file:" + uploadPath.toString() + "/";

                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations(uploadResourceLocation, "file:uploads/", "file:./uploads/");
            }
        };
    }
}
