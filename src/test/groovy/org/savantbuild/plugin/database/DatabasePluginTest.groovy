/*
 * Copyright (c) 2014, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.savantbuild.plugin.database

import org.savantbuild.dep.domain.License
import org.savantbuild.dep.domain.Version
import org.savantbuild.domain.Project
import org.savantbuild.output.Output
import org.savantbuild.output.SystemOutOutput
import org.savantbuild.runtime.RuntimeConfiguration
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.testng.Assert.assertEquals
/**
 * Tests the database plugin.
 *
 * @author Brian Pontarelli
 */
class DatabasePluginTest {
  public static Path projectDir

  Output output

  Project project

  @BeforeSuite
  static void beforeSuite() {
    projectDir = Paths.get("")
    if (!Files.isRegularFile(projectDir.resolve("LICENSE"))) {
      projectDir = Paths.get("../database-plugin")
    }
  }

  @BeforeMethod
  void beforeMethod() {
    output = new SystemOutOutput(true)
    output.enableDebug()

    project = new Project(projectDir, output)
    project.group = "org.savantbuild.test"
    project.name = "database-plugin"
    project.version = new Version("1.0")
    project.licenses.put(License.ApacheV2_0, null)
  }

  @Test
  void mysqlEnsureEqual() throws Exception {
    DatabasePlugin plugin = new DatabasePlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.type = "mysql"
    plugin.settings.createUsername = "root"
    plugin.createTestDatabase()
    plugin.execute(file: "src/test/resources/test-mysql.sql")

    plugin.createMainDatabase()
    plugin.execute(file: "src/test/resources/test-mysql.sql")

    plugin.ensureEqual(left: "database_plugin", right: "database_plugin_test")
  }

  @Test
  void postgresqlEnsureEqual() throws Exception {
    DatabasePlugin plugin = new DatabasePlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.type = "postgresql"
    plugin.settings.createUsername = "postgres"
    plugin.createTestDatabase()
    plugin.execute(file: "src/test/resources/test-postgresql.sql")

    plugin.createMainDatabase()
    plugin.execute(file: "src/test/resources/test-postgresql.sql")

    plugin.ensureEqual(left: "database_plugin", right: "database_plugin_test")
  }

  @Test
  void mysqlDatabase() throws Exception {
    DatabasePlugin plugin = new DatabasePlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.type = "mysql"
    plugin.settings.createUsername = "root"
    plugin.createDatabase()

    plugin.execute(file: "src/test/resources/test-mysql.sql")

    Process process = ["mysql", "-udev", "-pdev", "-e", "show tables", "-v", "database_plugin"].execute()
    assertEquals(process.text, "--------------\nshow tables\n--------------\n\nTables_in_database_plugin\ntest\n")
    assertEquals(process.exitValue(), 0)
  }

  @Test
  void mysqlCreateMainDatabase() throws Exception {
    DatabasePlugin plugin = new DatabasePlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.name = "old"
    plugin.settings.type = "mysql"
    plugin.settings.createUsername = "root"
    plugin.createMainDatabase()

    plugin.execute(file: "src/test/resources/test-mysql.sql")

    Process process = ["mysql", "-udev", "-pdev", "-e", "show tables", "-v", "database_plugin"].execute()
    assertEquals(process.text, "--------------\nshow tables\n--------------\n\nTables_in_database_plugin\ntest\n")
    assertEquals(process.exitValue(), 0)
  }

  @Test
  void mysqlCreateTestDatabase() throws Exception {
    DatabasePlugin plugin = new DatabasePlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.type = "mysql"
    plugin.settings.createUsername = "root"
    plugin.createTestDatabase()

    plugin.execute(file: "src/test/resources/test-mysql.sql")

    Process process = ["mysql", "-udev", "-pdev", "-e", "show tables", "-v", "database_plugin_test"].execute()
    assertEquals(process.text, "--------------\nshow tables\n--------------\n\nTables_in_database_plugin_test\ntest\n")
    assertEquals(process.exitValue(), 0)
  }

  @Test
  void postgresqlDatabase() throws Exception {
    DatabasePlugin plugin = new DatabasePlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.type = "postgresql"
    plugin.settings.createUsername = "postgres"
    plugin.createDatabase()

    plugin.execute(file: "src/test/resources/test-postgresql.sql")

    Process process = ["psql", "-U", "dev", "-c", "\\dt", "database_plugin"].execute()
    assertEquals(process.text, "       List of relations\n" +
        " Schema | Name | Type  | Owner \n" +
        "--------+------+-------+-------\n" +
        " public | test | table | dev\n" +
        "(1 row)\n" +
        "\n")
    assertEquals(process.exitValue(), 0)
  }
}
