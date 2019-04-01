/*
 * Copyright (c) 2018-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.ci.jenkins.jira

import org.sonatype.nexus.ci.jenkins.http.SonatypeHTTPBuilder
import org.sonatype.nexus.ci.jenkins.bitbucket.PolicyEvaluationResult
import org.sonatype.nexus.ci.jenkins.util.JiraFieldMappingUtil
import spock.lang.Ignore
import spock.lang.Specification

class JiraClientTest
    extends Specification
{
  //private static final String port = "59454" //for Charles Proxy
  private static final String port = "8080"

  def http
  JiraClient client

  def setup() {
    http = Mock(SonatypeHTTPBuilder)
    client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', System.out, true)
    client.http = http
  }

  def 'lookupJiraTickets has correct url - with all params'() {
    def url

    when:
    client.lookupJiraTickets("JIRAIQ", "Done", "IQ Application", "test")

    then:
    1 * http.get(_, _) >> { args -> url = args[0]}

    and:
    url == "http://localhost:${port}/rest/api/2/search?jql=project+%3D+%22JIRAIQ%22+AND+status+%21%3D+%22Done%22+AND+%22IQ+Application%22+%7E+%22test%22"
  }

  def 'lookupJiraTickets has correct url - without application field'() {
    def url

    when:
    client.lookupJiraTickets("JIRAIQ", "Done", null, null)

    then:
    1 * http.get(_, _) >> { args -> url = args[0]}

    and:
    url == "http://localhost:${port}/rest/api/2/search?jql=project+%3D+%22JIRAIQ%22+AND+status+%21%3D+%22Done%22"
  }

  def 'lookupJiraTickets has correct url - without transition status'() {
    def url

    when:
    client.lookupJiraTickets("JIRAIQ", null, null, null)

    then:
    1 * http.get(_, _) >> { args -> url = args[0]}

    and:
    url == "http://localhost:${port}/rest/api/2/search?jql=project+%3D+%22JIRAIQ%22"
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Get All Tickets'() {
    setup:
      def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', System.out, true)
      def resp = client.lookupJiraTickets("JIRAIQ", "Done", "IQ Application", "aaaaaaa-testidegrandfathering")

    expect:
      resp != null
      resp.issues.size > 0
      resp.issues[0].key != null
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Get All Custom Fields'() {
    setup:
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', System.out, true)
    def resp = client.lookupCustomFields()

    expect:
    resp != null
    resp.size > 0
    resp[0].name != null
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Get Project Ticket Fields Metadata'() {
    setup:
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', System.out, true)
    def resp = client.lookupMetadataConfigurationForCreateIssue("JIRAIQ", "Task")

    expect:
    resp != null
    resp.expand == "projects"
    resp.projects.size == 1
    resp.projects[0].name == "Jira IQ"
    resp.projects[0].issuetypes.size == 1
    resp.projects[0].issuetypes[0].name == "Task"
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Create Ticket'() {
    setup:
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', System.out, true)

    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(null, client, null, System.out)
    jiraFieldMappingUtil.applicationCustomFieldName = "IQ Application"
    jiraFieldMappingUtil.organizationCustomFieldName = "IQ Organization"
    jiraFieldMappingUtil.violationIdCustomFieldName = "Finding ID"
    jiraFieldMappingUtil.violationDetectDateCustomFieldName = "Detect Date"
    jiraFieldMappingUtil.lastScanDateCustomFieldName = "Last Scan Date"
    jiraFieldMappingUtil.scanTypeCustomFieldName = "Scan Type"
    jiraFieldMappingUtil.toolNameCustomFieldName = "Tool Name"
    jiraFieldMappingUtil.findingTemplateCustomFieldName = "Finding Template"

    jiraFieldMappingUtil.projectKey = "JIRAIQ"
    jiraFieldMappingUtil.issueTypeName = "Bug"
    jiraFieldMappingUtil.subTaskIssueTypeName = "Sub-task"
    jiraFieldMappingUtil.priorityName = "Low"

    //Pass-Through Custom Field Values
    jiraFieldMappingUtil.scanTypeCustomFieldValue = "CI"
    jiraFieldMappingUtil.toolNameCustomFieldValue = "Nexus IQ"
    jiraFieldMappingUtil.findingTemplateCustomFieldValue = "NA"

    jiraFieldMappingUtil.mapCustomFieldNamesToIds()

    def resp = client.createIssue(jiraFieldMappingUtil,
                                  "Sonatype IQ Server SECURITY-HIGH Policy Violation",
                                  "CVE-2019-1234",
                                  "SonatypeIQ:IQServerAppId:scanIQ",
                                  "1",
                                  "SONATYPEIQ-APPID-COMPONENTID-SVCODE",
                                  "aaaaaaa-testidegrandfathering",
                                  "test org",
                                  null,
                                  null,
                                  null,
                                  null,
                                  "some-sha-value")

    expect:
    resp != null
    resp.key != null
  }

  //@Ignore
  def 'helper test to verify interaction with Jira Server - Create Task and SubTask'() {
    setup:
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', System.out, true)

    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(null, client, null, System.out)
    jiraFieldMappingUtil.applicationCustomFieldName = "IQ Application"
    jiraFieldMappingUtil.organizationCustomFieldName = "IQ Organization"
    jiraFieldMappingUtil.violationIdCustomFieldName = "Finding ID"
    jiraFieldMappingUtil.violationDetectDateCustomFieldName = "Detect Date"
    jiraFieldMappingUtil.lastScanDateCustomFieldName = "Last Scan Date"
    jiraFieldMappingUtil.scanTypeCustomFieldName = "Scan Type"
    jiraFieldMappingUtil.toolNameCustomFieldName = "Tool Name"
    jiraFieldMappingUtil.findingTemplateCustomFieldName = "Finding Template"

    jiraFieldMappingUtil.projectKey = "JIRAIQ"
    jiraFieldMappingUtil.issueTypeName = "Bug"
    jiraFieldMappingUtil.subTaskIssueTypeName = "Sub-task"
    jiraFieldMappingUtil.priorityName = "Low"

    //Pass-Through Custom Field Values
    jiraFieldMappingUtil.scanTypeCustomFieldValue = "CI"
    jiraFieldMappingUtil.toolNameCustomFieldValue = "Nexus IQ"
    jiraFieldMappingUtil.findingTemplateCustomFieldValue = "NA"

    jiraFieldMappingUtil.mapCustomFieldNamesToIds()

    def resp = client.createIssue(jiraFieldMappingUtil,
                                  "Sonatype IQ Server SECURITY-HIGH Policy Violation",
                                  "CVE-2019-1234",
                                  "SonatypeIQ:IQServerAppId:scanIQ",
                                  "1",
                                  "SONATYPEIQ-APPID-COMPONENTID-SVCODE",
                                  "aaaaaaa-testidegrandfathering",
                                  "test org",
                                  null,
                                  null,
                                  null,
                                  null,
                                  "some-sha-value")


    //TODO: creating a subtask = https://developer.atlassian.com/server/jira/platform/jira-rest-api-examples/#creating-a-sub-task
    def resp2 = client.createIssue(jiraFieldMappingUtil,
                                  "Sonatype IQ Server SECURITY-HIGH Policy Violation",
                                  "CVE-2019-1234",
                                  "SonatypeIQ:IQServerAppId:scanIQ",
                                  "1",
                                  "SONATYPEIQ-APPID-COMPONENTID-SVCODE",
                                  "aaaaaaa-testidegrandfathering",
                                  "test org",
                                  null,
                                  null,
                                  null,
                                  null,
                                  "some-sha-value")

    expect:
    resp != null
    resp.key != null
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Edit Task - Update Last Scan Time'() {
    setup:
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', System.out, true)

    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(null, client, null, System.out)
    jiraFieldMappingUtil.lastScanDateCustomFieldName = "Last Scan Date"

    jiraFieldMappingUtil.mapCustomFieldNamesToIds()

    String ticketNumber = "JIRAIQ-156"

    def resp = client.updateIssueScanDate(jiraFieldMappingUtil, ticketNumber)

    expect:
    resp == null
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Close Ticket'() {
    setup:
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', System.out, true)
    def resp = client.closeTicket("10772", "Done")

    expect:
    resp == null
  }
}
