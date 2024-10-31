package org.springframework.samples.petclinic.openapitest;

import org.junit.jupiter.api.Test;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.output.ConsoleRender;
import org.openapitools.openapidiff.core.output.HtmlRender;

import org.springframework.test.annotation.DirtiesContext;

import java.io.*;

public class OpenapiCompareTest {

	String oldSpecLocation = System.getProperty("oldSpecLocation");

	@DirtiesContext
	@Test
	public void getDiffSpec() throws FileNotFoundException {

		/*
		 * WebTestClient webClient =
		 * WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
		 *
		 * Mono<String> responseBodyMono = webClient.get() .uri("/v3/api-docs")
		 * .exchange() .expectStatus() .isOk() .returnResult(String.class)
		 * .getResponseBody() .next();
		 *
		 * responseBodyMono.subscribe(responseBody -> {
		 *
		 * System.out.println(responseBody); try { FileOutputStream fos = new
		 * FileOutputStream("output.json");
		 * fos.write(responseBody.getBytes(StandardCharsets.UTF_8)); } catch
		 * (FileNotFoundException e) { System.out.println(e.getMessage()); } catch
		 * (IOException e) { System.out.println(e.getMessage()); } });
		 */
		System.out.println("Reading path to old specs : " + oldSpecLocation);

		String newLocation = System.getProperty("user.dir") + "/target/generated-openapi/openapi.yaml";
		// Aggiungere un'eccezione se non vengono trovate le specifiche
		ChangedOpenApi diff = OpenApiCompare.fromLocations(oldSpecLocation, newLocation);

		try {
			FileWriter fw = new FileWriter(
					new File(System.getProperty("user.dir") + "/target/generated-openapi/testDiff.txt"));
			ConsoleRender consoleRender = new ConsoleRender();
			consoleRender.render(diff);
			fw.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		try {
			FileWriter fw = new FileWriter(
					new File(System.getProperty("user.dir") + "/target/generated-openapi/testNewApi.html"));
			HtmlRender htmlRender = new HtmlRender();
			htmlRender.render(diff);
			fw.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
