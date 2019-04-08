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
package org.sonatype.nexus.ci.jenkins.util

import groovy.json.JsonSlurper
import hudson.EnvVars
import hudson.model.Run
import hudson.model.TaskListener
import org.sonatype.nexus.ci.jenkins.jira.JiraClient
import org.sonatype.nexus.ci.jenkins.jira.JiraClientFactory
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification
import spock.lang.Ignore
import spock.lang.Specification

class JiraFieldMappingUtilTest
    extends Specification
{
  //private static final String jiraPort = "59454" //for Charles Proxy
  private static final String jiraPort = "8080"
  //private static final String iqPort = "60359" //for Charles Proxy
  private static final String iqPort = "8060"

  boolean verboseLogging = true
  //def mockLogger = Mock(PrintStream)
  def mockLogger = System.out
  def mockListener = Mock(TaskListener)
  def mockRun = Mock(Run)

  JiraNotification jiraNotificationCustomFieldMapTest
  JiraNotification jiraNotificationMinimalTest

  def setup() {
    mockListener.getLogger() >> mockLogger
    mockRun.getEnvironment(_) >> [:]

    jiraNotificationMinimalTest = new JiraNotification(true,
                                                       false,
                                                       'JIRAIQ',
                                                       "Task",
                                                       null,
                                                       "Low",
                                                       false,
                                                       false,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       false,
                                                       false,
                                                       null,
                                                       null,
                                                       null)

    jiraNotificationCustomFieldMapTest = new JiraNotification(true,
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

  def 'expands arguments'() {
    setup:
      GroovyMock(JiraClientFactory.class, global: true)
      def client = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> client

      EnvVars ev = ['projectKey': 'project']
      jiraNotificationMinimalTest.projectKey = '${projectKey}'

    when:
      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationMinimalTest, client, ev, mockLogger)

    then:
      assert jiraFieldMappingUtil.projectKey == "project"
  }

  def 'validate custom fields names are mapped to ids - app name'() {
    setup:
      GroovyMock(JiraClientFactory.class, global: true)
      def client = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> client

      jiraNotificationMinimalTest.applicationCustomFieldName = "IQ Application"

      List<Map<String, Object>> customFields = [
                  [
                          "id": "customfield_10200",
                          "name": "IQ Application"
                  ]
          ]

    when:
      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationMinimalTest, client, mockRun.getEnvironment(mockListener), mockLogger)

    then:
      1 * client.lookupCustomFields() >> customFields

      assert jiraFieldMappingUtil.getApplicationCustomField().customFieldId == "customfield_10200"
  }

  def 'validate custom fields names are mapped to ids - all fields from jsonslurper'() {
    setup:
      GroovyMock(JiraClientFactory.class, global: true)
      def client = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> client

      def customFields = new JsonSlurper().parse(new File('src/test/resources/jira-custom-fields.json'))

    when:
      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCustomFieldMapTest, client, mockRun.getEnvironment(mockListener), mockLogger)

    then:
      1 * client.lookupCustomFields() >> customFields

      assert jiraFieldMappingUtil.getApplicationCustomField().customFieldId == "customfield_10200"
      assert jiraFieldMappingUtil.getApplicationCustomField().customFieldName == "IQ Application"
      assert jiraFieldMappingUtil.getApplicationCustomField().customFieldType == "string"

      assert jiraFieldMappingUtil.getOrganizationCustomField().customFieldId == "customfield_10201"
      assert jiraFieldMappingUtil.getViolationIdCustomField().customFieldId == "customfield_10300"
      assert jiraFieldMappingUtil.getViolationDetectDateCustomField().customFieldId == "customfield_10502"
      assert jiraFieldMappingUtil.getLastScanDateCustomField().customFieldId == "customfield_10503"

      //TODO: update with the rest of the fields

      assert jiraFieldMappingUtil.getPassthroughCustomField("Scan Type").customFieldId == "customfield_10400"

      assert jiraFieldMappingUtil.getPassthroughCustomField("Tool Name").customFieldId == "customfield_10501"
      assert jiraFieldMappingUtil.getPassthroughCustomField("Tool Name").customFieldValue == "Nexus IQ"
      assert jiraFieldMappingUtil.getPassthroughCustomField("Tool Name").customFieldType == "option"

      assert jiraFieldMappingUtil.getPassthroughCustomField("Finding Template").customFieldId == "customfield_10500"
  }

  def 'validate minimal custom fields names are mapped to stubs and not null pointers - all fields from jsonslurper'() {
    setup:
      GroovyMock(JiraClientFactory.class, global: true)
      def client = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> client

      def customFields = new JsonSlurper().parse(new File('src/test/resources/jira-custom-fields.json'))

    when:
      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationMinimalTest, client, mockRun.getEnvironment(mockListener), mockLogger)

    then:
      1 * client.lookupCustomFields() >> customFields

      assert jiraFieldMappingUtil.getApplicationCustomField().customFieldId == null
      assert jiraFieldMappingUtil.getOrganizationCustomField().customFieldId == null
      assert jiraFieldMappingUtil.getViolationIdCustomField().customFieldId == null
      assert jiraFieldMappingUtil.getViolationDetectDateCustomField().customFieldId == null
      assert jiraFieldMappingUtil.getLastScanDateCustomField().customFieldId == null

      //TODO: update with the rest of the fields
      jiraFieldMappingUtil.getPassthroughCustomField("Tool Name").customFieldId == null
  }

  def 'format date to string'() {
    setup:
    GroovyMock(JiraClientFactory.class, global: true)
    def client = Mock(JiraClient.class)
    JiraClientFactory.getJiraClient(*_) >> client

    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationMinimalTest, client, mockRun.getEnvironment(mockListener), mockLogger)

    def url
    def d = new Date(1318980569908)
    //def d = new Date(2019 - 1900, 3, 4, 12, 53, 13)
    //def tz = TimeZone.getTimeZone('GMT')
    def tz = TimeZone.getTimeZone('Australia/Sydney')
    //def tz = TimeZone.getDefault()

    when:
    url = jiraFieldMappingUtil.formatDateForJira(d, tz)

    then:
    //url != null
    //url == "2011-10-18T23:29:29.908+0000"
    url == "2011-10-19T10:29:29.908+1100"
  }

  def 'format date to string with format override'() {
    setup:
    GroovyMock(JiraClientFactory.class, global: true)
    def client = Mock(JiraClient.class)
    JiraClientFactory.getJiraClient(*_) >> client

    jiraNotificationMinimalTest.jiraDateFormatOverride = "yyyy-MM-dd"
    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationMinimalTest, client, mockRun.getEnvironment(mockListener), mockLogger)

    def url
    def d = new Date(2011 - 1900, 9, 19)

    when:
      url = jiraFieldMappingUtil.formatDateForJira(d)

    then:
      url == "2011-10-19"
  }

  def 'parse string to date'() {
    setup:
    GroovyMock(JiraClientFactory.class, global: true)
    def client = Mock(JiraClient.class)
    JiraClientFactory.getJiraClient(*_) >> client

    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationMinimalTest, client, mockRun.getEnvironment(mockListener), mockLogger)

    //def url = "1969-12-31T19:00:00.000-0500"
    def url = "2011-10-19T10:29:29.908+1100"
    //String url = "2019-04-04T12:53:13.000-0400"
    Date d
    Long millis

    when:
    d = jiraFieldMappingUtil.parseDateForJira(url)
    millis = d.getTime()

    then:
    d != null
    millis.equals(new Long(1318980569908))
  }

  /*
  ****************************************************************************************************************************************************
  *                                                     Integration Tests                                                                            *
  ****************************************************************************************************************************************************
   */

  @Ignore
  def 'helper test to verify interaction with Jira Server - Map Custom Fields'() {
    setup:
      def client = new JiraClient("http://localhost:${jiraPort}", 'admin', 'admin123', System.out, true)

      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCustomFieldMapTest, client, mockRun.getEnvironment(mockListener), mockLogger)

    expect:
      jiraFieldMappingUtil.getApplicationCustomField().customFieldName == "IQ Application"
      jiraFieldMappingUtil.getApplicationCustomField().customFieldId == "customfield_10200"
      jiraFieldMappingUtil.getApplicationCustomField().customFieldType == "string"
  }
}
