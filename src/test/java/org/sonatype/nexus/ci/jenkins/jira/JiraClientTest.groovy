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

import hudson.model.Run
import hudson.model.TaskListener
import org.sonatype.nexus.ci.jenkins.http.SonatypeHTTPBuilder
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification
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

  boolean verboseLogging = true
  //def mockLogger = Mock(PrintStream)
  def mockLogger = System.out
  def mockListener = Mock(TaskListener)
  def mockRun = Mock(Run)

  JiraNotification jiraNotificationCreateParentTicketTest

  def setup() {
    http = Mock(SonatypeHTTPBuilder)
    client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', mockLogger, verboseLogging)
    client.http = http

    mockListener.getLogger() >> mockLogger
    mockRun.getEnvironment(_) >> [:]

    jiraNotificationCreateParentTicketTest = new JiraNotification(true,
                                                                  verboseLogging,
                                                                  'JIRAIQ',
                                                                  "Bug",
                                                                  "Sub-task",
                                                                  "Low",
                                                                  true,
                                                                  true,
                                                                  "Done",
                                                                  "IQ Application",
                                                                  "IQ Organization",
                                                                  "Scan Stage",
                                                                  "Finding ID",
                                                                  "Detect Date",
                                                                  "Last Scan Date",
                                                                  "Severity",
                                                                  "CVE Code",
                                                                  "CVSS",
                                                                  "License",
                                                                  null,
                                                                  false,
                                                                  false,
                                                                  null,
                                                                  null,
                                                                  [
                                                                          [ customFieldName: 'Random Number', customFieldValue: '17'],
                                                                          [ customFieldName: 'Scan Type', customFieldValue: 'SCA'],
                                                                          [ customFieldName: 'Finding Template', customFieldValue: 'NA'],
                                                                          [ customFieldName: 'Tool Name', customFieldValue: 'Nexus IQ']
                                                                  ])
  }

  def 'lookupJiraTickets has correct url - with all params'() {
    def url, requestBody

    when:
      client.lookupJiraTickets("JIRAIQ", "Done", "IQ Application", "test", "customfield_10300", 0)

    then:
      1 * http.post(_, _, _) >> { args ->
        url = args[0]
        requestBody = args[1]
      }

    and:
      url != null
      url == "http://localhost:${port}/rest/api/2/search"
      requestBody != null
      requestBody == [jql:"project = JIRAIQ AND status != \"Done\" AND \"IQ Application\" ~ \"test\" ORDER BY key", fields:["id", "key", "issuetype", "summary", "status", "customfield_10300"], startAt:0, maxResults:50]
  }

  def 'lookupJiraTickets has correct url - without application field'() {
    def url, requestBody

    when:
      client.lookupJiraTickets("JIRAIQ", "Done", null, null, "customfield_10300", 0)

    then:
      1 * http.post(_, _, _) >> { args ->
        url = args[0]
        requestBody = args[1]
      }

    and:
      url != null
      url == "http://localhost:${port}/rest/api/2/search"
      requestBody != null
      requestBody == [jql:"project = JIRAIQ AND status != \"Done\" ORDER BY key", fields:["id", "key", "issuetype", "summary", "status", "customfield_10300"], startAt:0, maxResults:50]
  }

  def 'lookupJiraTickets has correct url - without transition status'() {
    def url, requestBody

    when:
    client.lookupJiraTickets("JIRAIQ", null, null, null, null, 0)

    then:
      1 * http.post(_, _, _) >> { args ->
        url = args[0]
        requestBody = args[1]
      }

    and:
      url != null
      url == "http://localhost:${port}/rest/api/2/search"
      requestBody != null
      requestBody == [jql:"project = JIRAIQ ORDER BY key", fields:["id", "key", "issuetype", "summary", "status"], startAt:0, maxResults:50]
  }

  /*
  ****************************************************************************************************************************************************
  *                                                     Integration Tests                                                                            *
  ****************************************************************************************************************************************************
   */

  /**
   * https://developer.atlassian.com/server/jira/platform/jira-rest-api-examples/#searching-for-issues-examples
   *
   * curl -s http://localhost:8080/rest/api/2/search?jql=project+%3D+%22JIRAIQ%22+AND+status+%21%3D+%22Done%22+AND+%22IQ+Application%22+%7E+%22aaaaaaa-testidegrandfathering%22 | jq-osx-amd64 .issues[].key
   *   maxResults: 50
   *   startAt: 0
   *   totla: 71
   *
   * @return
   */
  @Ignore
  def 'helper test to verify interaction with Jira Server - Get Not-Done Tickets for Project and App'() {
    setup:
      def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', mockLogger, verboseLogging)
      def resp = client.lookupJiraTickets("JIRAIQ",
                                          "Done",
                                          "IQ Application",
                                          "aaaaaaa-testidegrandfathering",
                                          "customfield_10300",
                                          0)

    expect:
      resp != null
      resp.issues.size > 0
      resp.issues[0].key != null
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Get All Custom Fields'() {
    setup:
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', mockLogger, verboseLogging)
    def resp = client.lookupCustomFields()

    expect:
    resp != null
    resp.size > 0
    resp[0].name != null
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Get Project Ticket Fields Metadata'() {
    setup:
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', mockLogger, verboseLogging)
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
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', mockLogger, verboseLogging)

    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCreateParentTicketTest, client, mockRun.getEnvironment(mockListener), mockLogger)

    def resp = client.createIssue(jiraFieldMappingUtil,
                                  "Sonatype IQ Server SECURITY-HIGH Policy Violation",
                                  "CVE-2019-123${detailCounter}",
                                  "SonatypeIQ:IQServerAppId:scanIQ",
                                  "1",
                                  "SONATYPEIQ-APPID-COMPONENTID-SVCODE",
                                  "aaaaaaa-testidegrandfathering",
                                  "test org",
                                  "Build",
                                  "Low",
                                  "CVE-2019-123${detailCounter}",
                                  2.3,
                                  "some-sha-value")

    expect:
    resp != null
    resp.key != null

    where:
      detailCounter << "4"
      //detailCounter << [1, 2, 3, 4, 5, 6, 7, 8, 9]
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Create Task and SubTask'() {
    setup:
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', mockLogger, verboseLogging)

    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCreateParentTicketTest, client, mockRun.getEnvironment(mockListener), mockLogger)

    def resp = client.createIssue(jiraFieldMappingUtil,
                                  "Component ABC has Policy Violations",
                                  "Policy Violations are bad",
                                  "SonatypeIQ:IQServerAppId:scanIQ",
                                  "1",
                                  "SONATYPEIQ-APPID-COMPONENTID",
                                  "aaaaaaa-testidegrandfathering",
                                  "test org",
                                  "Build",
                                  "Low",
                                  "CVS-2019-1234",
                                  null,
                                  "some-parent-sha-value")

    def resp2 = client.createSubTask(jiraFieldMappingUtil,
                                     resp.key,
                                    "Sonatype IQ Server SECURITY-HIGH Policy Violation",
                                    "CVE-2019-1234",
                                    "SonatypeIQ:IQServerAppId:scanIQ",
                                    "1",
                                    "SONATYPEIQ-APPID-COMPONENTID-SVCODE",
                                    "aaaaaaa-testidegrandfathering",
                                    "test org",
                                    "Build",
                                    "Low",
                                    "CVE-2019-1234",
                                    2.4,
                                    "some-child-sha-value")

    expect:
    resp != null
    resp.key != null
    resp2 != null
    resp2.key != null
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Edit Task - Update Last Scan Time'() {
    setup:
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', mockLogger, verboseLogging)

    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCreateParentTicketTest, client, mockRun.getEnvironment(mockListener), mockLogger)

    String ticketNumber = "JIRAIQ-156"

    def resp = client.updateIssueScanDate(jiraFieldMappingUtil, ticketNumber)

    expect:
    resp == null
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Close Ticket'() {
    setup:
    def client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', mockLogger, verboseLogging)
    def resp = client.closeTicket("10772", "Done")

    expect:
    resp == null
  }
}
