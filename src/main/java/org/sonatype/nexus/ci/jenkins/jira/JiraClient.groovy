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

import org.sonatype.nexus.ci.jenkins.util.AbstractToolClient

class JiraClient extends AbstractToolClient
{
  JiraClient(String serverUrl, String username, String password, PrintStream logger, final boolean verboseLogging = false) {
    super(serverUrl, username, password, logger, verboseLogging)
  }

  def createIssue(projectKey,
                  issueTypeName,
                  priorityName,
                  description,
                  detail,
                  source,
                  severity,
                  fprint,
                  iqAppExternalId, String iqAppExternalIdCustomFieldId,
                  String iqOrgExternalId, String iqOrgExternalIdCustomFieldId,
                  scanStage, scanStageId,
                  violationDate, violationDateId,
                  lastScanDate, lastScanDateId,
                  severityString, severityId,
                  cveCode, cveCodeId,
                  cvss, cvssId,
                  scanType, scanTypeId,
                  toolName, toolNameId,
                  String violationUniqueId,
                  String violationIdCustomFieldId)
  {
    def url = getCreateIssueRequestUrl(serverUrl)
    def body = getCreateIssueRequestBody(projectKey, issueTypeName, priorityName, description, detail, source, severity, fprint,
                                         iqAppExternalId, iqAppExternalIdCustomFieldId,
                                         iqOrgExternalId,iqOrgExternalIdCustomFieldId,
                                         scanStage, scanStageId,
                                         violationDate, violationDateId,
                                         lastScanDate, lastScanDateId,
                                         severityString, severityId,
                                         cveCode, cveCodeId,
                                         cvss, cvssId,
                                         scanType, scanTypeId,
                                         toolName, toolNameId,
                                         violationUniqueId, violationIdCustomFieldId)
    def headers = getRequestHeaders(username, password)

    http.post(url, body, headers)
  }

