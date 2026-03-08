package com.autograder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
//Use GSON for creating json objects instead of default jackson springboot has
@SuppressWarnings("removal")
@Configuration
public class GsonConfig implements WebMvcConfigurer {

    @Bean
    public Gson gson() {
        return new GsonBuilder().serializeNulls().create();
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(c -> c instanceof GsonHttpMessageConverter);
        GsonHttpMessageConverter converter = new GsonHttpMessageConverter(gson());
        converters.addFirst(converter);
    }
}
