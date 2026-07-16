package com.nvk.jsa.orchestrator.profile;

import com.nvk.jsa.common.Skill;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SkillRepository {

    private final JdbcClient jdbcClient;

    public SkillRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Skill> findByProfileId(UUID profileId) {
        return jdbcClient.sql("SELECT id, profile_id, name, level FROM skill WHERE profile_id = :profileId ORDER BY name")
                .param("profileId", profileId)
                .query(this::mapRow)
                .list();
    }

    public void replaceAll(UUID profileId, List<Skill> skills) {
        jdbcClient.sql("DELETE FROM skill WHERE profile_id = :profileId")
                .param("profileId", profileId)
                .update();
        for (Skill skill : skills) {
            jdbcClient.sql("INSERT INTO skill (id, profile_id, name, level) VALUES (:id, :profileId, :name, :level)")
                    .param("id", UUID.randomUUID())
                    .param("profileId", profileId)
                    .param("name", skill.name())
                    .param("level", skill.level())
                    .update();
        }
    }

    private Skill mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Skill(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("profile_id"),
                rs.getString("name"),
                rs.getString("level"));
    }
}
