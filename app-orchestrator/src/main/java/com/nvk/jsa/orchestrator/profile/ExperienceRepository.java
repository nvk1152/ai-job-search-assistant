package com.nvk.jsa.orchestrator.profile;

import com.nvk.jsa.common.Experience;
import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class ExperienceRepository {

    private final JdbcClient jdbcClient;
    private final DataSource dataSource;

    public ExperienceRepository(JdbcClient jdbcClient, DataSource dataSource) {
        this.jdbcClient = jdbcClient;
        this.dataSource = dataSource;
    }

    public List<Experience> findByProfileId(UUID profileId) {
        return jdbcClient.sql("""
                        SELECT id, profile_id, company, title, start_date, end_date, bullets
                        FROM experience WHERE profile_id = :profileId ORDER BY start_date DESC NULLS LAST
                        """)
                .param("profileId", profileId)
                .query(this::mapRow)
                .list();
    }

    public void replaceAll(UUID profileId, List<Experience> experiences) {
        jdbcClient.sql("DELETE FROM experience WHERE profile_id = :profileId")
                .param("profileId", profileId)
                .update();

        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            for (Experience experience : experiences) {
                Array bullets = connection.createArrayOf("text", experience.bullets().toArray(new String[0]));
                jdbcClient.sql("""
                                INSERT INTO experience (id, profile_id, company, title, start_date, end_date, bullets)
                                VALUES (:id, :profileId, :company, :title, :startDate, :endDate, :bullets)
                                """)
                        .param("id", UUID.randomUUID())
                        .param("profileId", profileId)
                        .param("company", experience.company())
                        .param("title", experience.title())
                        .param("startDate", experience.startDate())
                        .param("endDate", experience.endDate())
                        .param("bullets", bullets)
                        .update();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist experience bullets", e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private Experience mapRow(ResultSet rs, int rowNum) throws SQLException {
        Array bulletsArray = rs.getArray("bullets");
        List<String> bullets = bulletsArray == null ? List.of() : List.of((String[]) bulletsArray.getArray());
        Date start = rs.getDate("start_date");
        Date end = rs.getDate("end_date");
        return new Experience(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("profile_id"),
                rs.getString("company"),
                rs.getString("title"),
                start != null ? start.toLocalDate() : null,
                end != null ? end.toLocalDate() : null,
                bullets);
    }
}
