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

/* groovylint-disable JavaIoPackageAccess */

ArrayList calculateBadges(Map project) {
    def badges = []
    if (getStatus("https://ci-builds.apache.org/job/Sling/job/modules/job/sling-${project.folder}/job/master/badge/icon") != 404) {
        println 'Adding build status badge...'
        badges.add("&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-${project.folder}/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-${project.folder}/job/master/)")
    }
    if (responseValid("https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-${project.folder}/job/master/")) {
        println 'Adding test status badge...'
        badges.add("&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-${project.folder}/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-${project.folder}/job/master/test/?width=800&height=600)")
    }
    if (responseValid("https://sonarcloud.io/api/project_badges/measure?project=apache_sling-${project.folder}&metric=coverage")) {
        println 'Adding coverage status badge...'
        badges.add("&#32;[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-${project.folder}&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-${project.folder})")
    }
    if (responseValid("https://sonarcloud.io/api/project_badges/measure?project=apache_sling-${project.folder}&metric=alert_status")) {
        println 'Adding quality status badge...'
        badges.add("&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-${project.folder}&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-${project.folder})")
    }
    if (project.artifactId) {
        if (getStatus("https://www.javadoc.io/badge/org.apache.sling/${project.artifactId}.svg") != 404) {
            println 'Adding JavaDoc badge...'
            badges.add("&#32;[![JavaDoc](https://www.javadoc.io/badge/org.apache.sling/${project.artifactId}.svg)](https://www.javadoc.io/doc/org.apache.sling/${project.artifactId})")
        }
        if (responseValid("https://maven-badges.herokuapp.com/maven-central/org.apache.sling/${project.artifactId}/badge.svg")) {
            println 'Adding Maven release badge...'
            badges.add("&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/${project.artifactId}/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22${project.artifactId}%22)")
        }
    }
    if (project.contrib) {
        println 'Adding contrib status badge...'
        badges.add('&#32;[![Contrib](https://sling.apache.org/badges/status-contrib.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/status/contrib.md)')
    }
    if (project.deprecated) {
        println 'Adding deprecated status badge...'
        badges.add('&#32;[![Deprecated](https://sling.apache.org/badges/status-deprecated.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/status/deprecated.md)')
    }
    if (project.group) {
        println 'Adding group badge...'
        badges.add("&#32;[![${project.group}](https://sling.apache.org/badges/group-${project.group}.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/${project.group}.md)")
    }
    return badges
}

String getProjectGroup(String repoName, File aggregatorDir) {
    println 'Loading manifest...'

    manifest = new XmlParser().parseText(new File(aggregatorDir, 'default.xml').text)

    def groupNode = projectGroup = manifest.project.find {
        it.@path == repoName
    }
    if (groupNode != null) {
        def groupName = groupNode['@groups']
        if (groupName != null) {
            println "Using Group: ${groupName}"
            return groupName
        }
    }
    return ''
}

int getStatus(String url) {
    def get = new URL(url).openConnection()
    get.setRequestProperty('User-Agent', 'curl/7.35.0')
    def rc = get.getResponseCode()
    println 'Retrieved status ' + rc + ' from ' + url
    return rc
}

Map parseMavenPom(File projectFolder, File aggregatorDir) {
    File pomFile = new File(projectFolder, 'pom.xml')
    String repoName = projectFolder.name

    println "Fetching from ${projectFolder}..."

    println 'Reading status lists...'
    String[] deprecated = new File(aggregatorDir, 'deprecated-projects.txt').text.split('\\n')
    assert deprecated
    String[] contrib = new File(aggregatorDir, 'contrib-projects.txt').text.split('\\n')
    assert contrib

    if (pomFile.exists()) {
        println 'Parsing pom.xml...'
        Node pom = new XmlParser().parseText(pomFile.text)

        Map project = [:]
        project['artifactId'] = pom.artifactId.text()
        project['name'] = pom.name.text()
        project['description'] = pom.description.text().replace('\n', ' ')
        project['group'] = getProjectGroup(repoName, aggregatorDir)
        project['folder'] = repoName
        if (contrib.contains(repoName)) {
            println 'Setting project status to contrib...'
            project['contrib'] = true
        }
        if (deprecated.contains(repoName)) {
            println 'Setting project status to deprecated...'
            project['deprecated'] = true
        }
        return project
    }
    return null
}

boolean responseValid(String url) {
    def get = new URL(url).openConnection()
    get.setRequestProperty('User-Agent', 'curl/7.35.0')
    int rc = get.responseCode
    if (rc == 200) {
        println "Retrieved valid response code ${rc} from ${url}"
        String text = get.inputStream.text
        if (text.contains('inaccessible') || text.contains('not found') || text.contains('not been found')
            || text.contains('invalid') || text.contains('unknown') || text.contains('no tests found')) {
            println "Retrieved invalid response from ${url}"
            return false
            }
        return true
    }
    println "Retrieved invalid response code ${rc} from ${url}"
    return false
}
