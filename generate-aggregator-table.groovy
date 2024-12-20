#!/usr/bin/env groovy
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/* groovylint-disable SystemExit  */
/* groovylint-disable JavaIoPackageAccess */

import groovy.io.FileType
import groovy.xml.XmlParser
import groovy.util.Node

if (args.length == 0) {
    println 'Generates the project tables on the Sling Aggregator project.'
    println 'Please provide the Sling Directory: groovy generate-aggregator-table.groovy [SLING_DIR]'
    System.exit(1)
}

GroovyShell shell = new GroovyShell()

badges = shell.parse(new File('./generate-badges.groovy').text)
String slingDir = args[0]

println 'Aggregator Table Generation!'
println '-------------------------'

println "Updating aggregator tables in ${slingDir}/aggregator"

docsDir = new File(slingDir + '/aggregator/docs')
groupsDir = new File(docsDir, 'groups')
statusDir = new File(docsDir, 'status')

println 'Deleting old groups and status folders...'
boolean result = groupsDir.deleteDir()
assert result
result = statusDir.deleteDir()
assert result

println 'Re-creating groups and status folders...'
result = groupsDir.mkdirs()
assert result
result = statusDir.mkdirs()
assert result

println 'Overwriting module file...'
modulesFile = new File(docsDir, 'modules.md')
modulesFile.text = "[Apache Sling](https://sling.apache.org) > [Aggregator](https://github.com/apache/sling-aggregator/) > Modules\n# Modules\n\n| Module | Description | Module&nbsp;Status | Pull&nbsp;Requests |\n|---    |---    |---        |---|"

println 'Overwriting module parent info file...'
modulesParentInfoFile = new File(docsDir, 'modules-parent-info.md')
modulesParentInfoFile.text = "[Apache Sling](https://sling.apache.org) > [Aggregator](https://github.com/apache/sling-aggregator/) > Modules with Parent Info\n# Modules\n\n| Module | Version | Parent | Java Version | Java CI | Flags|\n|--- |--- |--- |--- |--- |--- |"

println 'Loading manifest...'
manifest = new XmlParser().parseText(new File('./default.xml').text)
manifest.project.@path.each { path ->
    addRepo(new File(slingDir, path))
}
println 'Aggregator tables updated!'

void addRepo(File repoFolder) {
    println "Adding badges for ${repoFolder.name}..."

    Map project = badges.parseMavenPom(repoFolder, new File('./'))
    if (project != null) {
        project.badges = badges.calculateBadges(project).join('')

        writeProject(project)
    } else {
        println 'No pom.xml found, skipping...'
    }
}

void writeProject(Map project) {
    println 'Writing project...'
    String projectStr = "| [${project.name}](https://github.com/apache/sling-${project.folder}) <br/> <small>([${project.artifactId}](https://central.sonatype.com/search?namespace=org.apache.sling&name=${project.artifactId}))</small> | ${project.description} | ${project.badges} | &#32;[![Pull Requests](https://img.shields.io/github/issues-pr/apache/sling-${project.folder}.svg)](https://github.com/apache/sling-${project.folder}/pulls) |"
    String projectParentInfoStr = "| [${project.name}](https://github.com/apache/sling-${project.folder})|${project.projectVersion} | ${project.parentVersion} | ${project.javaVersion} | ${project.buildJdks} | ${project.contrib ? 'contrib' : ''} ${project.deprecated ? 'deprecated' : ''}"

    if (project.group) {
        println "Adding to group file ${project.group}"
        File groupFile = new File(groupsDir, "${project.group}.md")
        if (!groupFile.exists()) {
            println "Creating group file for ${project.group}"
            groupFile.createNewFile()
            groupFile << "[Apache Sling](https://sling.apache.org) > [Aggregator](https://github.com/apache/sling-aggregator/) > [Modules](https://github.com/apache/sling-aggregator/blob/master/docs/modules.md) > ${project.group}\n# ${project.group} Modules\n\n| Module | Description | Module&nbsp;Status | Pull&nbsp;Requests |\n|---    |---    |---    |---    |"
        }
        groupFile << '\n' << projectStr
    }

    if (project.deprecated) {
        println 'Adding to deprecated status file'
        File statusFile = new File(statusDir, 'deprecated.md')
        if (!statusFile.exists()) {
            println 'Creating depreacated status file'
            statusFile.createNewFile()
            statusFile << '[Apache Sling](https://sling.apache.org) > [Aggregator](https://github.com/apache/sling-aggregator/) > [Modules](https://github.com/apache/sling-aggregator/blob/master/docs/modules.md) > Deprecated\n# Deprecated Modules\n\n| Module | Description | Module&nbsp;Status | Pull&nbsp;Requests |\n|---    |---    |---    |---    |'
        }
        statusFile << '\n' << projectStr
    }

    if (project.contrib) {
        println 'Adding to contrib status file'
        File statusFile = new File(statusDir, 'contrib.md')
        if (!statusFile.exists()) {
            println 'Creating contrib status file'
            statusFile.createNewFile()
            statusFile << '[Apache Sling](https://sling.apache.org) > [Aggregator](https://github.com/apache/sling-aggregator/) > [Modules](https://github.com/apache/sling-aggregator/blob/master/docs/modules.md) > Contrib\n# Contrib Modules\n\n| Module | Description | Module&nbsp;Status | Pull&nbsp;Requests |\n|---    |---    |---    |---    |'
        }
        statusFile << '\n' << projectStr
    }
    modulesFile << '\n' << projectStr
    modulesParentInfoFile << '\n' << projectParentInfoStr
}
