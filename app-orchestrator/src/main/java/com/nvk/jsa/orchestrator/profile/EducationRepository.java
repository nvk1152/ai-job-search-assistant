package com.nvk.jsa.orchestrator.profile;

import com.nvk.jsa.common.Education;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class EducationRepository {

    private final JdbcClient jdbcClient;

    public EducationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Education> findByProfileId(UUID profileId) {
        return jdbcClient.sql("""
                        SELECT id, profile_id, institution, degree, field, start_date, end_date
                        FROM education WHERE profile_id = :profileId ORDER BY start_date DESC NULLS LAST
                        """)
                .param("profileId", profileId)
                .query(this::mapRow)
                .list();
    }

    public void replaceAll(UUID profileId, List<Education> education) {
        jdbcClient.sql("DELETE FROM education WHERE profile_id = :profileId")
                .param("profileId", profileId)
                .update();
        for (Education entry : education) {
            jdbcClient.sql("""
                            INSERT INTO education (id, profile_id, institution, degree, field, start_date, end_date)
                            VALUES (:id, :profileId, :institution, :degree, :field, :startDate, :endDate)
                            """)
                    .param("id", UUID.randomUUID())
                    .param("profileId", profileId)
                    .param("institution", entry.institution())
                    .param("degree", entry.degree())
                    .param("field", entry.field())
                    .param("startDate", entry.startDate())
                    .param("endDate", entry.endDate())
                    .update();
        }
    }

    private Education mapRow(ResultSet rs, int rowNum) throws SQLException {
        Date start = rs.getDate("start_date");
        Date end = rs.getDate("end_date");
        return new Education(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("profile_id"),
                rs.getString("institution"),
                rs.getString("degree"),
                rs.getString("field"),
                start != null ? start.toLocalDate() : null,
                end != null ? end.toLocalDate() : null);
    }
}
