package com.nvk.jsa.orchestrator.profile;

import com.nvk.jsa.common.Profile;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {

    private final JdbcClient jdbcClient;

    public ProfileRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<Profile> findByUserId(UUID userId) {
        return jdbcClient.sql("SELECT id, user_id, headline, summary FROM profile WHERE user_id = :userId")
                .param("userId", userId)
                .query(this::mapRow)
                .optional();
    }

    public void save(Profile profile) {
        jdbcClient.sql("""
                        INSERT INTO profile (id, user_id, headline, summary)
                        VALUES (:id, :userId, :headline, :summary)
                        ON CONFLICT (user_id) DO UPDATE SET headline = EXCLUDED.headline, summary = EXCLUDED.summary
                        """)
                .param("id", profile.id())
                .param("userId", profile.userId())
                .param("headline", profile.headline())
                .param("summary", profile.summary())
                .update();
    }

    private Profile mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Profile(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("user_id"),
                rs.getString("headline"),
                rs.getString("summary"));
    }
}
