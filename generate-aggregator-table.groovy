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

import groovy.io.FileType
import groovy.util.XmlParser
import groovy.util.Node

if(args.length == 0){
    println "Please provide the Sling Directory: groovy generate-aggregator-table.groovy [SLING_DIR]"
    System.exit(1)
}

def slingDir = args[0]

println "Aggregator Table Generation!"
println "-------------------------"

println "Updating aggregator tables in " + slingDir + "/aggregator"

def docsDir = new File(slingDir + "/aggregator/docs")
groupsDir = new File(docsDir, "groups")
statusDir = new File(docsDir, "status")

println "Loading manifest..."
manifest = new XmlParser().parseText(new File(slingDir+"/aggregator/default.xml").text);
assert manifest instanceof Node

println "Reading status lists..."
deprecated = new File(slingDir+"/aggregator/deprecated-projects.txt").text.split("\\n")
assert deprecated
contrib = new File(slingDir+"/aggregator/contrib-projects.txt").text.split("\\n")
assert contrib

println "Deleting old groups and status folders..."
def result = groupsDir.deleteDir()
assert result
result = statusDir.deleteDir()
assert result

println "Re-creating groups and status folders..."
result = groupsDir.mkdirs();
assert result
result = statusDir.mkdirs();
assert result

println "Overwriting module file..."
modulesFile = new File(docsDir,"modules.md")
modulesFile.text = "[Apache Sling](https://sling.apache.org) > [Aggregator](https://github.com/apache/sling-aggregator/) > Modules\n# Modules\n\n| Module | Description | Module&nbsp;Status | Pull&nbsp;Requests |\n|---    |---    |---        |---|"

new File(slingDir).eachFile (FileType.DIRECTORIES) { folder ->
    addRepo(folder, manifest)
}
println "Aggregator tables updated!"

void addRepo(File repoFolder, Node manifest) {
    
    def pomFile = new File(repoFolder, "pom.xml")
    def repoName = repoFolder.getName()
    
    println "Fetching from " + repoName + "..."
    
    
    if (pomFile.exists()){
        println "Parsing pom.xml..."
        def pom = new XmlParser().parseText(pomFile.text) 

        assert pom instanceof Node 
        
        def project = [:]
        project['artifactId'] = pom.artifactId.text()
        project['name'] = pom.name.text()
        project['description'] = pom.description.text().replace("\n"," ")
        project['group'] = getProjectGroup(repoName)
        project['folder'] = repoName
        
        if (contrib.contains(repoName)) {
            println "Setting project status to contrib..."
            project['status'] = 'contrib'
        }
        if (deprecated.contains(repoName)) {
            println "Setting project status to deprecated..."
            project['status'] = 'deprecated'
        }
        
        calculateBadges(project)
        
        writeProject(project)
        
    } else {
        println "No pom.xml found, skipping..."
    }
}

void calculateBadges(Map project){
    def badges = [];
    if(getStatus("https://builds.apache.org/job/Sling/job/sling-"+project.folder+"/") != 404) {
        println "Adding Build Status badge..."
        badges.push("&#32;[![Build Status](https://builds.apache.org/buildStatus/icon?job=Sling/sling-"+project.folder+"/master)](https://builds.apache.org/job/Sling/job/sling-"+project.folder+"/job/master)")
    }
    if(responseValid("https://img.shields.io/jenkins/t/https/builds.apache.org/job/Sling/job/sling-"+project.folder+"/job/master.svg")){
        println "Adding test badge..."
        badges.push("&#32;[![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/job/Sling/job/sling-"+project.folder+"/job/master.svg?longCache=true)](https://builds.apache.org/job/Sling/job/sling-"+project.folder+"/job/master/test_results_analyzer/)")
    }
    if(responseValid("https://img.shields.io/jenkins/c/https/builds.apache.org/job/Sling/job/sling-"+project.folder+"/job/master.svg")){
        println "Adding coverage badge..."
        badges.push("&#32;[![Coverage Status](https://img.shields.io/jenkins/c/https/builds.apache.org/job/Sling/job/sling-"+project.folder+"/job/master.svg?longCache=true)]((https://builds.apache.org/job/Sling/job/sling-"+project.folder+"/job/master/)")
    }
    if(getStatus("https://www.javadoc.io/badge/org.apache.sling/"+project.folder+".svg") != 404) {
        println "Adding Build Status badge..."
        badges.push("&#32;[![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/"+project.folder+".svg)](https://www.javadoc.io/doc/org.apache.sling/"+project.folder+")")
    }
    if(responseValid("https://maven-badges.herokuapp.com/maven-central/org.apache.sling/"+project.artifactId+"/badge.svg")){
        println "Adding Maven release badge..."
        badges.push("&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/"+project.artifactId+"/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22"+project.artifactId+"%22)")
    }
    if (project.status) {
        println "Adding status badge..."
        badges.push("&#32;[!["+project.status+"](https://sling.apache.org/badges/status-"+project.status+".svg)](https://github.com/apache/sling-aggregator/blob/master/docs/status/"+project.status+".md)")
    }
    if (project.group) {
        println "Adding group badge..."
        badges.push("&#32;[!["+project.group+"](https://sling.apache.org/badges/group-"+project.group+".svg)](https://github.com/apache/sling-aggregator/blob/master/docs/group/"+project.group+".md)")
    }
    project.badges = badges.join('');
}

