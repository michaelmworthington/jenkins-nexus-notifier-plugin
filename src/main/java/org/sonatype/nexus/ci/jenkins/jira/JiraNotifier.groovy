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
import org.sonatype.nexus.ci.jenkins.notifier.JiraCustomFieldMappings
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
      logger.println("######################################")
      logger.println("Creating Jira Tickets for Project: ${jiraFieldMappingUtil.projectKey} with issue type: ${jiraFieldMappingUtil.issueTypeName} and priority: ${jiraFieldMappingUtil.priorityName}")
      logger.println("######################################")

      // IQ Link: http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/3d0fedc4857f44368e0b501a6b986048
      logger.println("IQ Link: " + policyEvaluationHealthAction.reportLink)

      String[] linkPieces = policyEvaluationHealthAction.reportLink.split("/")
      String iqReportInternalid = linkPieces[linkPieces.length-1]
      String iqAppExternalId = linkPieces[linkPieces.length-3]

      //TODO: look up the org from iq server
      String iqOrgExternalId = "org"

      if (jiraFieldMappingUtil.shouldCreateIndividualTickets)
      {
        // Data from IQ Server ("potential") and JIRA ("current") mapped by Fingerprint
        Map<String, PolicyViolation> potentialComponentsMap = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> potentialFindingsMap = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> currentComponentsMap = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> currentFindingsMap = new HashMap<String, PolicyViolation>()

        // Deduplicated findings
        Map<String, PolicyViolation> newIQComponents = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> repeatJiraComponentsWithNewFindings = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> newIQFindings = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> repeatJiraComponents = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> repeatJiraFindings = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> oldJiraComponents = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> oldJiraFindings = new HashMap<String, PolicyViolation>()


        /***************************************
              1. Get Policy Findings from IQ
         ***************************************/

        //http://localhost:8060/iq/rest/report/aaaaaaa-testidegrandfathering/a22d44d0209b47358c8dd2532bb7afb3/browseReport/policythreats.json
        def potentialFindings = iqClient.lookupPolcyDetailsFromIQ(iqReportInternalid, iqAppExternalId)

        logger.println("Parsing findings from the IQ Server Report: ${potentialFindings.aaData.size}")
        potentialFindings.aaData.each {
          PolicyViolation.buildFromIQ(potentialComponentsMap,
                                      potentialFindingsMap,
                                      it,
                                      policyEvaluationHealthAction.reportLink,
                                      iqAppExternalId,
                                      jiraFieldMappingUtil.policyFilterPrefix)
        }

        /***************************************
            2. Get Tickets from Jira
         ***************************************/

        //http://localhost:8080/rest/api/2/search?jql=project%3D%22JIRAIQ%22
        def currentFindings = jiraClient.lookupJiraTickets(jiraFieldMappingUtil.projectKey,
                                                           jiraFieldMappingUtil.transitionStatus,
                                                           jiraFieldMappingUtil.getApplicationCustomField().customFieldName,
                                                           iqAppExternalId)

        logger.println("Parsing findings from Jira: ${currentFindings.issues.size}")
        currentFindings.issues.each {
          PolicyViolation pv = PolicyViolation.buildFromJira(it, jiraFieldMappingUtil.getViolationIdCustomField().customFieldId)

          if(jiraFieldMappingUtil.shouldAggregateTicketsByComponent)
          {
            if("Sub-task".equals(pv.ticketType))
            {
              currentFindingsMap.put(pv.fingerprint, pv)
            }
            else
            {
              currentComponentsMap.put(pv.fingerprint, pv)
            }
          }
          else
          {
            if("Sub-task".equals(pv.ticketType))
            {
              logger.println("WARNING: Skipping Jira Ticket ${pv.ticketExternalId} since it is a Sub-task and tickets are not being aggregated")
            }
            else
            {
              currentFindingsMap.put(pv.fingerprint, pv)
            }
          }
        }

        /***************************************
            3. Filter out Existing Tickets
         ***************************************/
        logger.println("######################################")
        logger.println("        Compare Input")
        logger.println("######################################")
        logger.println("Potential Components from the IQ Server Report : ${potentialComponentsMap.size()}")
        logger.println("Potential Findings   from the IQ Server Report : ${potentialFindingsMap.size()}")
        logger.println("Current Components   in Jira before filtering  : ${currentComponentsMap.size()}")
        logger.println("Current Findings     in Jira before filtering  : ${currentFindingsMap.size()}")
        logger.println("######################################")
        logger.println("######################################")

        //which results from IQ do we need to create, and which alredy exist, and which need to be closed
        //  Loop through potential findings map and current findings map
        compareIQServerAndJira(jiraFieldMappingUtil,
                               potentialComponentsMap,
                               potentialFindingsMap,
                               currentComponentsMap,
                               currentFindingsMap,
                               newIQComponents,
                               repeatJiraComponentsWithNewFindings,
                               newIQFindings,
                               repeatJiraComponents,
                               repeatJiraFindings,
                               oldJiraComponents,
                               oldJiraFindings)

        logger.println("######################################")
        logger.println("        Compare Output")
        logger.println("######################################")
        logger.println("Number of components that do not have tickets      : ${newIQComponents.size()}")
        logger.println("Number of findings   that do not have tickets      : ${newIQFindings.size()}")
        logger.println("Number of components that have existing tickets    : ${repeatJiraComponents.size()} (${repeatJiraComponentsWithNewFindings.size()} have new findings)")
        logger.println("Number of findings   that have existing tickets    : ${repeatJiraFindings.size()}")
        logger.println("Number of tickets    that no longer have components: ${oldJiraComponents.size()}")
        logger.println("Number of tickets    that no longer have findings  : ${oldJiraFindings.size()}")
        logger.println("######################################")
        logger.println("######################################")

        /***************************************
          4. Create New Tickets
         ***************************************/

        createJiraTickets(newIQComponents,
                          repeatJiraComponentsWithNewFindings,
                          newIQFindings,
                          jiraClient,
                          jiraFieldMappingUtil,
                          iqAppExternalId,
                          iqOrgExternalId)

        /***************************************
         5. Update Scan Time on repeat findings
         ***************************************/

        logger.println("Updating ${repeatJiraComponents.size()} repeat component tickets with the new scan date")
        repeatJiraComponents.each{
          updateTicketScanDate(jiraClient, jiraFieldMappingUtil, it.value)
        }

        logger.println("Updating ${repeatJiraFindings.size()} repeat finding tickets with the new scan date")
        repeatJiraFindings.each{
          updateTicketScanDate(jiraClient, jiraFieldMappingUtil, it.value)
        }

        /***************************************
          6. Close Tickets that have no finding (i.e. they have been fixed)
         ***************************************/
        if (jiraFieldMappingUtil.shouldTransitionJiraTickets) {
          logger.println("Transitioning ${oldJiraFindings.size()} old finding tickets to: ${jiraFieldMappingUtil.transitionStatus}")
          oldJiraFindings.each{
            transitionTicket(jiraClient, jiraFieldMappingUtil.transitionStatus, it.value)
          }

          logger.println("Transitioning ${oldJiraComponents.size()} old component tickets to: ${jiraFieldMappingUtil.transitionStatus}")
          oldJiraComponents.each{
            transitionTicket(jiraClient, jiraFieldMappingUtil.transitionStatus, it.value)
          }
        }

        /***************************************
           7. Waive findings where the ticket has been closed
         ***************************************/

        //TODO: What tickets in Jira have been closed where we can apply a waiver in IQ
        //      need to deal with the edge cases on this one
        //
        // loop through **repeatJiraFindings**
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

  private void createJiraTickets(Map<String, PolicyViolation> newIQComponents,
                                 Map<String, PolicyViolation> repeatJiraComponentsWithNewFindings,
                                 Map<String, PolicyViolation> newIQFindings,
                                 JiraClient jiraClient,
                                 JiraFieldMappingUtil jiraFieldMappingUtil,
                                 String iqAppExternalId,
                                 String iqOrgExternalId)
  {
    if(jiraFieldMappingUtil.shouldAggregateTicketsByComponent)
    {
      Set<String> createdSubTasks = new HashSet<String>();
      def resp

      logger.println("Creating ${newIQComponents.size()} component tickets")

      newIQComponents.each {
        if (it.value.findingFingerprints.isEmpty())
        {
          logger.println("Skipping Jira Ticket for Component: ${it.value.fingerprintPrettyPrint} - findings have been filtered out")
        }
        else
        {
          resp = createIndividualTicket(jiraClient,
                                        jiraFieldMappingUtil,
                                        it.value,
                                        iqAppExternalId,
                                        iqOrgExternalId,
                                        "TODO: Scan Stage") //TODO

          if (jiraFieldMappingUtil.shouldCreateSubTasksForAggregatedTickets)
          {
            logger.println("Creating ${it.value.findingFingerprints.size()} finding tickets")
            it.value.findingFingerprints.each {
              createSubTask(jiraClient,
                            jiraFieldMappingUtil,
                            resp.key,
                            newIQFindings.get(it), //TODO: I think I need to tie these together more tightly. There's a scenario where a Parent is closed, but the children are open. That should not prevent the creation of a new parent and children (although they'll have the same fingerprint) - What happens then when we go to close the children and there are two of them??? I"ll guess we close one of them. WHAAAAAA
                            iqAppExternalId,
                            iqOrgExternalId,
                            "TODO: Scan Stage") //TODO

              createdSubTasks.add(it)
            }
          }
        }
      }

      if (jiraFieldMappingUtil.shouldCreateSubTasksForAggregatedTickets)
      {
        int moreFindings = newIQFindings.size() - createdSubTasks.size()
        logger.println("Creating ${moreFindings} finding tickets for repeat components")

        newIQFindings.each {
          if (repeatJiraComponentsWithNewFindings.containsKey(it.value.componentFingerprint))
          {
            if (createdSubTasks.contains(it.key) == false)
            {
              createSubTask(jiraClient,
                            jiraFieldMappingUtil,
                            repeatJiraComponentsWithNewFindings.get(it.value.componentFingerprint).ticketExternalId,
                            it.value,
                            iqAppExternalId,
                            iqOrgExternalId,
                            "TODO: Scan Stage") //TODO
            }
          } else
          {
            logger.println("WARNING: skipping creating Jira Sub-task for finding: ${it.value.fingerprintPrettyPrint} because I could not find the parent task for fingerprint: ${it.value.componentFingerprintPrettyPrint}")
          }
        }
      }
    }
    else
    {
      logger.println("Creating ${newIQFindings.size()} finding tickets")
      newIQFindings.each {
        createIndividualTicket(jiraClient,
                               jiraFieldMappingUtil,
                               it.value,
                               iqAppExternalId,
                               iqOrgExternalId,
                               "TODO: Scan Stage") //TODO
        }
    }
  }

  /**
   *
   * @param jiraFieldMappingUtil
   * @param potentialComponentsMap component information from IQ Server
   * @param potentialFindingsMap   policy violations from IQ Server
   * @param currentComponentsMap   component Ticket from Jira
   * @param currentFindingsMap     finding Ticket from Jira
   * @param newIQComponents
   * @param newIQFindings
   * @param repeatJiraComponents
   * @param repeatJiraFindings
   * @param oldJiraComponents
   * @param oldJiraFindings
   */
  private void compareIQServerAndJira(JiraFieldMappingUtil jiraFieldMappingUtil,
                                      Map<String, PolicyViolation> potentialComponentsMap,
                                      Map<String, PolicyViolation> potentialFindingsMap,
                                      Map<String, PolicyViolation> currentComponentsMap,
                                      Map<String, PolicyViolation> currentFindingsMap,
                                      Map<String, PolicyViolation> newIQComponents,
                                      Map<String, PolicyViolation> repeatJiraComponentsWithNewFindings,
                                      Map<String, PolicyViolation> newIQFindings,
                                      Map<String, PolicyViolation> repeatJiraComponents,
                                      Map<String, PolicyViolation> repeatJiraFindings,
                                      Map<String, PolicyViolation> oldJiraComponents,
                                      Map<String, PolicyViolation> oldJiraFindings)
  {
    if(jiraFieldMappingUtil.shouldAggregateTicketsByComponent)
    {
      calculateNewAndRepeatFindings("Component", potentialComponentsMap, currentComponentsMap, newIQComponents, repeatJiraComponents)
      calculateOldFindings("Component", potentialComponentsMap, currentComponentsMap, oldJiraComponents)
    }

    if(jiraFieldMappingUtil.shouldCreateSubTasksForAggregatedTickets || jiraFieldMappingUtil.shouldAggregateTicketsByComponent == false)
    {
      calculateNewAndRepeatFindings("Finding", potentialFindingsMap, currentFindingsMap, newIQFindings, repeatJiraFindings)
      calculateOldFindings("Finding", potentialFindingsMap, currentFindingsMap, oldJiraFindings)

      calculateRepeatComponentsWithNewFindings(newIQFindings, repeatJiraComponents, repeatJiraComponentsWithNewFindings)
    }
  }

  static private void calculateRepeatComponentsWithNewFindings(Map<String, PolicyViolation> pNewIQFindings,
                                                               Map<String, PolicyViolation> pRepeatJiraComponents,
                                                               Map<String, PolicyViolation> pRepeatJiraComponentsWithNewFindings)
  {
    pNewIQFindings.each {
      if (pRepeatJiraComponents.containsKey(it.value.componentFingerprint))
      {
        pRepeatJiraComponentsWithNewFindings.put(it.value.componentFingerprint, pRepeatJiraComponents.get(it.value.componentFingerprint))
      }
    }
  }

  private void calculateNewAndRepeatFindings(String pLoggingLabel,
                                             Map<String, PolicyViolation> pPotentialFindings,
                                             Map<String, PolicyViolation> pCurrentFindings,
                                             Map<String, PolicyViolation> pNewFindings,
                                             Map<String, PolicyViolation> pRepeatFindings)
  {
    pPotentialFindings.each {
      if (pCurrentFindings.containsKey(it.key))
      {
        logger.println("Jira ticket: ${pCurrentFindings.get(it.key).ticketExternalId} already exists for ${pLoggingLabel}: ${it.value.fingerprintPrettyPrint}")
        PolicyViolation pv = pCurrentFindings.get(it.key)
        pRepeatFindings.put(pv.fingerprint, pv)
      } else
      {
        logger.println("Jira ticket: not found for for ${pLoggingLabel}: ${it.value.fingerprintPrettyPrint}")
        pNewFindings.put(it.value.fingerprint, it.value)
      }
    }
  }

  private void calculateOldFindings(String pLoggingLabel,
                                    Map<String, PolicyViolation> pPotentialFindings,
                                    Map<String, PolicyViolation> pCurrentFindings,
                                    Map<String, PolicyViolation> oldFindings)
  {
    pCurrentFindings.each{
      if (pPotentialFindings.containsKey(it.key)){
        logger.println("${pLoggingLabel}: ${pPotentialFindings.get(it.key).fingerprintPrettyPrint} still exists for Jira Ticket: ${it.value.ticketExternalId}")
      } else {
        logger.println("${pLoggingLabel} no longer exists for Jira Ticket: ${it.value.ticketExternalId} - ${it.value.ticketSummary}")
        oldFindings.put(it.value.fingerprint, it.value)
      }
    }
  }

  private void updateTicketScanDate(JiraClient jiraClient, JiraFieldMappingUtil jiraFieldMappingUtil, PolicyViolation pPolicyViolation)
  {
    JiraCustomFieldMappings lastScanDateField = jiraFieldMappingUtil.getLastScanDateCustomField()
    logger.println("Updating Jira Ticket: ${pPolicyViolation.ticketExternalId} - ${pPolicyViolation.ticketSummary} with new scan date in field: ${lastScanDateField.customFieldName} (${lastScanDateField.customFieldId})")

    jiraClient.updateIssueScanDate(jiraFieldMappingUtil, pPolicyViolation.ticketInternalId)
  }

  private void transitionTicket(JiraClient jiraClient, String transitionStatus, PolicyViolation pPolicyViolation)
  {
    logger.println("Transitioning Jira Ticket: ${pPolicyViolation.ticketExternalId} - ${pPolicyViolation.ticketSummary} to Status: ${transitionStatus}")

    jiraClient.closeTicket(pPolicyViolation.ticketInternalId, transitionStatus)
  }

  private def createIndividualTicket(JiraClient jiraClient,
                                     JiraFieldMappingUtil jiraFieldMappingUtil,
                                     PolicyViolation pPolicyViolation,
                                     String iqAppExternalId,
                                     String iqOrgExternalId,
                                     String scanStage)
  {
    logger.println("Creating Jira Ticket in Project: ${jiraFieldMappingUtil.projectKey} for Component: ${pPolicyViolation.fingerprintPrettyPrint}")

    def description
    if (pPolicyViolation.policyName)
    {
      description = "Sonatype IQ Server ${pPolicyViolation.policyName} Policy Violation - ${pPolicyViolation.componentName}"
    }
    else
    {
      description = "Sonatype IQ Server Policy Violation - ${pPolicyViolation.componentName}"
    }

    jiraClient.createIssue(jiraFieldMappingUtil,
                           description,
                           pPolicyViolation.cvssReason,
                           pPolicyViolation.reportLink,
                           pPolicyViolation.policyThreatLevel,
                           pPolicyViolation.fingerprintKey,
                           iqAppExternalId,
                           iqOrgExternalId,
                           scanStage,
                           pPolicyViolation.severity,
                           pPolicyViolation.cveCode,
                           pPolicyViolation.cvssScore,
                           pPolicyViolation.fingerprint)
  }

  private def createSubTask(JiraClient jiraClient,
                                     JiraFieldMappingUtil jiraFieldMappingUtil,
                                     String pParentIssueKey,
                                     PolicyViolation pPolicyViolation,
                                     String iqAppExternalId,
                                     String iqOrgExternalId,
                                     String scanStage)
  {
    logger.println("Creating Jira Sub-task in Project: ${jiraFieldMappingUtil.projectKey} for Finding: ${pPolicyViolation.fingerprintPrettyPrint}")

    String description = "Sonatype IQ Server ${pPolicyViolation.policyName} Policy Violation - ${pPolicyViolation.componentName}"

    jiraClient.createSubTask(jiraFieldMappingUtil,
                             pParentIssueKey,
                             description,
                             pPolicyViolation.cvssReason,
                             pPolicyViolation.reportLink,
                             pPolicyViolation.policyThreatLevel,
                             pPolicyViolation.fingerprintKey,
                             iqAppExternalId,
                             iqOrgExternalId,
                             scanStage,
                             pPolicyViolation.severity,
                             pPolicyViolation.cveCode,
                             pPolicyViolation.cvssScore,
                             pPolicyViolation.fingerprint)
  }

  private void createSummaryTicket(JiraClient jiraClient,
                                   JiraFieldMappingUtil jiraFieldMappingUtil,
                                   PolicyEvaluationHealthAction policyEvaluationHealthAction,
                                   String iqAppExternalId,
                                   String iqReportId)
  {
    //TODO: Create a summary ticket from the policyEvaluationHealthAction - what else should be in a summary ticket?
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
