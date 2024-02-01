/*
 * SPDX-FileCopyrightText:  © 2024 Opencast Software Europe Ltd <https://opencastsoftware.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.opencastsoftware.pgtap;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.nio.file.Paths;

@Testcontainers
public class ExampleTest extends PgTapTest {
    private static final Path pgTapTestPath = Paths.get("src", "test", "sql");
    private static final Path pgTapTmpPath = Paths.get("/tmp", "pgtap", "sql");

    private static final DockerImageName containerName = DockerImageName
            .parse("ghcr.io/opencastsoftware/timescaledb-with-pgtap:2.13.0-pg15")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer<?> timescaleDb = new PostgreSQLContainer<>(containerName)
            .waitingFor(Wait.forListeningPort())
            .withCopyFileToContainer(
                    MountableFile.forHostPath(pgTapTestPath.toAbsolutePath()),
                    pgTapTmpPath.toString()
            );

    public ExampleTest() {
        super(timescaleDb, pgTapTestPath, pgTapTmpPath);
    }

    @BeforeAll
    public static void runFlyway() {
        Flyway.configure()
                .locations("classpath:db/migration")
                .dataSource(timescaleDb.getJdbcUrl(), timescaleDb.getUsername(), timescaleDb.getPassword())
                .defaultSchema("test")
                .load()
                .migrate();
    }
}
