package com.jaba.awr_3.seversettings.configurations;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import com.jaba.awr_3.seversettings.basic.BasicService;

@Configuration
public class LocaleConfig implements WebMvcConfigurer {

  @Bean
  public LocaleResolver localeResolver() {
    SessionLocaleResolver slr = new SessionLocaleResolver();

    String lang = BasicService.LANGUAGE;
    Locale defaultLocale;

    if (lang != null && !lang.trim().isEmpty()) {
      defaultLocale = Locale.forLanguageTag(lang);
    } else {
      defaultLocale = Locale.ENGLISH; // ან Locale.US, Locale.of("ka", "GE") თუ ქართული გინდა
      // ან შეგიძლია properties-დან აიღო default
    }

    slr.setDefaultLocale(defaultLocale);
    return slr;
  }

}
