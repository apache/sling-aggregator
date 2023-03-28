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
import groovy.cli.commons.CliBuilder
import groovy.yaml.YamlBuilder
import groovy.yaml.YamlSlurper

def cli = new CliBuilder(usage: 'update-asf-yaml.groovy target-repo')

def options = cli.parse(args)
if ( !options || !options.arguments() || !options.arguments().size() == 1 ) {
    print cli.usage()
    System.exit 1
}

def targetDir = new File(options.arguments()[0])
if ( !targetDir.isDirectory() ) {
    println "Target directory ${targetDir} does not exist or is not a directory, aborting"
    System.exit 1
}
def targetFile = new File(targetDir, '.asf.yaml')
def asfYaml = [:]
if ( !targetFile.exists() ) {
    println "Target file ${targetFile} does not exist, creating with defaults"
} else {
    println "Target file ${targetFile} exists, merging with defaults"
    def parser = new YamlSlurper()
    asfYaml = parser.parse(targetFile)
}

if  ( ! asfYaml['github'] ) {
    asfYaml['github'] = [:]
}

// set up autolinks ; note: missing merge
// https://cwiki.apache.org/confluence/display/INFRA/Git+-+.asf.yaml+features#Git.asf.yamlfeatures-AutolinksforJira
def autolinks = asfYaml['github']['autolinks']
if ( ! autolinks ) {
    asfYaml['github']['autolink_jira'] = ['SLING', 'OAK', 'JCR', 'JCRVLT', 'INFRA','FELIX', 'MNG']
}

if ( !asfYaml['notifications'] ) {
    asfYaml['notifications'] = [:]
}
// set up jira integration
// https://cwiki.apache.org/confluence/display/INFRA/Git+-+.asf.yaml+features#Git.asf.yamlfeatures-Jiranotificationoptions
def jiraOptions = asfYaml['notifications']['jira_options']
if ( !jiraOptions ) {
    asfYaml['notifications']['jira_options'] = 'link'
}
def builder = new YamlBuilder()
builder(asfYaml)

targetFile << builder
