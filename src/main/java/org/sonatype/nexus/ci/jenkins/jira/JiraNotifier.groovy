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

import hudson.AbortException
import hudson.model.Run
import hudson.model.TaskListener
import org.sonatype.nexus.ci.jenkins.iq.IQClient
import org.sonatype.nexus.ci.jenkins.iq.IQClientFactory
import org.sonatype.nexus.ci.jenkins.model.PolicyEvaluationHealthAction
import org.sonatype.nexus.ci.jenkins.model.PolicyViolation
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification

import javax.annotation.Nonnull

import static com.google.common.base.Preconditions.checkArgument
import static com.google.common.base.Strings.isNullOrEmpty

class JiraNotifier
{
  final Run run
  final TaskListener listener
  final PrintStream logger

  JiraNotifier(@Nonnull final Run run, @Nonnull final TaskListener listener) {
    this.run = run
    this.listener = listener
    this.logger = listener.logger
  }

  void send(final boolean buildPassing,
            final JiraNotification jiraNotification,
            final PolicyEvaluationHealthAction pPolicyEvaluationHealthAction)
  {
    checkArgument(!isNullOrEmpty(jiraNotification.projectKey), Messages.JiraNotifier_NoProjectKey())

    IQClient iqClient = IQClientFactory.getIQClient(jiraNotification.jobIQCredentialsId, logger)
    JiraClient jiraClient = JiraClientFactory.getJiraClient(jiraNotification.jobJiraCredentialsId, logger)

    def envVars = run.getEnvironment(listener)
    def projectKey = envVars.expand(jiraNotification.projectKey)
    boolean shouldCreateIndividualTickets = jiraNotification.shouldCreateIndividualTickets
    boolean shouldTransitionJiraTickets = jiraNotification.shouldTransitionJiraTickets
    String transitionStatus = jiraNotification.jiraTransitionStatus

    //Custom Fields
    String applicationCustomFieldName = jiraNotification.applicationCustomFieldName
    String organizationCustomFieldName = jiraNotification.organizationCustomFieldName
    String scanStageCustomFieldName = jiraNotification.scanStageCustomFieldName
    String violationIdCustomFieldName = jiraNotification.violationIdCustomFieldName
    String violationDetectDateCustomFieldName = jiraNotification.violationIdCustomFieldName
    String lastScanDateCustomFieldName = jiraNotification.lastScanDateCustomFieldName
    String severityCustomFieldName = jiraNotification.severityCustomFieldName
    String cveCodeCustomFieldName = jiraNotification.cveCodeCustomFieldName
    String cvssCustomFieldName = jiraNotification.cvssCustomFieldName
    String policyFilterPrefix = jiraNotification.policyFilterPrefix
    boolean shouldAggregateTicketsByComponent = jiraNotification.shouldAggregateTicketsByComponent //TODO: Aggregate by component - epics & stories
    String scanTypeCustomFieldName = jiraNotification.scanTypeCustomFieldName
    String scanTypeCustomFieldValue = jiraNotification.scanTypeCustomFieldValue
    String toolNameCustomFieldName = jiraNotification.toolNameCustomFieldName
    String toolNameCustomFieldValue = jiraNotification.toolNameCustomFieldValue

    logger.println("Creating Jira Tickets for Project: " + projectKey)

    PolicyEvaluationHealthAction policyEvaluationHealthAction = PolicyEvaluationHealthAction.build(pPolicyEvaluationHealthAction)

    try {
      // IQ Link: http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/3d0fedc4857f44368e0b501a6b986048
      logger.println("IQ Link: " + policyEvaluationHealthAction.reportLink)

      String[] linkPieces = policyEvaluationHealthAction.reportLink.split("/")
      String iqReportInternalid = linkPieces[linkPieces.length-1]
      String iqAppExternalId = linkPieces[linkPieces.length-3]

      //TODO: look up the org
      String iqOrgExternalId = "org";

      def customFields = jiraClient.lookupCustomFields() //todo: streamline this - just pass the name around and do the lookups inside of jira client when creating the ticket??
      String applicationCustomFieldId = jiraClient.lookupCustomFieldId(customFields, applicationCustomFieldName)
      String organizationCustomFieldId = jiraClient.lookupCustomFieldId(customFields, organizationCustomFieldName)
      String scanStageCustomFieldId = jiraClient.lookupCustomFieldId(customFields, scanStageCustomFieldName)
      String violationIdCustomFieldId = jiraClient.lookupCustomFieldId(customFields, violationIdCustomFieldName)
      String violationDetectDateCustomFieldId = jiraClient.lookupCustomFieldId(customFields, violationDetectDateCustomFieldName)
      String lastScanDateCustomFieldId = jiraClient.lookupCustomFieldId(customFields, lastScanDateCustomFieldName)
      String severityCustomFieldId = jiraClient.lookupCustomFieldId(customFields, severityCustomFieldName)
      String cveCodeCustomFieldId = jiraClient.lookupCustomFieldId(customFields, cveCodeCustomFieldName)
      String cvssCustomFieldId = jiraClient.lookupCustomFieldId(customFields, cvssCustomFieldName)
      String scanTypeCustomFieldId = jiraClient.lookupCustomFieldId(customFields, scanTypeCustomFieldName)
      String toolNameCustomFieldId = jiraClient.lookupCustomFieldId(customFields, toolNameCustomFieldName)

      if (shouldCreateIndividualTickets)
      {
        /***************************************
              1. Get Policy Findings from IQ
         ***************************************/

        //http://localhost:8060/iq/rest/report/aaaaaaa-testidegrandfathering/a22d44d0209b47358c8dd2532bb7afb3/browseReport/policythreats.json
        def potentialFindings = iqClient.lookupPolcyDetailsFromIQ(iqReportInternalid, iqAppExternalId)
        Map<String, PolicyViolation> potentialFindingsMap = new HashMap<String, PolicyViolation>()
        potentialFindings.aaData.each {
          PolicyViolation.buildFromIQ(it, policyEvaluationHealthAction.reportLink, iqAppExternalId, policyFilterPrefix).each {
            potentialFindingsMap.put(it.fingerprint, it)
          }
        }

        /***************************************
            2. Get Tickets from Jira
         ***************************************/

        //http://localhost:8080/rest/api/2/search?jql=project%3D%22JIRAIQ%22
        def currentFindings = jiraClient.lookupJiraTickets(projectKey, transitionStatus, applicationCustomFieldName, iqAppExternalId) //todo: update current findings
        Map<String, PolicyViolation> currentFindingsMap = new HashMap<String, PolicyViolation>()
        currentFindings.issues.each {
          PolicyViolation pv = PolicyViolation.buildFromJira(it, violationIdCustomFieldId)
          currentFindingsMap.put(pv.fingerprint, pv)
        }

        /***************************************
            3. Filter out Existing Tickets
         ***************************************/

        //todo : aggregate by component - how to track all the violations?
        logger.println("Potential Components with Findings from the IQ Server Report: ${potentialFindings.aaData.size}")
        logger.println("Potential Policy Violation Findings from the IQ Server Report: ${potentialFindingsMap.size()}")
        logger.println("Current Findings in Jira before filtering: ${currentFindings.issues.size}")

        //which results from IQ do we need to create, and which alredy exist, and which need to be closed
        //  Loop through potential findings map and current findings map
        Set<PolicyViolation> newFindings = new HashSet<PolicyViolation>()
        Set<PolicyViolation> repeatFindings = new HashSet<PolicyViolation>()
        calculateNewAndRepeatFindings(potentialFindingsMap, currentFindingsMap, newFindings, repeatFindings)
        Set<PolicyViolation> oldFindings = calculateOldFindings(potentialFindingsMap, currentFindingsMap)

        logger.println("Number of findings that do not have tickets: ${newFindings.size()}")
        logger.println("Number of findings that have existing tickets: ${repeatFindings.size()}")
        logger.println("Number of tickets that no longer have findings: ${oldFindings.size()}")

        /***************************************
          4. Create New Tickets
         ***************************************/

        logger.println("Creating ${newFindings.size()} tickets for each policy violation")
        newFindings.each {
          createIndividualTicket(jiraClient, projectKey, it,
                                 iqAppExternalId, applicationCustomFieldId,
                                 iqOrgExternalId, organizationCustomFieldId,
                                 "TODO: Scan Stage", scanStageCustomFieldId,
                                 "TODO: Violation Date", violationDetectDateCustomFieldId,
                                 "TODO: last scan date", lastScanDateCustomFieldId,
                                 "TODO: severity", severityCustomFieldId,
                                 "TODO: cve code", cveCodeCustomFieldId,
                                 "TODO: cvss", cvssCustomFieldId,
                                 scanTypeCustomFieldValue, scanTypeCustomFieldId,
                                 toolNameCustomFieldValue, toolNameCustomFieldId,
                                 violationIdCustomFieldId)
        }

        /***************************************
          5. Close Tickets that have no finding (i.e. they have been fixed
         ***************************************/

        if (shouldTransitionJiraTickets) {
          logger.println("Transitioning ${oldFindings.size()} old tickets to: ${transitionStatus}")
          oldFindings.each{
            transitionTicket(jiraClient, transitionStatus, it)
          }
        }

        /***************************************
           6. Waive findings where the ticket has been closed
         ***************************************/

        //TODO: What tickets in Jira have been closed where we can apply a waiver in IQ
        //      need to deal with the edge cases on this one
        //
        // loop through **repeatFindings**
      }
      else {
        logger.println("Creating just one ticket with a violation summary in the description")
        createSummaryTicket(jiraClient, projectKey, policyEvaluationHealthAction, iqAppExternalId, iqReportInternalid)

        if (shouldTransitionJiraTickets)
        {
          logger.println("Skipping Transitioning tickets when in Summary Mode")
        }
      }
    } catch (ex) {
      logger.println(ex.message)
      ex.printStackTrace(logger)
      throw new AbortException(ex.message)
    }
  }

