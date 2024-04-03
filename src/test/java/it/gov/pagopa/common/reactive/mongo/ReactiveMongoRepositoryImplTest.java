package it.gov.pagopa.common.reactive.mongo;

import com.mongodb.client.result.DeleteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import reactor.core.publisher.Mono;

import javax.swing.text.html.parser.Entity;

import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ReactiveMongoRepositoryImplTest {

    private ReactiveMongoRepositoryImpl<Entity, String> repository;
    @Mock
    private ReactiveMongoOperations mongoOperationsMock;
    @Mock
    private MongoEntityInformation<Entity, String> entityInformationMock;

    @BeforeEach
    void setUp() {
        repository = new ReactiveMongoRepositoryImpl<>(entityInformationMock, mongoOperationsMock);
    }

    @Test
    void testRemoveById() {
        // Given
        String id = "TestId";
        when(entityInformationMock.getIdAttribute()).thenReturn("_id");
        when(entityInformationMock.getJavaType()).thenReturn(Entity.class);
        when(entityInformationMock.getCollectionName()).thenReturn("entityCollection");

        // When
        Mono<DeleteResult> result = repository.removeById(id);

        // Then
        verify(mongoOperationsMock).remove(any(Query.class), eq(Entity.class), eq("entityCollection"));

    }
}
