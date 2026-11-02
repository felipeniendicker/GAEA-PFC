package Gaea.Api.migration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gaea-flyway-validation;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class SchemaMigrationValidationTests {

    @Test
    void deveValidarMapeamentoJpaContraEsquemaMigrado() {
    }
}
