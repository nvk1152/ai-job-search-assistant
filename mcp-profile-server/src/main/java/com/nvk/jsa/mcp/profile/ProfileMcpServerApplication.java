package com.nvk.jsa.mcp.profile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP server: exposes tenant-scoped, read-only access to the authenticated user's profile
 * (experiences, skills, education) via MCP tools. Integrated into the orchestrator agent loop
 * for fit-gap analysis and tailored output generation.
 */
@SpringBootApplication
public class ProfileMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileMcpServerApplication.class, args);
    }
}
