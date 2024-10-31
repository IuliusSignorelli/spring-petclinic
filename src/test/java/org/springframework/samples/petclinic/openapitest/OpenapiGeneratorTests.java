package org.springframework.samples.petclinic.openapitest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springdoc.core.GenericResponseService;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SpringDocConfiguration;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springdoc.webmvc.core.RequestService;
import org.springdoc.webmvc.core.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.core.SpringWebMvcProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.test.annotation.DirtiesContext;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(
		classes = { OpenapiGeneratorTests.RestControllerLoader.class, OpenapiGeneratorTests.BeansInjector.class,
				OpenapiGeneratorTests.BeansFilter.class, },
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = { "springdoc.api-docs.enabled=true", // Ensure
																														// OpenAPI
																														// is
																														// enabled
		})

class OpenapiGeneratorTests {

	@LocalServerPort
	private int port;

	@DirtiesContext
	@Test
	void contextLoads() throws IOException, InterruptedException {
		HttpClient client = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(20))
			.build();
		HttpResponse<String> response = client.send(
				HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/v3/api-docs.yaml")).build(),
				HttpResponse.BodyHandlers.ofString());
		File outDir = new File(
				System.getProperty("openapi.out.path", System.getProperty("user.dir") + "/target/generated-openapi"));
		System.out.println("Creating dir: " + outDir.getAbsolutePath());
		outDir.mkdirs();
		FileCopyUtils.copy(response.body().getBytes(), new File(outDir, "openapi.yaml"));
		System.out.println(response.statusCode());
		// System.out.println(response.body());
	}

	@ImportAutoConfiguration({ WebMvcAutoConfiguration.class, // Necessary for MVC
																// features including
																// mvcConversionService
			ServletWebServerFactoryAutoConfiguration.class, // Embedded servlet container
			SpringDocConfiguration.class, // Base configuration for SpringDoc OpenAPI
			SpringDocConfigProperties.class, OpenApiWebMvcResource.class, SpringDocWebMvcConfiguration.class,
			SpringWebMvcProvider.class, RequestService.class, GenericResponseService.class, })
	static class BeansInjector {

		// Any test-specific beans can be declared here
		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		public ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

	}

	@Configuration
	@ComponentScan(basePackages = { "it.siav", "it.jarvis" },
			includeFilters = @ComponentScan.Filter(RestController.class), useDefaultFilters = false // Disable
																									// default
																									// scanning
																									// of
																									// other
																									// components
	)
	static class RestControllerLoader {

		// This configuration will only scan for beans
		// annotated with @RestController

	}

	@Configuration
	static class BeansFilter implements BeanFactoryPostProcessor, BeanDefinitionRegistryPostProcessor {

		private final Map<String, Object> ourControllers = new HashMap<String, Object>();

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			String[] beanDefinitionNames = registry.getBeanDefinitionNames();

			for (String beanName : beanDefinitionNames) { // for every bean
				BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);

				String className = beanDefinition.getBeanClassName();
				if (beanName.startsWith("openapiGenerator")) { // a bean used by the
																// generator
					continue;
				}
				if (isOurCode(className)) { // it's our code
					registry.removeBeanDefinition(beanName);
					if ((!beanName.endsWith("Controller")
							|| (className != null && !className.endsWith("Controller")))) { // not
																							// a
																							// controller:
																							// just
																							// remove
																							// it
						// System.out.println("Excluding bean " + beanName + " having
						// class: " + className);
					}
					else { // a controller: mock it
						final Class beanClass;
						try {
							beanClass = Class.forName(className);
						}
						catch (ClassNotFoundException e) {
							throw new RuntimeException(e);
						}
						// System.out.println("Mocking bean " + beanName + " having class:
						// " + className);
						Object mock = Mockito.mock(beanClass);
						ourControllers.put(beanName, mock);
					}
				}
			}
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			ourControllers.forEach((beanName, mock) -> { // register mocked controllers
				beanFactory.registerSingleton(beanName, mock);
			});
		}

		protected boolean isOurCode(@Nullable final String pkg) {
			return pkg != null && (pkg.startsWith("it.siav") || pkg.startsWith("it.jarvis"));
		}

	}

}