  private void calculateNewAndRepeatFindings(HashMap<String, PolicyViolation> pPotentialFindings,
                                             HashMap<String, PolicyViolation> pCurrentFindings,
                                             HashSet<PolicyViolation> newFindings,
                                             HashSet<PolicyViolation> repeatFindings)
  {
    pPotentialFindings.each {
      if (pCurrentFindings.containsKey(it.key))
      {
        logger.println("Jira ticket: ${pCurrentFindings.get(it.key).ticketExternalId} already exists for finding: ${it.value.fingerprintPrettyPrint}")
        repeatFindings.add(it.value)
      } else
      {
        logger.println("Jira ticket: not found for for finding: ${it.value.fingerprintPrettyPrint}")
        newFindings.add(it.value)
      }
    }
  }

  private Set<PolicyViolation> calculateOldFindings(HashMap<String, PolicyViolation> pPotentialFindings, HashMap<String, PolicyViolation> pCurrentFindings)
  {
    Set<PolicyViolation> returnValue = new HashSet<PolicyViolation>()

    pCurrentFindings.each{
      if (pPotentialFindings.containsKey(it.key)){
        logger.println("Finding: ${pPotentialFindings.get(it.key).fingerprintPrettyPrint} still exists for Jira Ticket: ${it.value.ticketExternalId}")
      } else {
        logger.println("Finding no longer exists for Jira Ticket: ${it.value.ticketExternalId} - ${it.value.ticketSummary}")
        returnValue.add(it.value)
      }
    }

    return returnValue
  }

