package example.cashcard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class CashCardJsonTest {

    @Autowired
    private JacksonTester<CashCard> json;

    @Test
    void cashCardSerializationTest() throws IOException {
        CashCard cashCard = new CashCard(99L, 123.45);

        assertThat(json.write(cashCard)).isStrictlyEqualToJson("cash-card.json");

        assertThat(json.write(cashCard)).hasJsonPathNumberValue("@.id")
                .extractingJsonPathNumberValue("@.id").isEqualTo(99);

        assertThat(json.write(cashCard)).hasJsonPathNumberValue("@.amount")
                .extractingJsonPathNumberValue("@.amount").isEqualTo(123.45);
    }

    @Test
    void cashCardDeserializationTest() throws IOException {
        String jsonContent = """
                {
                    "id":99,
                    "amount":123.45
                }
                """;

        assertThat(json.parse(jsonContent)).isEqualTo(new CashCard(99L, 123.45));
        assertThat(json.parseObject(jsonContent).id()).isEqualTo(99);
        assertThat(json.parseObject(jsonContent).amount()).isEqualTo(123.45);
    }
}
