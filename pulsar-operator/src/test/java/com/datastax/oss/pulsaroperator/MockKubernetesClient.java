package com.datastax.oss.pulsaroperator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NamespaceVisitFromServerGetWatchDeleteRecreateWaitApplicable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
@Getter
public class MockKubernetesClient {

    private static ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    @Data
    @AllArgsConstructor
    public static class ResourceInteraction<T extends HasMetadata> {
        T resource;
        NamespaceVisitFromServerGetWatchDeleteRecreateWaitApplicable interaction;

        @SneakyThrows
        public String getResourceYaml() {
            return YAML_MAPPER.writeValueAsString(resource);
        }
    }

    final KubernetesClient client;
    final List<ResourceInteraction> createdResources = new ArrayList<>();

    public MockKubernetesClient(String namespace) {
        client = mock(KubernetesClient.class);
        when(client.resource(any(HasMetadata.class))).thenAnswer((ic) -> {
            final NamespaceVisitFromServerGetWatchDeleteRecreateWaitApplicable interaction =
                    mock(NamespaceVisitFromServerGetWatchDeleteRecreateWaitApplicable.class);
            when(interaction.inNamespace(eq(namespace))).thenReturn(interaction);
            createdResources.add(new ResourceInteraction(ic.getArgument(0), interaction));
            return interaction;
        });
    }

    public <T extends HasMetadata> ResourceInteraction<T> getCreatedResource(Class<T> castTo) {
        for (ResourceInteraction createdResource : createdResources) {
            if (createdResource.getResource().getClass().isAssignableFrom(castTo)) {
                return createdResource;
            }
        }
        return null;
    }

    @SneakyThrows
    public static <T> T readYaml(String yaml, Class<T> toClass) {
        return YAML_MAPPER.readValue(yaml, toClass);
    }
}