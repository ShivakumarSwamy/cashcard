package example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashCardApplicationTests {

	@Autowired
	TestRestTemplate testRestTemplate;

	@Test
	void shouldBe200WhenCashCardIsFetched() {
		ResponseEntity<String> responseEntity = testRestTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards/99", String.class);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(responseEntity.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		Number amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	@Test
	void shouldBe404WhenCashCardHasUnknownId() {
		ResponseEntity<String> responseEntity = testRestTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards/1000", String.class);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(responseEntity.getBody()).isBlank();
	}

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void shouldBe200WhenCashCardSaved() {
		CashCard newCashCard = new CashCard(null, 250.00, "sarah1");
		ResponseEntity<Void> responseEntity = testRestTemplate.withBasicAuth("sarah1", "abc123").postForEntity("/cashcards", newCashCard, Void.class);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI location = responseEntity.getHeaders().getLocation();
		ResponseEntity<String> savedResponseEntity = testRestTemplate.withBasicAuth("sarah1", "abc123").getForEntity(location, String.class);
		assertThat(savedResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(savedResponseEntity.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");
		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(250.00);
	}

	@Test
	void shouldBe200WForAllCashCardsWhenListIsRequested() {
		ResponseEntity<String> response = testRestTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		int cashCardCount = documentContext.read("$.length()");
		assertThat(cashCardCount).isEqualTo(3);

		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.0, 150.00);
	}

	@Test
	void shouldBe200APageOfCashCards() {
		ResponseEntity<String> response = testRestTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards?page=0&size=1", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);
	}

	@Test
	void shouldBe200ASortedPageOfCashCards() {
		ResponseEntity<String> response = testRestTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray read = documentContext.read("$[*]");
		assertThat(read.size()).isEqualTo(1);

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	@Test
	void shouldBe200ASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
		ResponseEntity<String> response = testRestTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
	}

	@Test
	void shouldBe401WhenReturnCashCardUsingBadCredentialsOfPassword() {
		ResponseEntity<String> response = testRestTemplate.withBasicAuth("sarah1", "BAD-PASSWORD").getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldBe401WhenReturnCashCardUsingBadCredentialsOfUsername() {
		ResponseEntity<String> response = testRestTemplate.withBasicAuth("BAD-USER", "abc123").getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldBe403WhenUserTryToAccessNotOwningCard() {
		ResponseEntity<String> response = testRestTemplate.withBasicAuth("hank-owns-no-cards", "qrs456").getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}
}
