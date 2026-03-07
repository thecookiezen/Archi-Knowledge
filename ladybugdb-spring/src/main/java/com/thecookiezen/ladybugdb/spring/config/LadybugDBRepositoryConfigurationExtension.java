package com.thecookiezen.ladybugdb.spring.config;

import com.thecookiezen.ladybugdb.spring.repository.NodeRepository;
import com.thecookiezen.ladybugdb.spring.repository.support.LadybugDBRepositoryFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

import java.util.Collection;
import java.util.Collections;

/**
 * Configuration extension for LadybugDB repositories.
 * Provides the module-specific configuration details for Spring Data.
 */
public class LadybugDBRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

    private static final String MODULE_NAME = "LadybugDB";
    private static final String MODULE_PREFIX = "ladybugdb";

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    protected String getModulePrefix() {
        return MODULE_PREFIX;
    }

    @Override
    public String getRepositoryFactoryBeanClassName() {
        return LadybugDBRepositoryFactoryBean.class.getName();
    }

    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.singleton(NodeRepository.class);
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {
        source.getAttribute("ladybugDBTemplateRef")
                .ifPresent(ref -> builder.addPropertyReference("template", ref));
    }
}
