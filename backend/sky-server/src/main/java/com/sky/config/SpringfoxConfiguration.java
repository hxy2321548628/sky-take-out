package com.sky.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 兼容 Springfox 3.0 与 Actuator。
 *
 * Spring Boot 2.6+ 的 Actuator 端点映射（WebMvcEndpointHandlerMapping）使用
 * PathPatternParser，其 RequestMappingInfo 的 PatternsRequestCondition 为 null；
 * Springfox 在启动时遍历所有 HandlerMapping 并调用 getPatterns()，于是 NPE：
 * <pre>
 * Failed to start bean 'documentationPluginsBootstrapper'
 * java.lang.NullPointerException: ... "this.condition" is null
 * </pre>
 * 这里把 PathPattern 风格的映射从 Springfox 的扫描列表里剔除，只保留 Ant 风格映射。
 *
 * 后续如果迁到 springdoc-openapi，可以整体删掉这个类。
 */
@Configuration
public class SpringfoxConfiguration {

    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }
        };
    }

    private static <T extends RequestMappingInfoHandlerMapping> void customizeSpringfoxHandlerMappings(List<T> mappings) {
        List<T> antMappings = mappings.stream()
                .filter(mapping -> mapping.getPatternParser() == null)
                .collect(Collectors.toList());
        mappings.clear();
        mappings.addAll(antMappings);
    }

    @SuppressWarnings("unchecked")
    private static List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
        try {
            Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
            if (field == null) {
                throw new IllegalStateException("未找到 WebMvcRequestHandlerProvider.handlerMappings");
            }
            ReflectionUtils.makeAccessible(field);
            return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
