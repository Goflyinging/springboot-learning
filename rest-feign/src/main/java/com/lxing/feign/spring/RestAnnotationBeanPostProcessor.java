package com.lxing.feign.spring;

import static org.springframework.core.BridgeMethodResolver.findBridgedMethod;
import static org.springframework.core.BridgeMethodResolver.isVisibilityBridgeMethodPair;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotation;

import com.lxing.feign.annotation.Rest;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.annotation.InjectionMetadata.InjectedElement;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

public class RestAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
    implements MergedBeanDefinitionPostProcessor, PriorityOrdered, ApplicationContextAware,
    BeanClassLoaderAware, DisposableBean {

  private final Log logger = LogFactory.getLog(getClass());

  private ApplicationContext applicationContext;

  private ClassLoader classLoader;

  /**
   * 构造bean的builder类 必须是RestBeanBuilder 的子类
   */
  private String restBeanBuilderClass;

  public String getRestBeanBuilderClass() {
    return restBeanBuilderClass;
  }

  public void setRestBeanBuilderClass(String restBeanBuilderClass) {
    this.restBeanBuilderClass = restBeanBuilderClass;
  }

  private final ConcurrentMap<String, InjectionMetadata> injectionMetadataCache =
      new ConcurrentHashMap<>(256);

  private final ConcurrentMap<String, Object> referenceBeansCache = new ConcurrentHashMap<>();

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;

  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }


  @Override
  public void destroy() throws Exception {

  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }

  @Override
  public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType,
      String beanName) {
    if (beanType != null) {
      InjectionMetadata metadata = findReferenceMetadata(beanName, beanType, null);
      metadata.checkConfigMembers(beanDefinition);
    }
  }

  @Override
  public PropertyValues postProcessPropertyValues(
      PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName)
      throws BeanCreationException {
    InjectionMetadata metadata = findReferenceMetadata(beanName, bean.getClass(), pvs);
    try {
      metadata.inject(bean, beanName, pvs);
    } catch (BeanCreationException ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new BeanCreationException(beanName, "Injection of @Rest dependencies failed", ex);
    }
    return pvs;
  }


  private InjectionMetadata findReferenceMetadata(String beanName, Class<?> clazz,
      PropertyValues pvs) {

    String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
    InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
    if (InjectionMetadata.needsRefresh(metadata, clazz)) {
      synchronized (this.injectionMetadataCache) {
        metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
          if (metadata != null) {
            metadata.clear(pvs);
          }
          try {
            metadata = buildReferenceMetadata(clazz);
            this.injectionMetadataCache.put(cacheKey, metadata);
          } catch (NoClassDefFoundError err) {
            throw new IllegalStateException("Failed to introspect bean class [" + clazz.getName() +
                "] for reference metadata: could not find class that it depends on", err);
          }
        }
      }
    }
    return metadata;
  }


  private InjectionMetadata buildReferenceMetadata(final Class<?> beanClass) {

    final List<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectionMetadata.InjectedElement>();

    elements.addAll(findFieldReferenceMetadata(beanClass));

    elements.addAll(findMethodReferenceMetadata(beanClass));

    return new InjectionMetadata(beanClass, elements);

  }

  private List<InjectionMetadata.InjectedElement> findFieldReferenceMetadata(
      final Class<?> beanClass) {

    final List<InjectionMetadata.InjectedElement> elements = new LinkedList<>();

    ReflectionUtils.doWithFields(beanClass, field -> {

      Rest reference = getAnnotation(field, Rest.class);

      if (reference != null) {

        if (Modifier.isStatic(field.getModifiers())) {
          if (logger.isWarnEnabled()) {
            logger.warn("@Rest annotation is not supported on static fields: " + field);
          }
          return;
        }

        elements.add(new RestFieldElement(field, reference));
      }

    });

    return elements;

  }

  private Object buildReferenceBean(Rest rest, Class<?> referenceClass) throws Exception {

    String referenceBeanCacheKey = generateReferenceBeanCacheKey(rest, referenceClass);

    Object restBean = referenceBeansCache.get(referenceBeanCacheKey);
    if (restBean == null) {
      RestBeanBuilder<?> restBeanBuilder = null;
      if (StringUtils.hasLength(restBeanBuilderClass)) {
        Class<?> aClass = classLoader.loadClass(restBeanBuilderClass);
        Method method = aClass.getMethod("create", Class.class);
        Object invoke = method.invoke(null, referenceClass);
        if (invoke != null && invoke instanceof RestBeanBuilder) {
          restBeanBuilder = (RestBeanBuilder) invoke;
        }
      } else {
        restBeanBuilder = RestBeanBuilder.create(referenceClass);
      }
      Assert.notNull(restBeanBuilder, "The restBeanBuilder must not be null!");
      restBean = restBeanBuilder.annotation(rest)
          .applicationContext(applicationContext)
          .classLoader(classLoader).build();
      referenceBeansCache.putIfAbsent(referenceBeanCacheKey, restBean);

    }

    return restBean;
  }

  private static String generateReferenceBeanCacheKey(Rest rest, Class<?> beanClass) {
    String interfaceName = resolveInterfaceName(rest, beanClass);
    return rest.feignName() + "/" + interfaceName + ":" + rest.version();
  }

  private static String resolveInterfaceName(Rest rest, Class<?> beanClass)
      throws IllegalStateException {
    String interfaceName;
    if (!"".equals(rest.interfaceName())) {
      interfaceName = rest.interfaceName();
    } else if (!void.class.equals(rest.interfaceClass())) {
      interfaceName = rest.interfaceClass().getName();
    } else if (beanClass.isInterface()) {
      interfaceName = beanClass.getName();
    } else {
      throw new IllegalStateException(
          "The @Rest undefined interfaceClass or interfaceName, and the property type "
              + beanClass.getName() + " is not a interface.");
    }

    return interfaceName;

  }


  private List<InjectionMetadata.InjectedElement> findMethodReferenceMetadata(
      final Class<?> beanClass) {

    final List<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectedElement>();

    ReflectionUtils.doWithMethods(beanClass, method -> {

      Method bridgedMethod = findBridgedMethod(method);

      if (!isVisibilityBridgeMethodPair(method, bridgedMethod)) {
        return;
      }

      Rest rest = findAnnotation(bridgedMethod, Rest.class);

      if (rest != null && method.equals(ClassUtils.getMostSpecificMethod(method, beanClass))) {
        if (Modifier.isStatic(method.getModifiers())) {
          if (logger.isWarnEnabled()) {
            logger.warn("@Rest annotation is not supported on static methods: " + method);
          }
          return;
        }
        if (method.getParameterTypes().length == 0) {
          if (logger.isWarnEnabled()) {
            logger.warn("@Rest  annotation should only be used on methods with parameters: " +
                method);
          }
        }
        PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, beanClass);
        elements.add(new RestMethodElement(method, pd, rest));
      }
    });

    return elements;

  }


  private class RestMethodElement extends InjectionMetadata.InjectedElement {

    private final Method method;

    private final Rest rest;

    protected RestMethodElement(Method method, PropertyDescriptor pd, Rest rest) {
      super(method, pd);
      this.method = method;
      this.rest = rest;
    }

    @Override
    protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

      Class<?> referenceClass = pd.getPropertyType();

      Object referenceBean = buildReferenceBean(rest, referenceClass);

      ReflectionUtils.makeAccessible(method);

      method.invoke(bean, referenceBean);

    }

  }

  /**
   *
   */
  private class RestFieldElement extends InjectionMetadata.InjectedElement {

    private final Field field;

    private final Rest rest;

    protected RestFieldElement(Field field, Rest rest) {
      super(field, null);
      this.field = field;
      this.rest = rest;
    }

    @Override
    protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

      Class<?> referenceClass = field.getType();

      Object referenceBean = buildReferenceBean(rest, referenceClass);

      ReflectionUtils.makeAccessible(field);

      field.set(bean, referenceBean);

    }

  }

}
