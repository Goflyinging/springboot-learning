package com.lxing.feign.spring.config;

import com.lxing.feign.spring.RestAnnotationBeanPostProcessor;
import com.lxing.feign.spring.bean.TestBean;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.jaxrs.JAXRSContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestAnnotationConfig {

  @Bean(name = "RestAnnotationBeanPostProcessor")
  public RestAnnotationBeanPostProcessor RestAnnotationBeanPostProcessor() {
    return new RestAnnotationBeanPostProcessor();
  }

  @Bean(name = "feign")
  public Feign feign() {
    return Feign.builder()
        .contract(new JAXRSContract())
        .encoder(new JacksonEncoder())
        .decoder(new JacksonDecoder())
        .decode404().build();
  }

  @Bean
  public TestBean testBean() {
    return new TestBean();
  }

}
