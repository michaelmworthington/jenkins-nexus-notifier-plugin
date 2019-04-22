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

import groovy.json.JsonSlurper
import hudson.model.Run
import hudson.model.TaskListener
import org.sonatype.nexus.ci.jenkins.http.SonatypeHTTPBuilder
import org.sonatype.nexus.ci.jenkins.model.ComponentIdentifier
import org.sonatype.nexus.ci.jenkins.model.PolicyViolation
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification
import org.sonatype.nexus.ci.jenkins.util.JiraFieldMappingUtil
import spock.lang.Requires
import spock.lang.Specification

//TODO: Think about removing these or making the JiraNotification sharing better
@SuppressWarnings("GroovyAccessibility")
class JiraClientTest
    extends Specification
{
  //private static final String port = "59454" //for Charles Proxy
  private static final String port = "8080"

  SonatypeHTTPBuilder http
  JiraClient client, integrationTestJiraClient
  def jqlMaxResultsOverride = 50
  def disableJqlFieldFilter = false
  def dryRun = false

  def customFields

  boolean verboseLogging = true
  //def mockLogger = Mock(PrintStream)
  def mockLogger = System.out
  def mockListener = Mock(TaskListener)
  def mockRun = Mock(Run)

  JiraNotification jiraNotificationCreateParentTicketTest

  def setup() {
    http = Mock(SonatypeHTTPBuilder)
    client = new JiraClient("http://localhost:${port}", 'admin', 'admin123', mockLogger, verboseLogging)
    integrationTestJiraClient = Spy(JiraClient, constructorArgs: ["http://localhost:${port}", 'admin', 'admin123', mockLogger, verboseLogging, dryRun, disableJqlFieldFilter, jqlMaxResultsOverride])

    client.http = http

    mockListener.getLogger() >> mockLogger
    mockRun.getEnvironment(_) >> [:]

    customFields = new JsonSlurper().parse(new File('src/test/resources/jira-custom-fields.json'))


    jiraNotificationCreateParentTicketTest = new JiraNotification(true,
                                                                'JIRAIQ',
                                                                "Bug",
                                                                "Sub-task",
                                                                "Low",
                                                                true,
                                                                false,
                                                                false,
                                                                true,
                                                                "Done",
                                                                "License",
                                                                null,
                                                                null,
                                                                verboseLogging,
                                                                false,
                                                                null,
                                                                false,
                                                                5,
                                                                null,
                                                                "IQ Application",
                                                                "IQ Organization",
                                                                "Scan Stage",
                                                                "Finding ID",
                                                                "Detect Date",
                                                                "Last Scan Date",
                                                                "Severity",
                                                                "CVE Code",
                                                                "CVSS",
                                                                "Report Link",
                                                                "Violation Name",
                                                                "Threat Level",
                                                                "Finding Vendor",
                                                                "Finding Library",
                                                                "Finding Version",
                                                                "Finding Classifier",
                                                                "Finding Extension",
                                                                [
                                                                        [ customFieldName: 'Random Number', customFieldValue: '17'],
                                                                        [ customFieldName: 'Scan Type', customFieldValue: 'SCA'],
                                                                        [ customFieldName: 'Finding Template', customFieldValue: 'NA'],
                                                                        [ customFieldName: 'Tool Name', customFieldValue: 'Nexus IQ'],
                                                                        [ customFieldName: 'Scan Stage', customFieldValue: 'Build']
                                                                ])
  }

  def 'lookupJiraTickets has correct url - with all params'() {
    def url, requestBody

    when:
      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCreateParentTicketTest, client, mockRun.getEnvironment(mockListener), mockLogger)
      jiraFieldMappingUtil.getApplicationCustomField().customFieldValue = "test"

      client.lookupJiraTickets(jiraFieldMappingUtil, 0)

    then:
      1 * http.get("${client.serverUrl}/rest/api/2/field", _) >> customFields

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
      jiraNotificationCreateParentTicketTest.applicationCustomFieldName = null
      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCreateParentTicketTest, client, mockRun.getEnvironment(mockListener), mockLogger)

      client.lookupJiraTickets(jiraFieldMappingUtil, 0)

    then:
      1 * http.get("${client.serverUrl}/rest/api/2/field", _) >> customFields

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
      jiraNotificationCreateParentTicketTest.applicationCustomFieldName = null
      jiraNotificationCreateParentTicketTest.jiraTransitionStatus = null
      jiraNotificationCreateParentTicketTest.violationIdCustomFieldName = null
      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCreateParentTicketTest, client, mockRun.getEnvironment(mockListener), mockLogger)

      client.lookupJiraTickets(jiraFieldMappingUtil, 0)

    then:
      1 * http.get("${client.serverUrl}/rest/api/2/field", _) >> customFields

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
  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Get Not-Done Tickets for Project and App'() {
    setup:
      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCreateParentTicketTest, integrationTestJiraClient, mockRun.getEnvironment(mockListener), mockLogger)
      jiraFieldMappingUtil.getApplicationCustomField().customFieldValue = "aaaaaaa-testidegrandfathering"

      def resp = integrationTestJiraClient.lookupJiraTickets(jiraFieldMappingUtil, 0)

    expect:
      resp != null
