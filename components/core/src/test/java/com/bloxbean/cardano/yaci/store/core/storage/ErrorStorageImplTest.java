package com.bloxbean.cardano.yaci.store.core.storage;

import com.bloxbean.cardano.yaci.store.core.storage.impl.ErrorRepository;
import com.bloxbean.cardano.yaci.store.core.storage.impl.model.ErrorEntity;
import com.bloxbean.cardano.yaci.store.events.ErrorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
class ErrorStorageImplTest {
    private ErrorRepository errorRepository;
    private ErrorStorageImpl errorStorage;

    @BeforeEach
    void setUp() {
        errorRepository = mock(ErrorRepository.class);
        errorStorage = new ErrorStorageImpl(errorRepository);
    }

    @Test
    void save() {
        ErrorEvent errorEvent = ErrorEvent.builder()
                .block(123L)
                .errorCode("404")
                .reason("Not Found")
                .details("Details about the error")
                .build();

        errorStorage.save(errorEvent);

        verify(errorRepository, times(1)).save(argThat(errorEntity ->
                errorEntity.getBlock().equals(123L)
                        && errorEntity.getErrorCode().equals("404")
                        && errorEntity.getReason().equals("Not Found")
                        && errorEntity.getDetails().equals("Details about the error")
        ));
    }

    @Test
    void findById() {
        ErrorEntity errorEntity = ErrorEntity.builder()
                .id(1)
                .block(123L)
                .errorCode("404")
                .reason("Not Found")
                .details("Details about the error")
                .build();

        when(errorRepository.findById(1)).thenReturn(java.util.Optional.of(errorEntity));

        ErrorEvent result = errorStorage.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(123L, result.getBlock());
        assertEquals("404", result.getErrorCode());
        assertEquals("Not Found", result.getReason());
        assertEquals("Details about the error", result.getDetails());
    }

    @Test
    void findAll() {
        ErrorEntity errorEntity1 = ErrorEntity.builder()
                .id(1)
                .block(123L)
                .errorCode("404")
                .reason("Not Found")
                .details("Details 1")
                .build();
        ErrorEntity errorEntity2 = ErrorEntity.builder()
                .id(2)
                .block(456L)
                .errorCode("500")
                .reason("Internal Server Error")
                .details("Details 2")
                .build();

        when(errorRepository.findAllOrderById()).thenReturn(List.of(errorEntity1, errorEntity2));

        List<ErrorEvent> result = errorStorage.findAll();

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals(123L, result.get(0).getBlock());
        assertEquals("404", result.get(0).getErrorCode());
        assertEquals("Details 1", result.get(0).getDetails());

        assertEquals(2, result.get(1).getId());
        assertEquals(456L, result.get(1).getBlock());
        assertEquals("500", result.get(1).getErrorCode());
        assertEquals("Details 2", result.get(1).getDetails());
    }
}
