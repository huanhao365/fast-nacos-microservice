package fast.cloud.nacos.grpc.starter.config;


import fast.cloud.nacos.grpc.starter.GrpcClient;
import fast.cloud.nacos.grpc.starter.GrpcServer;
import fast.cloud.nacos.grpc.starter.annotation.GrpcService;
import fast.cloud.nacos.grpc.starter.annotation.GrpcServiceScan;
import fast.cloud.nacos.grpc.starter.binding.GrpcServiceProxy;
import fast.cloud.nacos.grpc.starter.service.CommonService;
import fast.cloud.nacos.grpc.starter.service.SerializeService;
import fast.cloud.nacos.grpc.starter.service.impl.SofaHessianSerializeService;
import fast.cloud.nacos.grpc.starter.util.ClassNameUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@EnableConfigurationProperties(GrpcProperties.class)
public class GrpcAutoConfiguration {

    private final AbstractApplicationContext applicationContext;

    private final GrpcProperties grpcProperties;

    public GrpcAutoConfiguration(AbstractApplicationContext applicationContext, GrpcProperties grpcProperties) {
        this.applicationContext = applicationContext;
        this.grpcProperties = grpcProperties;
    }

    /**
     * ?????? RPC ?????????/????????????
     */
    @Bean
    @ConditionalOnMissingBean(SerializeService.class)
    public SerializeService serializeService() {
        return new SofaHessianSerializeService();
    }

    /**
     * PRC ????????????
     */
    @Bean
    public CommonService commonService(SerializeService serializeService) {
        return new CommonService(applicationContext, serializeService);
    }

    /**
     * RPC ?????????
     */
    @Bean
    @ConditionalOnMissingBean(GrpcServer.class)
    @ConditionalOnProperty(value = "spring.grpc.enable", havingValue = "true")
    public GrpcServer grpcServer(CommonService commonService) throws Exception {
        GrpcServer server = new GrpcServer(grpcProperties, commonService);
        server.start();
        return server;
    }

    /**
     * RPC ?????????
     */
    @Bean
    @ConditionalOnMissingBean(GrpcClient.class)
    public GrpcClient grpcClient(SerializeService serializeService) {
        GrpcClient client = new GrpcClient(grpcProperties, serializeService);
        client.init();
        return client;
    }

    /**
     * ???????????? @GrpcService ??????????????????????????????????????????????????? Spring ??????
     */
    public static class ExternalGrpcServiceScannerRegistrar implements BeanFactoryAware, ImportBeanDefinitionRegistrar,
        ResourceLoaderAware {

        private BeanFactory beanFactory;

        private ResourceLoader resourceLoader;

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }

        @Override
        public void setResourceLoader(ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            ClassPathBeanDefinitionScanner scanner = new ClassPathGrpcServiceScanner(registry);
            scanner.setResourceLoader(this.resourceLoader);
            scanner.addIncludeFilter(new AnnotationTypeFilter(GrpcService.class));
            Set<BeanDefinition> beanDefinitions = scanPackages(importingClassMetadata, scanner);
            ProxyUtil.registerBeans(beanFactory, beanDefinitions);
        }

        /**
         * ?????????
         */
        private Set<BeanDefinition> scanPackages(AnnotationMetadata importingClassMetadata, ClassPathBeanDefinitionScanner scanner) {
            List<String> packages = new ArrayList<>();
            Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(GrpcServiceScan.class.getCanonicalName());
            if (annotationAttributes != null) {
                String[] basePackages = (String[]) annotationAttributes.get("packages");
                if (basePackages.length > 0) {
                    packages.addAll(Arrays.asList(basePackages));
                }
            }
            Set<BeanDefinition> beanDefinitions = new HashSet<>();
            if (CollectionUtils.isEmpty(packages)) {
                return beanDefinitions;
            }
            packages.forEach(pack -> beanDefinitions.addAll(scanner.findCandidateComponents(pack)));
            return beanDefinitions;
        }

    }

    protected static class ClassPathGrpcServiceScanner extends ClassPathBeanDefinitionScanner {

        ClassPathGrpcServiceScanner(BeanDefinitionRegistry registry) {
            super(registry, false);
        }

        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
        }

    }

    protected static class ProxyUtil {
        static void registerBeans(BeanFactory beanFactory, Set<BeanDefinition> beanDefinitions) {
            for (BeanDefinition beanDefinition : beanDefinitions) {
                String className = beanDefinition.getBeanClassName();
                if (StringUtils.isEmpty(className)) {
                    continue;
                }
                try {
                    // ???????????????
                    Class<?> target = Class.forName(className);
                    Object invoker = new Object();
                    InvocationHandler invocationHandler = new GrpcServiceProxy<>(target, invoker);
                    Object proxy = Proxy
                        .newProxyInstance(GrpcService.class.getClassLoader(), new Class[]{target}, invocationHandler);

                    // ????????? Spring ??????
                    String beanName = ClassNameUtils.beanName(className);
                    ((DefaultListableBeanFactory) beanFactory).registerSingleton(beanName, proxy);
                } catch (ClassNotFoundException e) {
                    log.warn("class not found : " + className);
                }
            }
        }
    }

}