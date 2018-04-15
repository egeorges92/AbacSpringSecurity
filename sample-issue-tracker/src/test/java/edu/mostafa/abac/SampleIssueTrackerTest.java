package edu.mostafa.abac;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postman.Collection;
import com.postman.Header;
import com.postman.Item;
import com.postman.Request;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SampleIssueTrackerTest {

	private static final String LF = "\n";
	@Autowired
	TestRestTemplate restTemplate;

	// @Test
	public void contextLoads() {
	}

	//@Test
	public void fullScenarioFromPostmanJSONFile() throws Exception {
		// parse json file
		String json = null;
		try (Scanner scanner = new Scanner(Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("AbacSpringSecurity.postman_collection.json"), StandardCharsets.UTF_8.name())) {
			json = scanner.useDelimiter("\\A").next();
		}
		// extract Postman Collection from JSON String.
		ObjectMapper mapper = new ObjectMapper();
		Collection collection = mapper.readValue(json, Collection.class);
		// ierate over postman items 
		for (Item item : collection.getItem()) {			
			Request request = item.getRequest();
			// prepare headers
			HttpHeaders requestHeaders = new HttpHeaders();
			for (Header header : request.getHeader()) {
				requestHeaders.add(header.getKey(), header.getValue());
			}
			// prepare request entity
			HttpEntity<?> httpEntity = new HttpEntity<String>(request.getBody().getRaw(), requestHeaders);
			ResponseEntity<String> response = restTemplate.exchange(request.getUrl(), HttpMethod.valueOf(request.getMethod()), httpEntity, String.class );
			Assert.assertEquals(item.getName() ,HttpStatus.OK, response.getStatusCode());
			
			StringBuffer buffer = new StringBuffer();
			buffer.append("\t@Test").append(LF);
			buffer.append("\t ").append(item.getName().replaceAll("- ",  "").replaceAll(" ", "_")).append("()").append(LF);
			buffer.append("\t\trequestHeaders = new HttpHeaders();").append(LF);
			for (Header header : request.getHeader()) {
				buffer.append("\t\trequestHeaders.add(\"").append(header.getKey()).append("\", \"").append(header.getValue()).append("\");").append(LF);
			}
			buffer.append("\t\thttpEntity = new HttpEntity<String>(\"").append(request.getBody().getRaw().replaceAll("\r", "").replaceAll("\n", "").replaceAll("\"", "\\\"")).append("\", requestHeaders);").append(LF);
			buffer.append("\t\tresponse = restTemplate.exchange(\"").append(request.getUrl()).append("\", HttpMethod.").append(request.getMethod()).append(", httpEntity, String.class );").append(LF);
			buffer.append("\t\tAssert.assertEquals(item.getName() ,HttpStatus.OK.value(), response.getStatusCode().value());").append(LF);
			if(response.getBody()!=null) {
				buffer.append("\t\tAssert.assertEquals(\"").append(item.getName()).append("\" , \"").append(response.getBody()).append("\", response.getBody());").append(LF);
			}
			buffer.append("\t}").append(LF);
			System.err.println(buffer.toString());
			//return;
		}
	}
	
	@Test
	public void fullScenario() throws Exception {
		
		// YWRtaW46cGFzc3dvcmQ admin:password
		
		// cG0xOnBhc3N3b3Jk pm1:password
		
		// Project - Add
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add("Content-Type", "application/json");
		HttpEntity<?> httpEntity = new HttpEntity<String>("{\"name\": \"FirstProject\", \"description\": \"The first project\"}", requestHeaders);
		ResponseEntity<String> response = restTemplate.withBasicAuth("admin", "password").exchange("/projects/", HttpMethod.POST, httpEntity, String.class );
		Assert.assertEquals("Project - Add" ,HttpStatus.OK.value(), response.getStatusCode().value());
		
		// Project - List
		requestHeaders = new HttpHeaders();
		requestHeaders.add("Content-Type", "application/json");
		httpEntity = new HttpEntity<String>("{\"name\": \"FirstProject\", \"description\": \"The first project\"}", requestHeaders);
		response = restTemplate.withBasicAuth("admin", "password").exchange("/projects/", HttpMethod.GET, httpEntity, String.class );
		Assert.assertEquals("Project - List" ,HttpStatus.OK.value(), response.getStatusCode().value());
		Assert.assertEquals("Project - List" ,"[{\"id\":1,\"name\":\"FirstProject\",\"description\":\"The first project\"}]", response.getBody());
		
		// Project - Assign PM
		requestHeaders = new HttpHeaders();
		requestHeaders.add("Content-Type", "text/plain");
		httpEntity = new HttpEntity<String>("pm1", requestHeaders);
		response = restTemplate.withBasicAuth("admin", "password").exchange("/projects/1/pm/", HttpMethod.PUT, httpEntity, String.class );
		Assert.assertEquals("Project - Assign PM" ,HttpStatus.OK.value(), response.getStatusCode().value());
		
		// Project - Assign Developer
		requestHeaders = new HttpHeaders();
		requestHeaders.add("Content-Type", "application/json");
		httpEntity = new HttpEntity<String>("{\"name\": \"dev1\", \"role\": \"DEVELOPER\"}", requestHeaders);
		response = restTemplate.withBasicAuth("pm1", "password").exchange("/projects/1/users/", HttpMethod.POST, httpEntity, String.class );
		Assert.assertEquals("Project - Assign Developer" ,HttpStatus.OK.value(), response.getStatusCode().value());
		
		// Project - Assign Tester
		requestHeaders = new HttpHeaders();
		requestHeaders.add("Content-Type", "application/json");
		httpEntity = new HttpEntity<String>("{\"name\": \"test1\", \"role\": \"TESTER\"}", requestHeaders);
		response = restTemplate.withBasicAuth("pm1", "password").exchange("/projects/1/users/", HttpMethod.POST, httpEntity, String.class );
		Assert.assertEquals("Project - Assign Tester" ,HttpStatus.OK.value(), response.getStatusCode().value());
		
		// Project - List Users
		requestHeaders = new HttpHeaders();
		httpEntity = new HttpEntity<String>("", requestHeaders);
		response = restTemplate.withBasicAuth("pm1", "password").exchange("/projects/1/users/", HttpMethod.GET, httpEntity, String.class );
		Assert.assertEquals("Project - List Users" ,HttpStatus.OK.value(), response.getStatusCode().value());
		Assert.assertEquals("Project - List Users" ,"[{\"name\":\"dev1\",\"role\":\"DEVELOPER\"},{\"name\":\"pm1\",\"role\":\"PM\"},{\"name\":\"test1\",\"role\":\"TESTER\"}]", response.getBody());
		
		// Project - Delete User
		requestHeaders = new HttpHeaders();
		httpEntity = new HttpEntity<String>("", requestHeaders);
		response = restTemplate.withBasicAuth("pm1", "password").exchange("/projects/1/users/dev1", HttpMethod.DELETE, httpEntity, String.class );
		Assert.assertEquals("Project - Delete User" ,HttpStatus.OK.value(), response.getStatusCode().value());
		
		// Project - View
		requestHeaders = new HttpHeaders();
		httpEntity = new HttpEntity<String>("", requestHeaders);
		response = restTemplate.withBasicAuth("pm1", "password").exchange("/projects/1/", HttpMethod.GET, httpEntity, String.class );
		Assert.assertEquals("Project - View" ,HttpStatus.OK.value(), response.getStatusCode().value());
		Assert.assertEquals("Project - View" ,"{\"id\":1,\"name\":\"FirstProject\",\"description\":\"The first project\"}", response.getBody());
		
		
		// Project - Delete
		requestHeaders = new HttpHeaders();
		httpEntity = new HttpEntity<String>("", requestHeaders);
		response = restTemplate.withBasicAuth("admin", "password").exchange("/projects/1/", HttpMethod.DELETE, httpEntity, String.class );
		Assert.assertEquals("Project - Delete" ,HttpStatus.OK.value(), response.getStatusCode().value());
		
		// Issues - List
		requestHeaders = new HttpHeaders();
		httpEntity = new HttpEntity<String>("", requestHeaders);
		response = restTemplate.withBasicAuth("pm1", "password").exchange("/projects/1/issues/", HttpMethod.GET, httpEntity, String.class );
		Assert.assertEquals("Issues - List" ,HttpStatus.OK.value(), response.getStatusCode().value());
		
		// Issues - Create
		requestHeaders = new HttpHeaders();
		requestHeaders.add("Content-Type", "application/json");
		httpEntity = new HttpEntity<String>("{ \"type\": \"TASK\", \"name\": \"Task1\", \"description\": \"First task\"}", requestHeaders);
		response = restTemplate.withBasicAuth("pm1", "password").exchange("/projects/1/issues/", HttpMethod.POST, httpEntity, String.class );
		Assert.assertEquals("Issues - Create" ,HttpStatus.OK.value(), response.getStatusCode().value());
		
		// Issues - Update
		requestHeaders = new HttpHeaders();
		requestHeaders.add("Content-Type", "application/json");
		httpEntity = new HttpEntity<String>("{ \"type\": \"TASK\", \"name\": \"Task1\", \"description\": \"First task, after update\"}", requestHeaders);
		response = restTemplate.withBasicAuth("pm1", "password").exchange("/projects/1/issues/1/", HttpMethod.PUT, httpEntity, String.class );
		Assert.assertEquals("Issues - Update" ,HttpStatus.OK.value(), response.getStatusCode().value());
		
		// Issues - Delete
		requestHeaders = new HttpHeaders();
		httpEntity = new HttpEntity<String>("", requestHeaders);
		response = restTemplate.withBasicAuth("pm1", "password").exchange("/projects/1/issues/1/", HttpMethod.DELETE, httpEntity, String.class );
		Assert.assertEquals("Issues - Delete" ,HttpStatus.OK.value(), response.getStatusCode().value());
		
		// Issues - Assign
		requestHeaders = new HttpHeaders();
		requestHeaders.add("Content-Type", "text/plain");
		httpEntity = new HttpEntity<String>("dev1", requestHeaders);
		response = restTemplate.withBasicAuth("pm1", "password").exchange("/projects/1/issues/1/assignee", HttpMethod.PUT, httpEntity, String.class );
		Assert.assertEquals("Issues - Assign" ,HttpStatus.OK.value(), response.getStatusCode().value());
		
		// Issues - Change Status
		requestHeaders = new HttpHeaders();
		requestHeaders.add("Content-Type", "text/plain");
		httpEntity = new HttpEntity<String>("COMPLETED", requestHeaders);
		response = restTemplate.withBasicAuth("pm1", "password").exchange("/projects/1/issues/1/status", HttpMethod.PUT, httpEntity, String.class );
		Assert.assertEquals("Issues - Change Status" ,HttpStatus.OK.value(), response.getStatusCode().value());
	}

}