//      resp.issues.size > 0
//      resp.issues[0].key != null
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Get All Custom Fields'() {
    setup:
    def resp = integrationTestJiraClient.lookupCustomFields()

    expect:
    resp != null
    resp.size > 0
    resp[0].name != null
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Get Project Ticket Fields Metadata'() {
    setup:
    def resp = integrationTestJiraClient.lookupMetadataConfigurationForCreateIssue("JIRAIQ", "Task")

    expect:
    resp != null
    resp.expand == "projects"
    resp.projects.size == 1
    resp.projects[0].name == "Jira IQ"
    resp.projects[0].issuetypes.size == 1
    resp.projects[0].issuetypes[0].name == "Task"
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Create Ticket'() {
    setup:
    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCreateParentTicketTest, integrationTestJiraClient, mockRun.getEnvironment(mockListener), mockLogger)
    jiraFieldMappingUtil.getApplicationCustomField().customFieldValue = "aaaaaaa-testidegrandfathering"
    jiraFieldMappingUtil.getOrganizationCustomField().customFieldValue = "test org"
    jiraFieldMappingUtil.getScanStageCustomField().customFieldValue = "Build"

    def pReportLink = "SonatypeIQ:IQServerAppId:scanIQ"
    ComponentIdentifier componentIdentifier = new ComponentIdentifier([format     : "maven",
                                                                       coordinates: [groupId   : "org.apache.struts",
                                                                                     artifactId: "struts2-core",
                                                                                     version   : "1.2.3"]])
    def policyName = "Security-Low"
    def severity = "Low"
    def policyThreatLevel = 1
    def policyId = "abc123"
    def cveCode = "CVS-2019-123${detailCounter}"
    def cvssScore = 2.4
    def conditionReasonText = "Sonatype IQ Server SECURITY-HIGH Policy Violation"
    def findingFingerprintHash = "some-sha-value"
    def findingFingerprintKey = "SONATYPEIQ-APPID--POLICYID-COMPONENTNAME-SVREASON"
    def fingerprintPrettyPrint = "${componentIdentifier.prettyName} - ${policyName} - ${conditionReasonText}"


    PolicyViolation policyViolationSubTask = new PolicyViolation(reportLink: pReportLink,
                                                                 componentIdentifier: componentIdentifier,
                                                                 policyId: policyId,
                                                                 policyName: policyName,
                                                                 policyThreatLevel: policyThreatLevel,
                                                                 cvssReason: conditionReasonText,
                                                                 cvssScore: cvssScore,
                                                                 cveCode: cveCode,
                                                                 severity: severity,
                                                                 fingerprintPrettyPrint: fingerprintPrettyPrint,
                                                                 fingerprintKey: findingFingerprintKey,
                                                                 fingerprint: findingFingerprintHash)

    def resp = integrationTestJiraClient.createIssue(jiraFieldMappingUtil, policyViolationSubTask)

    expect:
    resp != null
    resp.key != null

    where:
      detailCounter << "4"
      //detailCounter << [1, 2, 3, 4, 5, 6, 7, 8, 9]
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Create Task and SubTask'() {
    setup:
    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCreateParentTicketTest, integrationTestJiraClient, mockRun.getEnvironment(mockListener), mockLogger)
    jiraFieldMappingUtil.getApplicationCustomField().customFieldValue = "aaaaaaa-testidegrandfathering"
    jiraFieldMappingUtil.getOrganizationCustomField().customFieldValue = "test org"
    jiraFieldMappingUtil.getScanStageCustomField().customFieldValue = "Build"

    //The Story Aggregated Component
    def pReportLink = "SonatypeIQ:IQServerAppId:scanIQ"
    ComponentIdentifier componentIdentifier = new ComponentIdentifier([format     : "maven",
                                                                       coordinates: [groupId   : "org.apache.struts",
                                                                                     artifactId: "struts2-core",
                                                                                     version   : "1.2.3"]])
    def componentFingerprintPretty = componentIdentifier.prettyName
    def componentFingerprintKey = "SONATYPEIQ-APPID-COMPONENTID"
    def componentFingerprintHash = "some-parent-sha-value"

    PolicyViolation potentialComponentViolation = new PolicyViolation(reportLink: pReportLink,
                                                                      componentIdentifier: componentIdentifier,
                                                                      fingerprintPrettyPrint: componentFingerprintPretty,
                                                                      fingerprintKey: componentFingerprintKey,
                                                                      fingerprint: componentFingerprintHash)

    //The Sub-Task Finding
    def policyName = "Security-Low"
    def severity = "Low"
    def policyThreatLevel = 1
    def policyId = "abc123"
    def cveCode = "CVS-2019-1234"
    def cvssScore = 2.4
    def conditionReasonText = "Sonatype IQ Server SECURITY-HIGH Policy Violation"
    def findingFingerprintHash = "some-child-sha-value"
    def findingFingerprintKey = "SONATYPEIQ-APPID--POLICYID-COMPONENTNAME-SVREASON"
    def fingerprintPrettyPrint = "${componentIdentifier.prettyName} - ${policyName} - ${conditionReasonText}"

    PolicyViolation policyViolationSubTask = new PolicyViolation(reportLink: pReportLink,
                                                                 componentIdentifier: componentIdentifier,
                                                                 componentFingerprintPrettyPrint: componentFingerprintPretty,
                                                                 componentFingerprintKey: componentFingerprintKey,
                                                                 componentFingerprint: componentFingerprintHash,
                                                                 policyId: policyId,
                                                                 policyName: policyName,
                                                                 policyThreatLevel: policyThreatLevel,
                                                                 cvssReason: conditionReasonText,
                                                                 cvssScore: cvssScore,
                                                                 cveCode: cveCode,
                                                                 severity: severity,
                                                                 fingerprintPrettyPrint: fingerprintPrettyPrint,
                                                                 fingerprintKey: findingFingerprintKey,
                                                                 fingerprint: findingFingerprintHash)

    potentialComponentViolation.addViolationToComponent(policyViolationSubTask)

    def resp = integrationTestJiraClient.createIssue(jiraFieldMappingUtil, potentialComponentViolation)
    def resp2 = integrationTestJiraClient.createSubTask(jiraFieldMappingUtil, resp.key, policyViolationSubTask)

    expect:
    resp != null
    resp.key != null
    resp2 != null
    resp2.key != null
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Edit Task - Update Last Scan Time'() {
    setup:
    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCreateParentTicketTest, integrationTestJiraClient, mockRun.getEnvironment(mockListener), mockLogger)

    String ticketNumber = "JIRAIQ-156"

    def resp = integrationTestJiraClient.updateIssueScanDate(jiraFieldMappingUtil, ticketNumber)

    expect:
    resp == null
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Close Ticket'() {
    setup:
    def resp = integrationTestJiraClient.closeTicket("10772", "Done")

    expect:
    resp == null
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - All Tickets'() {
    setup:
      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCreateParentTicketTest, integrationTestJiraClient, mockRun.getEnvironment(mockListener), mockLogger)
      jiraFieldMappingUtil.getApplicationCustomField().customFieldValue = "aaaaaaa-testidegrandfathering"

    when: 'Get all the tickets'
      def resp = integrationTestJiraClient.lookupJiraTickets(jiraFieldMappingUtil, 0)

    then:
      resp != null
      resp.issues.size > 0
      resp.issues[0].key != null

    when: 'Close all the tickets'
      resp.issues.each {
        integrationTestJiraClient.closeTicket(it.id, "Done")
      }

    then:
      true //todo: validate


  }
}