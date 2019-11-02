package com.lxing.feign.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.lxing.feign.spring.bean.Contributor;
import com.lxing.feign.spring.bean.TestBean;
import com.lxing.feign.spring.config.RestAnnotationConfig;
import java.util.List;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class RestAnnotationBeanPostProcessorTest {

  ApplicationContext context;

  @Before
  public void before() throws Exception {
    this.context = new AnnotationConfigApplicationContext(RestAnnotationConfig.class);
  }

  @After
  public void after() throws Exception {
  }


  @Test
  public void testRegisterAnnotationBeanPostProcessor() {
    Object restAnnotationBeanPostProcessor = context.getBean("RestAnnotationBeanPostProcessor");
    assertThat(restAnnotationBeanPostProcessor.getClass()).isNotNull();
    TestBean bean = context.getBean(TestBean.class);
    List<Contributor> contributors = bean.getGitHub().contributors("netflix", "feign");
    for (Contributor contributor : contributors) {
      System.out.println(contributor.getLogin() + " (" + contributor.getContributions() + ")");
    }

  }


} 