  def lookupJiraTickets(String projectKey,
                        String transitionTargetStatus,
                        String applicationCustomFieldName,
                        String iqApplicationExternalId)
  {
    def url = getLookupTicketsForProjectUrl(serverUrl, projectKey, transitionTargetStatus, applicationCustomFieldName, iqApplicationExternalId)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  def lookupCustomFields()
  {
    def url = getLookupCustomFieldsUrl(serverUrl)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  def lookupMetadataConfigurationForCreateIssue(String projectKey, String issueTypeName)
  {
    def url = getLookupMetadataConfigurationForCreateIssueUrl(serverUrl, projectKey, issueTypeName)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  static String lookupCustomFieldId(Object customFields, String fieldName)
  {
    String returnValue = null
    customFields.each {
      if(it.name == fieldName)
      {
        returnValue = it.id
      }
    }

    returnValue
  }

  def closeTicket(String ticketInternalId, String pTransitionName)
  {
    //1. Get the transitions
    def url = getIssueTransitionsUrl(serverUrl, ticketInternalId)
    def headers = getRequestHeaders(username, password)
    def resp = http.get(url, headers)

    //2. Pick the right transition
    //todo : what should the default be? Or, what to do if no transition matches?
    def transition_id = "21"

    resp.transitions.each {
      if (pTransitionName == it.name){
        transition_id = it.id
      }
    }

    //3. Issue the transition
    url = getExecuteTransitionUrl(serverUrl, ticketInternalId)
    def body = getExecuteTransitionRequestBody(transition_id)
    http.post(url, body, headers)
  }

  private static Map getExecuteTransitionRequestBody(String transitionId) {
    return [
            transition : [
                    id: transitionId
            ]
    ]
  }

  private String getExecuteTransitionUrl(String serverUrl, String ticketInternalId)
  {
    verbosePrintLn("Execute an Issue Transition in Jira")

    //POST: /rest/api/2/issue/10110/transitions - [{"transition":{"id":"21"}}]
    return "${serverUrl}/rest/api/2/issue/${ticketInternalId}/transitions"
  }

  private String getIssueTransitionsUrl(String serverUrl, String ticketInternalId)
  {
    verbosePrintLn("Get the available Issue Transitions from Jira")

    // "/rest/api/2/issue/{{ticketInternalId}}/transitions?expand=transitions.fields"
    return "${serverUrl}/rest/api/2/issue/${ticketInternalId}/transitions?expand=transitions.fields"
  }

  private String getLookupTicketsForProjectUrl(String serverUrl,
                                                      String projectKey,
                                                      String transitionTargetStatus,
                                                      String applicationCustomFieldName,
                                                      String iqApplicationExternalId)
  {
    verbosePrintLn("Perform a JQL search to get the tickets for a Project, status, and application from Jira")

    // "/rest/api/2/search?jql=project%3D%22{{jiraProjectKey}}%22"
    // %20AND%20status!%3D%22Done%22
    // %20AND%20"IQ%20Application"%20~%20"test"

    //TODO: does this need to change for sub-tasks and waivers?


    StringBuffer plainSearchString = new StringBuffer()

    plainSearchString <<= "project = \"${projectKey}\""

    if (transitionTargetStatus?.trim())
    {
      plainSearchString <<= " AND status != \"${transitionTargetStatus}\""
    }

    if (applicationCustomFieldName?.trim())
    {
      plainSearchString <<= " AND \"${applicationCustomFieldName}\" ~ \"${iqApplicationExternalId}\""
    }

    String urlSearchString = URLEncoder.encode(plainSearchString.toString(), "UTF-8")

    return "${serverUrl}/rest/api/2/search?jql=${urlSearchString}"

  }

  /**
   * Get the list of all fields in jira so we can map custom field name to id
   *
   *  https://developer.atlassian.com/cloud/jira/platform/rest/v2/#api-rest-api-2-field-get
   *
   *  [
   *   {
   *     "id": "description",
   *     "name": "Description",
   *     "custom": false,
   *     "orderable": true,
   *     "navigable": true,
   *     "searchable": true,
   *     "clauseNames": [
   *       "description"
   *     ],
   *     "schema": {
   *       "type": "string",
   *       "system": "description"
   *     }
   *   },
   *   {
   *     "id": "summary",
   *     "key": "summary",
   *     "name": "Summary",
   *     "custom": false,
   *     "orderable": true,
   *     "navigable": true,
   *     "searchable": true,
   *     "clauseNames": [
   *       "summary"
   *     ],
   *     "schema": {
   *       "type": "string",
   *       "msary"
   *     }
   *   }
   * ]
   * @param serverUrl
   * @return
   */
  private String getLookupCustomFieldsUrl(String serverUrl)
  {
    verbosePrintLn("Get the list of custom fields from Jira")

    return "${serverUrl}/rest/api/2/field"
  }

  private String getLookupMetadataConfigurationForCreateIssueUrl(String serverUrl, String pProjectName, String pIssueTypeName)
  {
    verbosePrintLn("Get the list of field metadata needed when creating an issue in Jira")

    // {{jiraUrl}}/rest/api/2/issue/createmeta?projectKeys=JIRAIQ&issuetypeNames=Task&expand=projects.issuetypes.fields
    return "${serverUrl}/rest/api/2/issue/createmeta?projectKeys=${pProjectName}&issuetypeNames=${pIssueTypeName}&expand=projects.issuetypes.fields"
  }

  /**
   * https://developer.atlassian.com/cloud/jira/platform/rest/v2/#api-rest-api-2-issue-post
   * https://developer.atlassian.com/server/jira/platform/jira-rest-api-examples/#creating-an-issue-examples
   *
   * @param serverUrl
   * @return
   */
  private String getCreateIssueRequestUrl(serverUrl) {
    verbosePrintLn("Create a Jira Ticket")

    //post: /rest/api/2/issue - [{"fields":{"project":{"key":"DP"},"summary":"Sonatype IQ Server SECURITY-HIGH Policy Violation","description":"\n\tDescription: Sonatype IQ Server SECURITY-HIGH Policy Violation\n\n\tTimestamp: 2019-01-26 01:38:59 -0500\n\n\tSource: SonatypeIQ:IQServerAppId:scanIQ\n\n\tSeverity: 1\n\n\tFingerprint:  57767fa9ecbe0b6271f20ea215e969ac7ed8f24ff7a67ee77dbf090e9e7f469b\n\n\tFound by:  SonatypeIQ\n\n\tDetail:  CVE-2019-1234",
    // "priority":{"name":"Low"},"issuetype":{"name":"Bug"}},"labels":["Glue","triage.git"]}]
    return "${serverUrl}/rest/api/2/issue"
  }

  private static Map getCreateIssueRequestBody(projectKey, issueTypeName, priorityName, description, detail, source, severity, fprint,
                                               iqAppExternalId,iqAppExternalIdCustomFieldId,
                                               iqOrgExternalId, iqOrgExternalIdCustomFieldId,
                                               scanStage, scanStageId,
                                               violationDate, violationDateId,
                                               lastScanDate, lastScanDateId,
                                               severityString, severityId,
                                               cveCode, cveCodeId,
                                               cvss, cvssId,
                                               scanType, scanTypeId,
                                               toolName, toolNameId,
                                               violationUniqueId, violationIdCustomFieldId)
  {
    String newdate = new Date().format("yyyy-MM-dd HH:mm:ss Z") //2019-01-26 01:38:59 -0500

    String formatted_summary = "${description}"
    String formatted_description = "\n\tDescription: ${description}\n\n\tTimestamp: ${newdate}\n\n\tSource: ${source}\n\n\tPolicy Threat Level: ${severity}\n\n\tFingerprint:  ${fprint}\n\n\tFound by:  SonatypeIQ\n\n\tDetail:  ${detail}"

    def returnValue = [
            fields : [
                      project: [
                              key: projectKey
                      ],
                      summary: formatted_summary,
                      description: formatted_description
                    ]
           ]

    if(priorityName)
    {
      addCustomFieldToTicket(returnValue, "priority", [ name: priorityName ])
    }

    if(issueTypeName)
    {
      addCustomFieldToTicket(returnValue, "issuetype", [ name: issueTypeName ])
    }

    addCustomFieldToTicket(returnValue, iqAppExternalIdCustomFieldId, iqAppExternalId)
    addCustomFieldToTicket(returnValue, iqOrgExternalIdCustomFieldId, iqOrgExternalId)
    addCustomFieldToTicket(returnValue, scanStageId, scanStage)
    addCustomFieldToTicket(returnValue, violationDateId, violationDate)
    addCustomFieldToTicket(returnValue, lastScanDateId, lastScanDate) //TODO: Update on each scan if the finding already exists
    addCustomFieldToTicket(returnValue, severityId, severityString)
    addCustomFieldToTicket(returnValue, cveCodeId, cveCode)
    addCustomFieldToTicket(returnValue, cvssId, cvss)
    addCustomFieldToTicket(returnValue, scanTypeId, [ value: scanType ]) //todo: see if i can make these dynamic based on the project metadata
    addCustomFieldToTicket(returnValue, toolNameId, [ value: toolName ])
    addCustomFieldToTicket(returnValue, violationIdCustomFieldId, violationUniqueId)

    return returnValue
  }

  def static addCustomFieldToTicket(Map ticketFieldsArray, String customFieldId, customFieldValue)
  {
    if (customFieldId && customFieldValue)
    {
      ticketFieldsArray.fields.put(customFieldId, customFieldValue)
    }
  }
}
