package com.bloxbean.cardano.yaci.store.core.storage;

import com.bloxbean.cardano.yaci.store.core.storage.impl.ErrorRepository;
import com.bloxbean.cardano.yaci.store.core.storage.impl.model.ErrorEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class ErrorRepositoryTest {
    @Autowired
    private ErrorRepository errorRepository;

    @Test
    void save_shouldPersistErrorEntity() {
        ErrorEntity errorEntity = new ErrorEntity();
        errorEntity.setBlock(12345L);
        errorEntity.setErrorCode("errorCode123");
        errorEntity.setReason("Test reason");
        errorEntity.setDetails("Test details");

        ErrorEntity savedErrorEntity = errorRepository.save(errorEntity);

        assertNotNull(savedErrorEntity.getId());
        assertEquals(12345, savedErrorEntity.getBlock());
        assertEquals("errorCode123", savedErrorEntity.getErrorCode());
        assertEquals("Test reason", savedErrorEntity.getReason());
        assertEquals("Test details", savedErrorEntity.getDetails());
    }

    @Test
    void findAllOrderById_shouldReturnErrorsInDescendingOrder() {
        ErrorEntity errorEntity1 = new ErrorEntity(null, 12345L, "errorCode1", "reason1", "details1", LocalDateTime.now());
        ErrorEntity errorEntity2 = new ErrorEntity(null, 23456L, "errorCode2", "reason2", "details2", LocalDateTime.now());
        errorRepository.save(errorEntity1);
        errorRepository.save(errorEntity2);

        List<ErrorEntity> errors = errorRepository.findAllOrderById();

        assertEquals(2, errors.size());

        assertNotNull(errors.get(1).getId());
        assertEquals("errorCode2", errors.get(0).getErrorCode());
        assertEquals("reason2", errors.get(0).getReason());
        assertEquals("details2", errors.get(0).getDetails());

        assertNotNull(errors.get(1).getId());
        assertEquals("errorCode1", errors.get(1).getErrorCode());
        assertEquals("reason1", errors.get(1).getReason());
        assertEquals("details1", errors.get(1).getDetails());
    }

    @Test
    void findById_shouldReturnErrorEntityById() {
        ErrorEntity errorEntity = new ErrorEntity(null, 23456L, "errorCode2", "reason2", "details2", LocalDateTime.now());
        ErrorEntity savedErrorEntity = errorRepository.save(errorEntity);

        Optional<ErrorEntity> foundErrorEntity = errorRepository.findById(savedErrorEntity.getId());

        assertTrue(foundErrorEntity.isPresent());
        assertEquals("errorCode2", foundErrorEntity.get().getErrorCode());
    }
}
