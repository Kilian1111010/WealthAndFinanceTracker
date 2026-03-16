package kilian1111010.wealthandfinancetracker.domain;  // ← Im domain-Paket

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiPathConfig implements WebMvcConfigurer {

 @Override
 public void configurePathMatch(PathMatchConfigurer configurer) {
  configurer.addPathPrefix("/api",
          HandlerTypePredicate.forBasePackage("kilian1111010.wealthandfinancetracker.domain"));
 }
}
