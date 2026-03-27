package com.autograder.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
Registers a kuberentes client object for the rest of the server so we don't 
manualy have to write stuff with kubectl, needed for Fabric8 to communciate
with the Kubernetes cluster which should be installed if this is being run.
*/
@Configuration
public class KubernetesConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        Config config = new ConfigBuilder().build();
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}