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
import org.sonatype.nexus.ci.jenkins.util.JiraFieldMappingUtil

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

  void send(final boolean buildPassing, //TODO: don't run if the build is failed??? Maybe the check on the IQ Report will fail the process, but make sure that we don't close all the jira tickets if there is an upstream failure
            final JiraNotification jiraNotification,
            final PolicyEvaluationHealthAction pPolicyEvaluationHealthAction)
  {
    checkArgument(!isNullOrEmpty(jiraNotification.projectKey), Messages.JiraNotifier_NoProjectKey()) //todo: the proper way to validate input strings - for custom fields in lookupAndValidateCustomField()

    IQClient iqClient = IQClientFactory.getIQClient(jiraNotification.jobIQCredentialsId, logger, jiraNotification.verboseLogging)
    JiraClient jiraClient = JiraClientFactory.getJiraClient(jiraNotification.jobJiraCredentialsId, logger, jiraNotification.verboseLogging)
    JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotification, jiraClient, run.getEnvironment(listener), logger)
    PolicyEvaluationHealthAction policyEvaluationHealthAction = PolicyEvaluationHealthAction.build(pPolicyEvaluationHealthAction)

    try {
      jiraFieldMappingUtil.expandEnvVars()
      jiraFieldMappingUtil.assignFieldsFromConfig()

      logger.println("Creating Jira Tickets for Project: ${jiraFieldMappingUtil.projectKey} with issue type: ${jiraFieldMappingUtil.issueTypeName} and priority: ${jiraFieldMappingUtil.priorityName}")

      // IQ Link: http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/3d0fedc4857f44368e0b501a6b986048
      logger.println("IQ Link: " + policyEvaluationHealthAction.reportLink)

      String[] linkPieces = policyEvaluationHealthAction.reportLink.split("/")
      String iqReportInternalid = linkPieces[linkPieces.length-1]
      String iqAppExternalId = linkPieces[linkPieces.length-3]

      //TODO: look up the org
      String iqOrgExternalId = "org"

      //Just for debugging right now.
      // todo: Maybe i'll use the response for some validation
      jiraClient.lookupMetadataConfigurationForCreateIssue(jiraFieldMappingUtil.projectKey, jiraFieldMappingUtil.issueTypeName)
      if (jiraFieldMappingUtil.shouldCreateSubTasksForAggregatedTickets)
      {
        jiraClient.lookupMetadataConfigurationForCreateIssue(jiraFieldMappingUtil.projectKey, jiraFieldMappingUtil.subTaskIssueTypeName)
      }

      jiraFieldMappingUtil.mapCustomFieldNamesToIds()

      if (jiraFieldMappingUtil.shouldCreateIndividualTickets)
      {
        // Data from IQ Server ("potential") and JIRA ("current") mapped by Fingerprint
        Map<String, PolicyViolation> potentialFindingsMap = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> currentFindingsMap = new HashMap<String, PolicyViolation>()

        // Deduplicated findings
        Set<PolicyViolation> newFindings = new HashSet<PolicyViolation>()
        Set<PolicyViolation> repeatFindings = new HashSet<PolicyViolation>()
        Set<PolicyViolation> oldFindings = new HashSet<PolicyViolation>()


        /***************************************
              1. Get Policy Findings from IQ
         ***************************************/

        //http://localhost:8060/iq/rest/report/aaaaaaa-testidegrandfathering/a22d44d0209b47358c8dd2532bb7afb3/browseReport/policythreats.json
        def potentialFindings = iqClient.lookupPolcyDetailsFromIQ(iqReportInternalid, iqAppExternalId)

        potentialFindings.aaData.each {
          PolicyViolation.buildFromIQ(it,
                                      policyEvaluationHealthAction.reportLink,
                                      iqAppExternalId,
                                      jiraFieldMappingUtil.policyFilterPrefix)
                  .each {
                    potentialFindingsMap.put(it.fingerprint, it)
                  }
        }

        /***************************************
            2. Get Tickets from Jira
         ***************************************/

        //http://localhost:8080/rest/api/2/search?jql=project%3D%22JIRAIQ%22
        def currentFindings = jiraClient.lookupJiraTickets(jiraFieldMappingUtil.projectKey,
                                                           jiraFieldMappingUtil.transitionStatus,
                                                           jiraFieldMappingUtil.applicationCustomFieldName,
                                                           iqAppExternalId)

        currentFindings.issues.each {
          PolicyViolation pv = PolicyViolation.buildFromJira(it, jiraFieldMappingUtil.violationIdCustomFieldId)
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
        calculateNewAndRepeatFindings(potentialFindingsMap, currentFindingsMap, newFindings, repeatFindings)
        calculateOldFindings(potentialFindingsMap, currentFindingsMap, oldFindings)

        logger.println("Number of findings that do not have tickets: ${newFindings.size()}")
        logger.println("Number of findings that have existing tickets: ${repeatFindings.size()}")
        logger.println("Number of tickets that no longer have findings: ${oldFindings.size()}")

        /***************************************
          4. Create New Tickets
         ***************************************/

        logger.println("Creating ${newFindings.size()} tickets for each policy violation")
        newFindings.each {
          createIndividualTicket(jiraClient,
                                 jiraFieldMappingUtil,
                                 it,
                                 iqAppExternalId,
                                 iqOrgExternalId,
                                 "TODO: Scan Stage", //TODO
                                 "TODO: severity", //TODO
                                 "TODO: cve code", //TODO
                                 "TODO: cvss" ) //TODO
        }

        /***************************************
         5. Update Scan Time on repeat findings
         ***************************************/

        logger.println("Updating ${repeatFindings.size()} repeat finding tickets with the new scan date")
        repeatFindings.each{
          updateTicketScanDate(jiraClient, jiraFieldMappingUtil, it)
        }

        /***************************************
          6. Close Tickets that have no finding (i.e. they have been fixed)
         ***************************************/

        if (jiraFieldMappingUtil.shouldTransitionJiraTickets) {
          logger.println("Transitioning ${oldFindings.size()} old tickets to: ${jiraFieldMappingUtil.transitionStatus}")
          oldFindings.each{
            transitionTicket(jiraClient, jiraFieldMappingUtil.transitionStatus, it)
          }
        }

        /***************************************
           7. Waive findings where the ticket has been closed
         ***************************************/

        //TODO: What tickets in Jira have been closed where we can apply a waiver in IQ
        //      need to deal with the edge cases on this one
        //
        // loop through **repeatFindings**
      }
      else {
        logger.println("Creating just one ticket with a violation summary in the description")
        createSummaryTicket(jiraClient, jiraFieldMappingUtil, policyEvaluationHealthAction, iqAppExternalId, iqReportInternalid)

        if (jiraFieldMappingUtil.shouldTransitionJiraTickets)
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
                                             Set<PolicyViolation> newFindings,
                                             Set<PolicyViolation> repeatFindings)
  {
    pPotentialFindings.each {
      if (pCurrentFindings.containsKey(it.key))
      {
        logger.println("Jira ticket: ${pCurrentFindings.get(it.key).ticketExternalId} already exists for finding: ${it.value.fingerprintPrettyPrint}")
        repeatFindings.add(pCurrentFindings.get(it.key))
      } else
      {
        logger.println("Jira ticket: not found for for finding: ${it.value.fingerprintPrettyPrint}")
        newFindings.add(it.value)
      }
    }
  }

  private void calculateOldFindings(HashMap<String, PolicyViolation> pPotentialFindings,
                                                    HashMap<String, PolicyViolation> pCurrentFindings,
                                                    Set<PolicyViolation> oldFindings)
  {
    pCurrentFindings.each{
      if (pPotentialFindings.containsKey(it.key)){
        logger.println("Finding: ${pPotentialFindings.get(it.key).fingerprintPrettyPrint} still exists for Jira Ticket: ${it.value.ticketExternalId}")
      } else {
        logger.println("Finding no longer exists for Jira Ticket: ${it.value.ticketExternalId} - ${it.value.ticketSummary}")
        oldFindings.add(it.value)
      }
    }
  }

  private void updateTicketScanDate(JiraClient jiraClient, JiraFieldMappingUtil jiraFieldMappingUtil, PolicyViolation pPolicyViolation)
  {
    logger.println("Updating Jira Ticket: ${pPolicyViolation.ticketExternalId} - ${pPolicyViolation.ticketSummary} with new scan date in field: ${jiraFieldMappingUtil.lastScanDateCustomFieldName} (${jiraFieldMappingUtil.lastScanDateCustomFieldId})")

    jiraClient.updateIssueScanDate(jiraFieldMappingUtil, pPolicyViolation.ticketInternalId)
  }

  private void transitionTicket(JiraClient jiraClient, String transitionStatus, PolicyViolation pPolicyViolation)
  {
    logger.println("Transitioning Jira Ticket: ${pPolicyViolation.ticketExternalId} - ${pPolicyViolation.ticketSummary} to Status: ${transitionStatus}")

    jiraClient.closeTicket(pPolicyViolation.ticketInternalId, transitionStatus)
  }

  private void createIndividualTicket(JiraClient jiraClient,
                                      JiraFieldMappingUtil jiraFieldMappingUtil,
                                      PolicyViolation pPolicyViolation,
                                      String iqAppExternalId,
                                      String iqOrgExternalId,
                                      String scanStage,
                                      String severityString,
                                      String cveCode,
                                      String cvss)
  {
    logger.println("Creating Jira Ticket in Project: ${jiraFieldMappingUtil.projectKey} for Component: ${pPolicyViolation.fingerprintPrettyPrint}")

    def description = "Sonatype IQ Server ${pPolicyViolation.policyName} Policy Violation - ${pPolicyViolation.componentName}"
    def detail = "$pPolicyViolation.cvssReason"
    def source = pPolicyViolation.reportLink
    def severity = pPolicyViolation.policyThreatLevel

    jiraClient.createIssue(jiraFieldMappingUtil,
                           description,
                           detail,
                           source,
                           severity,
                           pPolicyViolation.fingerprintKey,
                           iqAppExternalId,
                           iqOrgExternalId,
                           scanStage,
                           severityString,
                           cveCode,
                           cvss,
                           pPolicyViolation.fingerprint)
  }

  private void createSummaryTicket(JiraClient jiraClient,
                                   JiraFieldMappingUtil jiraFieldMappingUtil,
                                   PolicyEvaluationHealthAction policyEvaluationHealthAction,
                                   String iqAppExternalId,
                                   String iqReportId)
  {
    //TODO: Create a summary ticket from the policyEvaluationHealthAction
    logger.println("Creating Summary Jira Ticket for Project: ${jiraFieldMappingUtil.projectKey}")

    def description = "Sonatype IQ Server Summary of Violations"
    def detail = "${policyEvaluationHealthAction.affectedComponentCount} components"
    def source = policyEvaluationHealthAction.reportLink
    def severity = 1
    def fprint = "SONATYPEIQ-${iqAppExternalId}-${iqReportId}"

    jiraClient.createIssue(jiraFieldMappingUtil,
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
                           null,
                           null)

  }
}
