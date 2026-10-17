package com.deliveryfee.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherImportJobTest {

    @Mock private WeatherImportService importService;

    private ImportStatus importStatus;
    private WeatherImportJob job;

    @BeforeEach
    void setUp() {
        importStatus = new ImportStatus();
        job = new WeatherImportJob(importService, importStatus);
    }

    @Test
    void scheduledImport_success_recordsSuccess() {
        job.scheduledImport();

        verify(importService).importLatest();
        assertThat(importStatus.lastSuccessAt()).isPresent();
        assertThat(importStatus.lastError()).isEmpty();
    }

    @Test
    void importOnStartup_success_recordsSuccess() {
        job.importOnStartup();

        verify(importService).importLatest();
        assertThat(importStatus.lastSuccessAt()).isPresent();
        assertThat(importStatus.lastError()).isEmpty();
    }

    @Test
    void scheduledImport_failure_recordsFailureWithoutThrowing() {
        doThrow(new IllegalStateException("boom")).when(importService).importLatest();

        job.scheduledImport();

        assertThat(importStatus.lastError()).contains("boom");
        assertThat(importStatus.lastSuccessAt()).isEmpty();
    }
}
