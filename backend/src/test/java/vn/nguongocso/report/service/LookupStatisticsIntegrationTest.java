package vn.nguongocso.report.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.report.dto.response.LookupStatisticsResponse;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class LookupStatisticsIntegrationTest {

    @Autowired
    private LookupStatisticsService lookupStatisticsService;

    @Test
    void testGetStatistics_day_reproduce() {
        UUID orgId = UUID.randomUUID();
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getRoleCode()).thenReturn("VT-02");
        when(userDetails.getOrganizationId()).thenReturn(orgId);

        LookupStatisticsResponse responseDay = lookupStatisticsService.getStatistics(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 24),
                null,
                null,
                null,
                "DAY",
                userDetails
        );
        assertThat(responseDay).isNotNull();

        LookupStatisticsResponse responseWeek = lookupStatisticsService.getStatistics(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 24),
                null,
                null,
                null,
                "WEEK",
                userDetails
        );
        assertThat(responseWeek).isNotNull();

        LookupStatisticsResponse responseMonth = lookupStatisticsService.getStatistics(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 24),
                null,
                null,
                null,
                "MONTH",
                userDetails
        );
        assertThat(responseMonth).isNotNull();

        LookupStatisticsResponse responseYear = lookupStatisticsService.getStatistics(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 24),
                null,
                null,
                null,
                "YEAR",
                userDetails
        );
        assertThat(responseYear).isNotNull();
    }
}
