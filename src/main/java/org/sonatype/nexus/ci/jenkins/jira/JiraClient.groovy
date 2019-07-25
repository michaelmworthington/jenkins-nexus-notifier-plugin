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

import org.sonatype.nexus.ci.jenkins.model.PolicyViolation
import org.sonatype.nexus.ci.jenkins.util.AbstractToolClient
import org.sonatype.nexus.ci.jenkins.util.JiraFieldMappingUtil

class JiraClient extends AbstractToolClient
{
  private Boolean dryRun
  private Boolean disableJqlFieldFilter
  private int jqlMaxResultsOverride

  JiraClient(String serverUrl,
             String username,
             String password,
             PrintStream logger,
             final boolean verboseLogging = false,
             final boolean dryRun = false,
             final boolean disableJqlFieldFilter = false,
             final int jqlMaxResultsOverride = 50)
  {
    super(serverUrl, username, password, logger, verboseLogging)

    this.dryRun = dryRun
    this.disableJqlFieldFilter = disableJqlFieldFilter
    this.jqlMaxResultsOverride = jqlMaxResultsOverride

    if(jqlMaxResultsOverride < 1)
    {
      throw new RuntimeException("Invalid Configuration: Max Results was ${jqlMaxResultsOverride} and must be greater than zero.")
    }
  }

  def createIssue(JiraFieldMappingUtil jiraFieldMappingUtil, PolicyViolation pPolicyViolation)
  {
    def url = getCreateIssueRequestUrl(serverUrl)
    Map body = getCreateIssueRequestBody(jiraFieldMappingUtil, true, pPolicyViolation)
    def headers = getRequestHeaders(username, password)

    if (!dryRun)
    {
      http.post(url, body, headers)
    }
  }

  /**
   * https://developer.atlassian.com/server/jira/platform/jira-rest-api-examples/#creating-a-sub-task
   */
  def createSubTask(JiraFieldMappingUtil jiraFieldMappingUtil, String parentIssueKey, PolicyViolation pPolicyViolation)
  {
    def url = getCreateIssueRequestUrl(serverUrl)
    Map body = getCreateIssueRequestBody(jiraFieldMappingUtil, false, pPolicyViolation)
    getCreateSubTaskRequestBody(jiraFieldMappingUtil, parentIssueKey, body)
    def headers = getRequestHeaders(username, password)

    if (!dryRun)
    {
      http.post(url, body, headers)
    }
  }

  def updateIssueScanDate(JiraFieldMappingUtil jiraFieldMappingUtil, String issueKey)
  {
    def url = getUpdateIssueRequestUrl(serverUrl, issueKey)
    Map body = getUpdateIssueScanDateRequestBody(jiraFieldMappingUtil)
    def headers = getRequestHeaders(username, password)

    if (!dryRun)
    {
      http.put(url, body, headers)
    }
  }

//  def lookupJiraTicketsWithGet(String projectKey,
//                        String transitionTargetStatus,
//                        String applicationCustomFieldName,
//                        String iqApplicationExternalId)
//  {
//    def url = getLookupTicketsForProjectUrl(serverUrl, projectKey, transitionTargetStatus, applicationCustomFieldName, iqApplicationExternalId)
//    def headers = getRequestHeaders(username, password)
//
//    http.get(url, headers)
//  }

