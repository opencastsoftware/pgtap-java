/*
 * SPDX-FileCopyrightText:  © 2024 Opencast Software Europe Ltd <https://opencastsoftware.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.opencastsoftware.pgtap;

import org.junit.jupiter.api.*;
import org.junit.platform.commons.util.ExceptionUtils;
import org.tap4j.consumer.TapConsumerFactory;
import org.tap4j.model.Comment;
import org.tap4j.model.TestResult;
import org.tap4j.util.DirectiveValues;
import org.tap4j.util.StatusValues;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class PgTapTest {
    private final PostgreSQLContainer<?> database;
    private final Path pgTapTestPath;
    private final Path pgTapTmpPath;

    /**
     * Constructs a psql command based on the options used in the <a href="https://web.archive.org/web/20231118220249/https://pgtap.org/documentation.html#pgtaptestscripts">pgTAP documentation</a>.
     *
     * @param sqlFile the .sql script file to run
     * @return a psql command to execute the script file
     */
    private String createPsqlCommand(Path sqlFile) {
        return String.join(" ", List.of(
                "psql",
                "--quiet",
                "--tuples-only",
                "--no-align",
                "--set=ON_ERROR_ROLLBACK=1",
                "--set=ON_ERROR_STOP=true",
                "--pset=pager=off",
                "--username=test",
                "--dbname=test",
                "-c", "'CREATE EXTENSION IF NOT EXISTS pgtap;'",
                "-f", pgTapTmpPath.resolve(sqlFile).toString()
        ));
    }

    public PgTapTest(PostgreSQLContainer<?> database, Path pgTapTestPath, Path pgTapTmpPath) {
        this.database = database;
        this.pgTapTestPath = pgTapTestPath;
        this.pgTapTmpPath = pgTapTmpPath;
    }

    private boolean isSqlFile(Path file, BasicFileAttributes attrs) {
        return attrs.isRegularFile() && file.getFileName().toString().endsWith(".sql");
    }

    private Stream<Path> findPgTapTests() throws IOException {
        return Files.find(pgTapTestPath, Integer.MAX_VALUE, this::isSqlFile);
    }

    private DynamicTest createDynamicTest(Path sqlFile, TestResult testResult) {
        var directive = testResult.getDirective();

        // This test was marked as skipped or pending
        if (directive != null) {
            var directiveValue = directive.getDirectiveValue();

            if (directiveValue.equals(DirectiveValues.SKIP) || directiveValue.equals(DirectiveValues.TODO)) {
                var directiveReason = directive.getReason() != null ? " - " + directive.getReason() : "";
                var testName = testResult.getTestNumber() + directiveReason;

                return DynamicTest.dynamicTest(testName, sqlFile.toUri(), () -> {
                    Assumptions.abort(testResult.getDirective().getReason());
                });
            }
        }

        var description = testResult.getDescription() != null ? " " + testResult.getDescription() : "";
        var testName = testResult.getTestNumber() + description;

        return DynamicTest.dynamicTest(testName, sqlFile.toUri(), () -> {
            if (testResult.getStatus().equals(StatusValues.NOT_OK)) {
                var commentText = testResult.getComments().stream()
                        .map(Comment::getText)
                        .collect(Collectors.joining(System.lineSeparator(), System.lineSeparator(), ""));
                var failureDescription = commentText.isBlank() ? testResult.getDescription() : commentText;
                Assertions.fail(failureDescription);
            }
        });
    }

    @TestFactory
    Stream<DynamicNode> pgTapTests() throws IOException {
        var consumer = TapConsumerFactory.makeTap13Consumer();

        return findPgTapTests().flatMap(sqlFile -> {
            try {
                var relativeSqlFile = pgTapTestPath.relativize(sqlFile);
                var psqlCommand = createPsqlCommand(relativeSqlFile);

                var execResult = database.execInContainer("sh", "-c", psqlCommand);

                var tapString = execResult.getStdout();
                var errString = execResult.getStderr();

                if (!tapString.isBlank()) {
                    System.out.println(tapString);
                }

                if (!errString.isBlank()) {
                    System.err.println(errString);
                }

                if (execResult.getExitCode() != 0) {
                    return Stream.of(DynamicTest.dynamicTest(
                            relativeSqlFile.toString(),
                            sqlFile.toUri(),
                            () -> Assertions.fail(relativeSqlFile + " failed with exit code " + execResult.getExitCode())
                    ));
                }

                var tapOutput = consumer.load(tapString);

                var tapTests = tapOutput
                        .getTestResults()
                        .stream()
                        .map(result -> createDynamicTest(sqlFile, result));

                var tapTestFile = DynamicContainer.dynamicContainer(
                        relativeSqlFile.toString(),
                        sqlFile.toUri(),
                        tapTests
                );

                return Stream.of(tapTestFile);
            } catch (IOException | InterruptedException e) {
                throw ExceptionUtils.throwAsUncheckedException(e);
            }
        });
    }
}
