package com.nvk.jsa.orchestrator.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.nvk.jsa.common.Education;
import com.nvk.jsa.common.Experience;
import com.nvk.jsa.common.Profile;
import com.nvk.jsa.common.Skill;
import com.nvk.jsa.orchestrator.TestcontainersConfiguration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ProfileServiceIntegrationTest {

    @Autowired
    private ProfileService profileService;

    @Test
    void saveThenGetRoundTripsTheWholeAggregate() {
        UUID userId = UUID.randomUUID();

        ProfileView draft = new ProfileView(
                new Profile(null, userId, "Backend Engineer", "10 years of Java"),
                List.of(new Experience(null, null, "Acme", "Engineer",
                        LocalDate.of(2020, 1, 1), null, List.of("Built things"))),
                List.of(new Skill(null, null, "Java", "Expert")),
                List.of(new Education(null, null, "State University", "BS", "CS",
                        LocalDate.of(2012, 9, 1), LocalDate.of(2016, 5, 1))));

        ProfileView saved = profileService.saveProfile(userId, draft);
        ProfileView fetched = profileService.getProfile(userId);

        assertThat(fetched.profile().headline()).isEqualTo("Backend Engineer");
        assertThat(fetched.experiences()).hasSize(1);
        assertThat(fetched.experiences().get(0).bullets()).containsExactly("Built things");
        assertThat(fetched.skills()).hasSize(1);
        assertThat(fetched.education()).hasSize(1);
        assertThat(saved.profile().id()).isEqualTo(fetched.profile().id());
    }

    @Test
    void savingTwiceForTheSameUserReplacesRatherThanDuplicates() {
        UUID userId = UUID.randomUUID();
        ProfileView first = new ProfileView(
                new Profile(null, userId, "First headline", null),
                List.of(new Experience(null, null, "Acme", "Engineer", null, null, List.of())),
                List.of(),
                List.of());
        profileService.saveProfile(userId, first);

        ProfileView second = new ProfileView(
                new Profile(null, userId, "Second headline", null),
                List.of(new Experience(null, null, "Beta Corp", "Senior Engineer", null, null, List.of())),
                List.of(),
                List.of());
        profileService.saveProfile(userId, second);

        ProfileView fetched = profileService.getProfile(userId);
        assertThat(fetched.profile().headline()).isEqualTo("Second headline");
        assertThat(fetched.experiences()).hasSize(1);
        assertThat(fetched.experiences().get(0).company()).isEqualTo("Beta Corp");
    }
}
