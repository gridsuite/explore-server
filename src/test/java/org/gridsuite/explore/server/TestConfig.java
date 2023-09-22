package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.*;
import org.gridsuite.explore.server.utils.ParametersType;
import org.springframework.boot.test.autoconfigure.web.client.MockRestServiceServerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * because @AutoConfigureMockRestServiceServer support only 1 restTemplate instance, we need to do it ourselves
 * @see MockRestServiceServerAutoConfiguration
 */
@TestConfiguration
public class TestConfig {
    @DynamicPropertySource
    static void setDynamicProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.cache.type", () -> "none");
    }

    @Bean
    public MockServerRestTemplateCustomizer mockServerRestTemplateCustomizer() {
        return new MockServerRestTemplateCustomizer();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static <T> RestTemplate getRestTemplate(final Class<T> clazz, final T service) throws IllegalAccessException {
        final Field rtField = Arrays.stream(clazz.getDeclaredFields()).filter(f -> RestTemplate.class.equals(f.getType())).findAny().get();
        rtField.setAccessible(true);
        return (RestTemplate) rtField.get(service);
    }

    @Bean
    public MockRestServiceServer mockRestSrvCaseService(MockServerRestTemplateCustomizer customizer, CaseService caseService) throws IllegalAccessException {
        return customizer.getServer(getRestTemplate(CaseService.class, caseService));
    }

    @Bean
    public MockRestServiceServer mockRestSrvContingencyListService(MockServerRestTemplateCustomizer customizer, ContingencyListService contingencyListService) throws IllegalAccessException {
        return customizer.getServer(getRestTemplate(ContingencyListService.class, contingencyListService));
    }

    @Bean
    public MockRestServiceServer mockRestSrvDirectoryService(MockServerRestTemplateCustomizer customizer, DirectoryService directoryService) throws IllegalAccessException {
        return customizer.getServer(getRestTemplate(DirectoryService.class, directoryService));
    }

    @Bean
    public MockRestServiceServer mockRestSrvFilterService(MockServerRestTemplateCustomizer customizer, FilterService filterService) throws IllegalAccessException {
        return customizer.getServer(getRestTemplate(FilterService.class, filterService));
    }

    @Bean
    public MockRestServiceServer mockRestSrvStudyService(MockServerRestTemplateCustomizer customizer, StudyService studyService) throws IllegalAccessException {
        return customizer.getServer(getRestTemplate(StudyService.class, studyService));
    }

    @SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
    @Bean
    public Map<ParametersType, MockRestServiceServer> mockRestSrvParametersService(MockServerRestTemplateCustomizer customizer, ParametersService parametersService) throws IllegalAccessException {
        final Field rtField = Arrays.stream(ParametersService.class.getDeclaredFields()).filter(f -> Map.class.equals(f.getType())).findAny().get();
        rtField.setAccessible(true);
        return ((Map<ParametersType, RestTemplate>) rtField.get(parametersService)).entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> customizer.getServer(e.getValue())
        ));
    }
}
