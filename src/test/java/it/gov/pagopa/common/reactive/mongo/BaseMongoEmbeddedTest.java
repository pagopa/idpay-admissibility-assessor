package it.gov.pagopa.common.reactive.mongo;

import it.gov.pagopa.common.mongo.singleinstance.AutoConfigureSingleInstanceMongodb;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@TestPropertySource(properties = {
        "de.flapdoodle.mongodb.embedded.version=4.2.24",

        "spring.data.mongodb.database=idpay",
        "spring.data.mongodb.config.connectionPool.maxSize: 100",
        "spring.data.mongodb.config.connectionPool.minSize: 0",
        "spring.data.mongodb.config.connectionPool.maxWaitTimeMS: 120000",
        "spring.data.mongodb.config.connectionPool.maxConnectionLifeTimeMS: 0",
        "spring.data.mongodb.config.connectionPool.maxConnectionIdleTimeMS: 120000",
        "spring.data.mongodb.config.connectionPool.maxConnecting: 2",
})
@ExtendWith(SpringExtension.class)
@AutoConfigureSingleInstanceMongodb
@SuppressWarnings("squid:S2187")
public class BaseMongoEmbeddedTest {
}
