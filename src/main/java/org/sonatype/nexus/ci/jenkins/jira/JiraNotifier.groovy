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
            final PolicyEvaluationHealthAction policyEvaluationHealthAction)
  {
    checkArgument(!isNullOrEmpty(jiraNotification.projectKey), Messages.JiraNotifier_NoProjectKey())

    IQClient iqClient = IQClientFactory.getIQClient(jiraNotification.jobCredentialsId) //TODO: create separate credentials
    JiraClient jiraClient = JiraClientFactory.getJiraClient(jiraNotification.jobCredentialsId)

    def envVars = run.getEnvironment(listener)
    def projectKey = envVars.expand(jiraNotification.projectKey)
    boolean shouldCreateIndividualTickets = jiraNotification.shouldCreateIndividualTickets
    boolean shouldTransitionJiraTickets = jiraNotification.shouldTransitionJiraTickets
    String transitionStatus = jiraNotification.jiraTransitionStatus
    //TODO: ADD CONFIG FOR FILTERING POLICY NAME (i.e. ONLY CREATE TICKETS FOR SECURITY ISSUES

    logger.println("Creating Jira Tickets for Project: " + projectKey)

    sendPolicyEvaluationHealthAction(iqClient,
                                     jiraClient,
                                     projectKey,
                                     shouldCreateIndividualTickets,
                                     shouldTransitionJiraTickets,
                                     transitionStatus,
                                     PolicyEvaluationHealthAction.build(policyEvaluationHealthAction))
  }

  private void sendPolicyEvaluationHealthAction(final IQClient iqClient,
                                                final JiraClient jiraClient,
                                                final String projectKey,
                                                final boolean shouldCreateIndividualTickets,
                                                final boolean shouldTransitionJiraTickets,
                                                final String transitionStatus,
                                                final PolicyEvaluationHealthAction policyEvaluationHealthAction)
  {
    try {
      if (shouldCreateIndividualTickets) {
        /***************************************
              1. Get Policy Findings from IQ
         ***************************************/

        // IQ Link: http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/3d0fedc4857f44368e0b501a6b986048
        logger.println("IQ Link: " + policyEvaluationHealthAction.reportLink)

        //http://localhost:8060/iq/rest/report/aaaaaaa-testidegrandfathering/a22d44d0209b47358c8dd2532bb7afb3/browseReport/policythreats.json
        def potentialFindings = iqClient.lookupPolcyDetailsFromIQ(policyEvaluationHealthAction.reportLink)
        Map<String, PolicyViolation> potentialFindingsMap = new HashMap<String, PolicyViolation>()
        potentialFindings.aaData.each {
          PolicyViolation.buildFromIQ(it, policyEvaluationHealthAction.reportLink).each {
            potentialFindingsMap.put(it.fingerprint, it)
          }
        }

        /***************************************
            2. Get Tickets from Jira
         ***************************************/

        //http://localhost:8080/rest/api/2/search?jql=project%3D%22JIRAIQ%22
        def currentFindings = jiraClient.lookupJiraTickets(projectKey) //todo: lookup based on IQ App (component? custom field?) and also not closed tickets
        Map<String, PolicyViolation> currentFindingsMap = new HashMap<String, PolicyViolation>()
        currentFindings.issues.each {
          PolicyViolation pv = PolicyViolation.buildFromJira(it)
          currentFindingsMap.put(pv.fingerprint, pv)
        }

        /***************************************
            3. Filter out Existing Tickets
         ***************************************/

        logger.println("Potential Components with Findings from the IQ Server Report: ${potentialFindings.aaData.size}")
        logger.println("Potential Policy Violation Findings from the IQ Server Report: ${potentialFindingsMap.size()}")
        logger.println("Current Findings in Jira before filtering: ${currentFindings.issues.size}")

        //which results from IQ do we need to create, and which alredy exist, and which need to be closed
        //  Loop through potential findings map and current findings map
        Set<PolicyViolation> newFindings = calculateNewFindings(potentialFindingsMap, currentFindingsMap)
        Set<PolicyViolation> repeatFindings = calculateRepeatFindings(potentialFindingsMap, currentFindingsMap)
        Set<PolicyViolation> oldFindings = calculateOldFindings(potentialFindingsMap, currentFindingsMap)

        logger.println("Number of findings that do not have tickets: ${newFindings.size()}")
        logger.println("Number of findings that have existing tickets: ${repeatFindings.size()}")
        logger.println("Number of tickets that no longer have findings: ${oldFindings.size()}")

        /***************************************
          4. Create New Tickets
         ***************************************/

        logger.println("Creating tickets for each policy violation")
        newFindings.each {
          createIndividualTicket(jiraClient, projectKey, it)
        }

        /***************************************
          5. Close Tickets that have no finding (i.e. they have been fixed
         ***************************************/

        if (shouldTransitionJiraTickets) {
          logger.println("Transitioning old tickets to: ${transitionStatus}")
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
        createSummaryTicket(jiraClient, projectKey, policyEvaluationHealthAction)

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

  private Set<PolicyViolation> calculateNewFindings(HashMap<String, PolicyViolation> pPotentialFindings, HashMap<String, PolicyViolation> pCurrentFindings)
  {
    Set<PolicyViolation> returnValue = new HashSet<PolicyViolation>()

    pPotentialFindings.each{
      if (pCurrentFindings.containsKey(it.key)){
        //TODO: make the fingerprints match
        logger.println("Jira ticket: ${pCurrentFindings.get(it.key).ticketExternalId} already exists for finding: ${it.value.fingerprintKey}")
      } else {
        returnValue.add(it.value)
      }
    }

    return returnValue
  }

  private Set<PolicyViolation> calculateRepeatFindings(HashMap<String, PolicyViolation> pPotentialFindings, HashMap<String, PolicyViolation> pCurrentFindings)
  {
    Set<PolicyViolation> returnValue = new HashSet<PolicyViolation>()

    //TODO: combine with calculateNewFindings() so I don't have to loop through the map twice
    pPotentialFindings.each{
      if (pCurrentFindings.containsKey(it.key)){
        //TODO: make the fingerprints match
        //TODO: Should I only look up Open Tickets? What about waiving closed violations?
        returnValue.add(it.value)
      } else {
        logger.println("Jira ticket not found for finding: ${it.value.fingerprintKey}")
      }
    }

    return returnValue
  }

  private Set<PolicyViolation> calculateOldFindings(HashMap<String, PolicyViolation> pPotentialFindings, HashMap<String, PolicyViolation> pCurrentFindings)
  {
    Set<PolicyViolation> returnValue = new HashSet<PolicyViolation>()

    pCurrentFindings.each{
      if (pPotentialFindings.containsKey(it.key)){
        //TODO: make the fingerprints match
        logger.println("Finding: ${pPotentialFindings.get(it.key).fingerprintKey} still exists for Jira Ticket: ${it.value.ticketExternalId}")
      } else {
        returnValue.add(it.value)
      }
    }

    return returnValue
  }

  private void transitionTicket(JiraClient jiraClient, String transitionStatus, PolicyViolation pPolicyViolation)
  {
    logger.println("Transitioning Jira Ticket: ${pPolicyViolation.ticketExternalId} to Status: ${transitionStatus}")

    jiraClient.closeTicket(pPolicyViolation.ticketInternalId, transitionStatus)
  }

  private void createIndividualTicket(JiraClient jiraClient, String projectKey, PolicyViolation pPolicyViolation)
  {
    logger.println("Creating Jira Ticket in Project: ${projectKey} for Component: ${pPolicyViolation.fingerprintKey}")

    //TODO: pull from PV
    def description = "Sonatype IQ Server ${pPolicyViolation.policyName} Policy Violation - ${pPolicyViolation.componentName}"
    def detail = "CVE-2019-1234"
    def source = "SonatypeIQ:IQServerAppId:scanIQ"
    def severity = 1
    def fprint = "SONATYPEIQ-APPID-COMPONENTID-SVCODE"

    jiraClient.createIssue(projectKey,
                           description,
                           detail,
                           source,
                           severity,
                           fprint)
  }

  private void createSummaryTicket(JiraClient jiraClient, String projectKey, PolicyEvaluationHealthAction policyEvaluationHealthAction)
  {
    //TODO: Create a summary ticket from the policyEvaluationHealthAction
    logger.println("Creating Summary Jira Ticket for Project: ${projectKey}")

    def description = "Sonatype IQ Server Summary of Violations - three"
    def detail = "CVE-2019-1234"
    def source = "SonatypeIQ:IQServerAppId:scanIQ"
    def severity = 1
    def fprint = "SONATYPEIQ-APPID-COMPONENTID-SVCODE"

    jiraClient.createIssue(projectKey,
                           description,
                           detail,
                           source,
                           severity,
                           fprint)

  }

}