  def lookupJiraTickets(JiraFieldMappingUtil jiraFieldMappingUtil, int pStartAtIndex)
  {
    def url = getLookupTicketsForProjectUrl(serverUrl)
    Map body = getLookupTicketsForProjectBody(jiraFieldMappingUtil, pStartAtIndex)
    def headers = getRequestHeaders(username, password)

    http.post(url, body, headers)
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

  def closeTicket(String ticketInternalId, String pTransitionName)
  {
    //1. Get the transitions
    def url = getIssueTransitionsUrl(serverUrl, ticketInternalId)
    def headers = getRequestHeaders(username, password)
    def resp = http.get(url, headers)

    //2. Pick the right transition
    def transition_id = null

    resp.transitions.each {
      if (pTransitionName == it.name){
        transition_id = it.id
      }
    }

    if(!transition_id)
    {
      throw new RuntimeException("Transition not found for name: ${pTransitionName}")
    }

    //3. Issue the transition
    url = getExecuteTransitionUrl(serverUrl, ticketInternalId)
    def body = getExecuteTransitionRequestBody(transition_id)

    if (!dryRun)
    {
      http.post(url, body, headers)
    }
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

//  private String getLookupTicketsForProjectUrl(String serverUrl,
//                                               String projectKey,
//                                               String transitionTargetStatus,
//                                               String applicationCustomFieldName,
//                                               String iqApplicationExternalId)
//  {
//    verbosePrintLn("Perform a JQL search with GET to get the tickets for a Project, status, and application from Jira")
//
//    // "/rest/api/2/search?jql=project%3D%22{{jiraProjectKey}}%22"
//    // %20AND%20status!%3D%22Done%22
//    // %20AND%20"IQ%20Application"%20~%20"test"
//
//    StringBuffer plainSearchString = new StringBuffer()
//
//    plainSearchString <<= "project = \"${projectKey}\""
//
//    if (transitionTargetStatus?.trim())
//    {
//      plainSearchString <<= " AND status != \"${transitionTargetStatus}\""
//    }
//
//    if (applicationCustomFieldName?.trim())
//    {
//      plainSearchString <<= " AND \"${applicationCustomFieldName}\" ~ \"${iqApplicationExternalId}\""
//    }
//
//    String urlSearchString = URLEncoder.encode(plainSearchString.toString(), "UTF-8")
//
//    return "${serverUrl}/rest/api/2/search?jql=${urlSearchString}"
//
//  }

  private String getLookupTicketsForProjectUrl(String serverUrl)
  {
    verbosePrintLn("Perform a JQL search with POST to get the tickets for a Project, status, and application from Jira")

    return "${serverUrl}/rest/api/2/search"
  }

  private Map getLookupTicketsForProjectBody(JiraFieldMappingUtil jiraFieldMappingUtil, int pStartAtIndex)
  {
    String projectKey = jiraFieldMappingUtil.projectKey
    String transitionTargetStatus = jiraFieldMappingUtil.transitionStatus
    String applicationCustomFieldName = jiraFieldMappingUtil.getApplicationCustomField().customFieldName
    String iqApplicationExternalId = jiraFieldMappingUtil.getApplicationCustomField().customFieldValue
    String violationIdCustomFieldId = jiraFieldMappingUtil.getViolationIdCustomField().customFieldId

    def jql = [:]
    jql['jql'] = "project = " + projectKey

    if (transitionTargetStatus?.trim())
    {
      jql['jql'] += " AND status != \"${transitionTargetStatus}\""
    }

    if (applicationCustomFieldName?.trim() && iqApplicationExternalId?.trim())
    {
      jql['jql'] += " AND \"${applicationCustomFieldName}\" ~ \"${iqApplicationExternalId}\""
    }

    jql['jql'] += " ORDER BY key"


    def fields = [:]

    if (!this.disableJqlFieldFilter)
    {
      fields['fields'] = ["id",
                           "key",
                           "issuetype",
                           "summary",
                           "status"
      ]


      if (violationIdCustomFieldId?.trim())
      {
        fields['fields'] += violationIdCustomFieldId
      }
    }

    return jql + fields + [
            startAt   : pStartAtIndex,
            maxResults: this.jqlMaxResultsOverride
    ]
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

  /**
   * https://developer.atlassian.com/server/jira/platform/jira-rest-api-examples/#editing-an-issue-examples
   *
   * @param serverUrl
   * @return
   */
  private String getUpdateIssueRequestUrl(String serverUrl, String issueKey)
  {
    verbosePrintLn("Update a Jira Ticket")

    return "${serverUrl}/rest/api/2/issue/${issueKey}"
  }

  private static Map getCreateIssueRequestBody(JiraFieldMappingUtil jiraFieldMappingUtil, boolean pIsParentTicket, PolicyViolation pPolicyViolation)
  {
    def detail = pPolicyViolation.cvssReason
    def source = pPolicyViolation.reportLink
    def severity = pPolicyViolation.policyThreatLevel
    def fprint = pPolicyViolation.fingerprintKey

    String description = buildTicketSummaryTitleText(jiraFieldMappingUtil, pPolicyViolation, pIsParentTicket)

    //TODO: Pull these out and take them in as parameters (maybe, i seem to be moving the data objects here for formatting
    //TODO: also format a summary ticket
    String nowFormatted = jiraFieldMappingUtil.getFormattedScanDateForJira()

    //TODO: Nice Summary
    String formatted_summary = "${description}"
    //TODO: Nice Description (Add all policy violations if we are aggregating and not creating subtasks
    String formatted_description = "\n\tDescription: ${description}\n\n\tFirst Found Timestamp: ${nowFormatted}\n\n\tSource: ${source}\n\n\tPolicy Threat Level: ${severity}\n\n\tFingerprint:  ${fprint}\n\n\tFound by:  SonatypeIQ\n\n\tDetail:  ${detail}"

    def returnValue = [
            fields : [
                      project: [
                              key: jiraFieldMappingUtil.projectKey
                      ],
                      summary: formatted_summary,
                      description: formatted_description
                    ]
           ]

    if(jiraFieldMappingUtil.priorityName)
    {
      returnValue.fields.put("priority", [ name: jiraFieldMappingUtil.priorityName ])
    }

    if(jiraFieldMappingUtil.issueTypeName)
    {
      returnValue.fields.put("issuetype", [ name: jiraFieldMappingUtil.issueTypeName ])
    }

    jiraFieldMappingUtil.addCustomFieldsToTicket(returnValue, pPolicyViolation, pIsParentTicket)

    return returnValue
  }

  private static void getCreateSubTaskRequestBody(JiraFieldMappingUtil jiraFieldMappingUtil,
                                                  String parentIssueKey,
                                                  Map returnValue)
  {
    if(jiraFieldMappingUtil.issueTypeName)
    {
      returnValue.fields.put("issuetype", [ name: jiraFieldMappingUtil.subTaskIssueTypeName ])
    }

    if(parentIssueKey)
    {
      returnValue.fields.put("parent", [ key: parentIssueKey ])
    }
  }

  private static Map getUpdateIssueScanDateRequestBody(JiraFieldMappingUtil jiraFieldMappingUtil)
  {
    def date = [:]
    date[jiraFieldMappingUtil.getLastScanDateCustomField().customFieldId] = jiraFieldMappingUtil.getFormattedScanDateForJira()

    return [ fields: date ]
  }

  private static String buildTicketSummaryTitleText(JiraFieldMappingUtil jiraFieldMappingUtil, PolicyViolation pPolicyViolation, boolean pIsParentTicket)
  {
    def returnValue = "Sonatype Nexus IQ Server - ${jiraFieldMappingUtil.getApplicationCustomField().customFieldValue} -"

    if (jiraFieldMappingUtil.shouldCreateIndividualTickets)
    {
      if (jiraFieldMappingUtil.shouldAggregateTicketsByComponent && pIsParentTicket)
      {
        returnValue <<= " Component ${pPolicyViolation.componentIdentifier.prettyName} has Policy Violations"
      }
      else
      {
        if (pPolicyViolation.policyName)
        {
          returnValue <<= " ${pPolicyViolation.policyName}"
        }

        returnValue <<= " Policy Violation"


        //If a security issue, add in the CVE Code to the ticket title so we can tell the subtasks apart
        if (pPolicyViolation.cveCode)
        {
          returnValue <<= " - $pPolicyViolation.cveCode"
        }

        returnValue <<= " - ${pPolicyViolation.componentIdentifier.prettyName}"
      }
    }
    else
    {
      returnValue <<= " Summary of Violations"
    }

    return returnValue
  }
}
