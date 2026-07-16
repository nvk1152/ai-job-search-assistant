package com.nvk.jsa.orchestrator.profile;

import com.nvk.jsa.common.Profile;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant-scoped profile CRUD: userId is passed explicitly on every call to ensure users can only
 * read/write their own data. Coordinates across Profile, Experience, Skill, and Education repositories.
 */
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ExperienceRepository experienceRepository;
    private final SkillRepository skillRepository;
    private final EducationRepository educationRepository;

    public ProfileService(
            ProfileRepository profileRepository,
            ExperienceRepository experienceRepository,
            SkillRepository skillRepository,
            EducationRepository educationRepository) {
        this.profileRepository = profileRepository;
        this.experienceRepository = experienceRepository;
        this.skillRepository = skillRepository;
        this.educationRepository = educationRepository;
    }

    public ProfileView getProfile(UUID userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> new Profile(UUID.randomUUID(), userId, null, null));
        return loadChildren(profile);
    }

    @Transactional
    public ProfileView saveProfile(UUID userId, ProfileView incoming) {
        UUID profileId = profileRepository.findByUserId(userId).map(Profile::id).orElseGet(UUID::randomUUID);
        Profile profile = new Profile(profileId, userId, incoming.profile().headline(), incoming.profile().summary());
        profileRepository.save(profile);
        experienceRepository.replaceAll(profileId, incoming.experiences());
        skillRepository.replaceAll(profileId, incoming.skills());
        educationRepository.replaceAll(profileId, incoming.education());
        return loadChildren(profile);
    }

    private ProfileView loadChildren(Profile profile) {
        return new ProfileView(
                profile,
                experienceRepository.findByProfileId(profile.id()),
                skillRepository.findByProfileId(profile.id()),
                educationRepository.findByProfileId(profile.id()));
    }
}