String getProjectGroup(String repoName) {
    def groupNode = projectGroup = manifest.project.find{
        it.@path == repoName
    }
    if (groupNode != null) {
        def groupName = groupNode['@groups']
        if (groupName != null) {
            println "Using Group: " + groupName
            return groupName
        } else {
            return ""
        }
    } else {
        return "";
    }
}

int getStatus(String url) {
    def get = new URL(url).openConnection()
    get.setRequestProperty('User-Agent', 'curl/7.35.0')
    def rc = get.getResponseCode()
    println "Retrieved status "+rc+" from "+url
    return rc;
}

boolean responseValid(String url){
    def get = new URL(url).openConnection()
    get.setRequestProperty('User-Agent', 'curl/7.35.0')
    def rc = get.getResponseCode()
    if(rc == 200){
        def text = get.getInputStream().getText()
        if(text.contains('inaccessible') || text.contains('not found') || text.contains('invalid') || text.contains('unknown')) {
            println "Retrieved invalid response from "+url
            return false
        }
        return true
    }
    println "Retrieved invalid response code "+rc+" from "+url
    return false
}

void writeProject(Map project) {
    println "Writing project..."
    def projectStr = "| ["+project.name+"](https://github.com/apache/sling-"+project.folder+") <br/> <small>(["+project.artifactId+"](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22"+project.artifactId+"D%22))</small> | "+project.description+" | "
    projectStr += project.badges
    projectStr += " | &#32;[![Pull Requests](https://img.shields.io/github/issues-pr/apache/sling-"+project.folder+".svg)](https://github.com/apache/sling-"+project.folder+"/pulls) |"
    
    if (project.group) {
        println "Adding to group file " + project.group
        def groupFile = new File(groupsDir,project.group+".md")
        if(!groupFile.exists()){
            println "Creating group file for " + project.group
            groupFile.createNewFile()
            groupFile << "[Apache Sling](https://sling.apache.org) > [Aggregator](https://github.com/apache/sling-aggregator/) > [Modules](https://github.com/apache/sling-aggregator/blob/master/docs/modules.md) > "+project.group+"\n# "+project.group+" Modules\n\n| Module | Description | Module&nbsp;Status | Pull&nbsp;Requests |\n|---    |---    |---    |---    |"
        }
        groupFile << "\n" << projectStr
    }
    
    if (project.status) {
        println "Adding to status file " + project.status
        def statusFile = new File(statusDir,project.status+".md")
        if(!statusFile.exists()){
            println "Creating status file for " + project.status
            statusFile.createNewFile()
            statusFile << "[Apache Sling](https://sling.apache.org) > [Aggregator](https://github.com/apache/sling-aggregator/) > [Modules](https://github.com/apache/sling-aggregator/blob/master/docs/modules.md) > "+project.status+"\n# "+project.status+" Modules\n\n| Module | Description | Module&nbsp;Status | Pull&nbsp;Requests |\n|---    |---    |---    |---    |"
        }
        statusFile << "\n" << projectStr
    }
    modulesFile << "\n" << projectStr
}