  private void transitionTicket(JiraClient jiraClient, String transitionStatus, PolicyViolation pPolicyViolation)
  {
    logger.println("Transitioning Jira Ticket: ${pPolicyViolation.ticketExternalId} - ${pPolicyViolation.ticketSummary} to Status: ${transitionStatus}")

    jiraClient.closeTicket(pPolicyViolation.ticketInternalId, transitionStatus)
  }

  private void createIndividualTicket(JiraClient jiraClient, String projectKey, PolicyViolation pPolicyViolation,
                                      String iqAppExternalId, String iqAppExternalIdCustomFieldId,
                                      String iqOrgExternalId, String iqOrgExternalIdCustomFieldId,
                                      String scanStage, String scanStageId,
                                      String violationDate, String violationDateId,
                                      String lastScanDate, String lastScanDateId,
                                      String severityString, String severityId,
                                      String cveCode, String cveCodeId,
                                      String cvss, String cvssId,
                                      String scanType, String scanTypeId,
                                      String toolName, String toolNameId,
                                      String violationIdCustomFieldId)
  {
    logger.println("Creating Jira Ticket in Project: ${projectKey} for Component: ${pPolicyViolation.fingerprintPrettyPrint}")

    def description = "Sonatype IQ Server ${pPolicyViolation.policyName} Policy Violation - ${pPolicyViolation.componentName}"
    def detail = "$pPolicyViolation.cvssReason"
    def source = pPolicyViolation.reportLink
    def severity = pPolicyViolation.policyThreatLevel

    jiraClient.createIssue(projectKey,
                           description,
                           detail,
                           source,
                           severity,
                           pPolicyViolation.fingerprintKey,
                           iqAppExternalId, iqAppExternalIdCustomFieldId,
                           iqOrgExternalId, iqOrgExternalIdCustomFieldId,
                           scanStage, scanStageId,
                           violationDate, violationDateId,
                           lastScanDate, lastScanDateId,
                           severityString, severityId,
                           cveCode, cveCodeId,
                           cvss, cvssId,
                           scanType, scanTypeId,
                           toolName, toolNameId,
                           pPolicyViolation.fingerprint,
                           violationIdCustomFieldId)
  }

  private void createSummaryTicket(JiraClient jiraClient, String projectKey, PolicyEvaluationHealthAction policyEvaluationHealthAction, String iqAppExternalId, String iqReportId)
  {
    //TODO: Create a summary ticket from the policyEvaluationHealthAction
    logger.println("Creating Summary Jira Ticket for Project: ${projectKey}")

    def description = "Sonatype IQ Server Summary of Violations"
    def detail = "${policyEvaluationHealthAction.affectedComponentCount} components"
    def source = policyEvaluationHealthAction.reportLink
    def severity = 1
    def fprint = "SONATYPEIQ-${iqAppExternalId}-${iqReportId}"

    jiraClient.createIssue(projectKey,
                           description,
                           detail,
                           source,
                           severity,
                           fprint,
                           null,
                           null,
                           null,
                           null,
                           null,
                           null)

  }
}
