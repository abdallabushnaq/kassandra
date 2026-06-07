package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.dao.SprintDAO;
import de.bushnaq.abdalla.kassandra.dto.Status;
import de.bushnaq.abdalla.kassandra.repository.FeatureRepository;
import de.bushnaq.abdalla.kassandra.repository.SprintRepository;
import de.bushnaq.abdalla.kassandra.repository.VersionRepository;
import de.bushnaq.abdalla.kassandra.rest.dto.SprintOverviewDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SprintsOverviewServiceTest {

    @Mock
    SprintRepository sprintRepository;
    @Mock
    FeatureRepository featureRepository;
    @Mock
    VersionRepository versionRepository;
    @Mock
    ProductAclService productAclService;

    @InjectMocks
    SprintsOverviewService service;

    @BeforeEach
    void setUp() {
        // set admin auth so ACL checks are bypassed
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testLaneAssignment() {
        SprintDAO a = new SprintDAO();
        a.setName("A");
        a.setStart(LocalDateTime.of(2026,5,1,9,0));
        a.setEnd(LocalDateTime.of(2026,5,10,17,0));
        a.setStatus(Status.STARTED);

        SprintDAO b = new SprintDAO();
        b.setName("B");
        b.setStart(LocalDateTime.of(2026,5,5,9,0));
        b.setEnd(LocalDateTime.of(2026,5,15,17,0));
        b.setStatus(Status.STARTED);

        when(sprintRepository.findAll()).thenReturn(List.of(a, b));

        SprintOverviewDto dto = service.getOverview(LocalDateTime.of(2026,6,6,0,0), null);

        // overlapping sprints should be assigned to different lanes
        assertEquals(2, dto.lanes.size());
        // lane 0 should contain first sprint
        assertEquals(1, dto.lanes.get(0).sprints.size());
    }
}

