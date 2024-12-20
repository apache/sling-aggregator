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
import groovy.transform.SourceURI
import groovy.xml.XmlParser
import groovy.util.Node

import java.nio.file.Paths

if (args.length == 0) {
    println 'Generates the badges for a project.'
    println 'Please provide the Project Directory: groovy generate-project-badges.groovy [PROJECT_DIR]'
    System.exit(1)
}

GroovyShell shell = new GroovyShell()

@SourceURI
URI sourceUri

File getReadMe(File projectDir) {
    def readmes = []
    projectDir.eachFile(FileType.FILES, {
        if (it.path =~ ~/(?i)readme.md/) {
            readmes.push(it.name)
        }
    })
    return new File(projectDir, readmes[0])
}

def genBadges = shell.parse(new File(Paths.get(sourceUri).parent.toString(), './generate-badges.groovy').text)
String projectDir = args[0]
projectDir = new File(projectDir).getCanonicalPath()

println '\n\nGenerate Project Badges!'
println '-------------------------'

println "Updating badges in ${projectDir}"

Map project = genBadges.parseMavenPom(new File(projectDir), new File(Paths.get(sourceUri).parent.toString()))
if (project == null) {
    project = [:]
    project.folder = new File(projectDir).name
}
def badges = genBadges.calculateBadges(project)
badges.add(' [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)')

File readme = getReadMe(new File(projectDir))
def lines = readme.readLines()

println "Updating ${readme}"
lines[3..(lines.size() - 1)].join('\n')
readme.newWriter().withWriter { w ->
        w << "[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)\n\n${badges.join('')}\n${lines[3..(lines.size() - 1)].join('\n')}\n"
}

println 'Update complete!'

