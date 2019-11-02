package com.lxing.feign.spring;

import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

import com.lxing.feign.annotation.Rest;
import feign.Feign;
import feign.Target.HardCodedTarget;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

public class RestBeanBuilder<T> {

  private Rest rest;
  private ClassLoader classLoader;
  private ApplicationContext applicationContext;
  private Class<T> interfaceClass;

  private RestBeanBuilder(Class<T> interfaceClass) {
    this.interfaceClass = interfaceClass;
  }

  public static <T> RestBeanBuilder<T> create(Class<T> interfaceClass) {
    return new RestBeanBuilder<>(interfaceClass);
  }

  public RestBeanBuilder<T> annotation(Rest annotation) {
    this.rest = annotation;
    return this;
  }

  public RestBeanBuilder<T> classLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public RestBeanBuilder<T> applicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    return this;
  }

  public T build() {
    Assert.notNull(interfaceClass, "The interfaceClass must not be null!");
    Assert.notNull(rest, "The Annotation must not be null!");
    Assert.notNull(classLoader, "The ClassLoader must not be null!");
    Assert.notNull(applicationContext, "The ApplicationContext must not be null!");
    Feign feign = prepareFeign();
    String url = prepareUrl();
    Assert.notNull(feign, "The feign must not be null!");
    Assert.hasText(url,"The url must not be blank!");
    return feign.newInstance(new HardCodedTarget<>(interfaceClass, url));


  }

  protected Feign prepareFeign() {
    String feignName = rest.feignName();
    Map<String, Feign> FeignMap = beansOfTypeIncludingAncestors(applicationContext, Feign.class);
    return FeignMap.get(feignName);
  }

  protected String prepareUrl() {
    return rest.url();

  }
}
