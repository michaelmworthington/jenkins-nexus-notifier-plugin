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

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import hudson.AbortException
import hudson.model.Run
import hudson.model.TaskListener
import org.sonatype.nexus.ci.jenkins.iq.IQClient
import org.sonatype.nexus.ci.jenkins.iq.IQClientFactory
import org.sonatype.nexus.ci.jenkins.model.IQVersionRecommendation
import org.sonatype.nexus.ci.jenkins.model.PolicyEvaluationHealthAction
import org.sonatype.nexus.ci.jenkins.model.PolicyViolation
import org.sonatype.nexus.ci.jenkins.notifier.ContinuousMonitoringConfig
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

  void continuousMonitor(final String dynamicData, final ContinuousMonitoringConfig continuousMonitoringConfig, final JiraNotification jiraNotification)
  {
    logger.println("######################################")
    logger.println("Starting Jira Continuous Monitoring")
    logger.println("######################################")

    String singleAppId = continuousMonitoringConfig.applicationName
    String singleStage = continuousMonitoringConfig.stage

    //if the application id is specified, use that and ignore the dynamic data
    if (singleAppId)
    {
      checkArgument(!isNullOrEmpty(singleStage), "Continuous Monitoring Stage is required when specifying an application name")

      logger.println("#################################################################################################################")
      logger.println("Running Jira Continuous Monitoring for Single Application : ${singleAppId} at Stage : ${singleStage}")
      logger.println("#################################################################################################################")

      runContinuousMonitorForApp(dynamicData, singleAppId, singleStage, jiraNotification, continuousMonitoringConfig.shouldUpdateLastScanDate)
    }
    else
    {
      checkArgument(!isNullOrEmpty(continuousMonitoringConfig.dynamicDataApplicationKey), "Continuous Monitoring Dynamic Application Key or Application Name required")
      checkArgument(!isNullOrEmpty(dynamicData), "Dynamic Data required when looking up Applications using Continuous Monitoring Dynamic Application Key")

      String appKeyFieldName = continuousMonitoringConfig.dynamicDataApplicationKey
      def dynamicDataJson = new JsonSlurper().parseText(dynamicData)

      int current = 1
      int total = dynamicDataJson.size
      String stageToUse, appIdToUse
      dynamicDataJson.each{
        checkArgument(!isNullOrEmpty(it[appKeyFieldName]), "Application Name Value not found for key : ${appKeyFieldName} for Dynamic Data : ${it}")
        appIdToUse = it[appKeyFieldName]

        if(singleStage)
        {
          stageToUse = singleStage
        }
        else
        {
          checkArgument(!isNullOrEmpty(continuousMonitoringConfig.dynamicDataStageKey), "Continuous Monitoring Dynamic Stage Key or Stage Name required")
          stageToUse = it[continuousMonitoringConfig.dynamicDataStageKey]
          checkArgument(!isNullOrEmpty(stageToUse), "Continuous Monitoring Dynamic Stage Key Value : ${continuousMonitoringConfig.dynamicDataStageKey} not found for application : ${appIdToUse}")
        }

        logger.println("######################################################################")
        logger.println("Running Jira Continuous Monitoring for Application ${current}/${total} : ${appIdToUse} at Stage : ${stageToUse}")
        logger.println("######################################################################")
        runContinuousMonitorForApp(new JsonBuilder([it]).toString(), appIdToUse, stageToUse, jiraNotification, continuousMonitoringConfig.shouldUpdateLastScanDate)

        current++
      }
    }
  }

  private void runContinuousMonitorForApp(final String dynamicData, final String pAppId, final String pStage, final JiraNotification jiraNotification, final boolean shouldUpdateLastScanDate)
  {
    try
    {
      IQClient iqClient = IQClientFactory.getIQClient(jiraNotification.jobIQCredentialsId,
                                                      logger,
                                                      jiraNotification.verboseLogging,
                                                      jiraNotification.disableIQCVEDetails,
                                                      jiraNotification.disableIQRemediationRecommendation)

      PolicyEvaluationHealthAction policyEvaluationHealthAction = new PolicyEvaluationHealthAction()
      //TODO: this does have the date, which may be useful for setting the date on jira tickets based on the report rather than current time
      policyEvaluationHealthAction.reportLink = iqClient.lookupReportLink(pAppId, pStage)

      //todo: skip updating the last scan date, but it still needs to be set for new tickets
      send(true, dynamicData, jiraNotification, policyEvaluationHealthAction, shouldUpdateLastScanDate)
    }
    catch (Throwable ex)
    {
      logger.println("Could not initialize the Nexus Notifier Plugin. Please check Jira & IQ Server login credentials")
      logger.println(ex.message)
      ex.printStackTrace(logger)
      throw new AbortException(ex.message)
    }
  }

  void send(final boolean buildPassing, //TODO: don't run if the build is failed??? Maybe the check on the IQ Report will fail the process, but make sure that we don't close all the jira tickets if there is an upstream failure
            final String dynamicData,
            final JiraNotification jiraNotification,
            final PolicyEvaluationHealthAction pPolicyEvaluationHealthAction,
            final boolean shouldUpdateLastScanDate = true)
  {
    checkArgument(!isNullOrEmpty(jiraNotification.projectKey), Messages.JiraNotifier_NoProjectKey())
    checkArgument(!isNullOrEmpty(jiraNotification.issueTypeName), Messages.JiraNotifier_NoIssueType())
    if(jiraNotification.shouldCreateSubTasksForAggregatedTickets)
    {
      checkArgument(!isNullOrEmpty(jiraNotification.subTaskIssueTypeName), Messages.JiraNotifier_NoSubTaskIssueType())
    }
    //todo: other required field mappings - may need to be done at the appropriate location depending on what type of tickets are being created

    def dynamicDataJson = null
    if(dynamicData)
    {
      dynamicDataJson = new JsonSlurper().parseText(dynamicData)
      checkArgument(dynamicDataJson?.size == 1, "When running for a single app, only dynamic data with one object is supported.")
    }

    IQClient iqClient
    JiraClient jiraClient
    JiraFieldMappingUtil jiraFieldMappingUtil
    PolicyEvaluationHealthAction policyEvaluationHealthAction

    try
    {
      iqClient = IQClientFactory.getIQClient(jiraNotification.jobIQCredentialsId,
                                             logger,
                                             jiraNotification.verboseLogging,
                                             jiraNotification.disableIQCVEDetails,
                                             jiraNotification.disableIQRemediationRecommendation)
      jiraClient = JiraClientFactory.getJiraClient(jiraNotification.jobJiraCredentialsId,
                                                   logger,
                                                   jiraNotification.verboseLogging,
                                                   jiraNotification.dryRun,
                                                   jiraNotification.disableJqlFieldFilter,
                                                   jiraNotification.jqlMaxResultsOverride)
      jiraFieldMappingUtil = new JiraFieldMappingUtil(dynamicDataJson, jiraNotification, jiraClient, run.getEnvironment(listener), logger)
      policyEvaluationHealthAction = PolicyEvaluationHealthAction.build(pPolicyEvaluationHealthAction)
    }
    catch (Throwable ex)
    {
      logger.println("Could not initialize the Nexus Notifier Plugin. Please check Jira & IQ Server login credentials") //fix catching a general error log
      logger.println(ex.message)
      ex.printStackTrace(logger)
      throw new AbortException(ex.message)
    }

    try
    {
      logger.println("######################################")
      logger.println("Creating Jira Tickets for Project: ${jiraFieldMappingUtil.projectKey} with issue type: ${jiraFieldMappingUtil.issueTypeName} and priority: ${jiraFieldMappingUtil.priorityName}")
      logger.println("######################################")

      // IQ Link: http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/3d0fedc4857f44368e0b501a6b986048
      logger.println("IQ Link: " + policyEvaluationHealthAction.reportLink)
      jiraFieldMappingUtil.getIqServerReportLinkCustomField().customFieldValue = policyEvaluationHealthAction.reportLink

      String[] linkPieces = policyEvaluationHealthAction.reportLink.split("/")
      String iqReportInternalid = linkPieces[linkPieces.length-1]
      String iqAppExternalId = linkPieces[linkPieces.length-3]
      jiraFieldMappingUtil.getApplicationCustomField().customFieldValue = iqAppExternalId
      jiraFieldMappingUtil.getScanStageCustomField().customFieldValue = iqClient.lookupStageForReport(iqAppExternalId, iqReportInternalid)
      jiraFieldMappingUtil.setScanInternalId(iqReportInternalid)

      try
      {
        jiraFieldMappingUtil.getOrganizationCustomField().customFieldValue = iqClient.lookupOrganizationName(iqAppExternalId)
      }
      catch (Exception e)
      {
        logger.print("INFO: Error occurred while looking up the IQ Organization for the application.")
        logger.print("INFO: The most likely cause is the user does not have view access to the organization.")
        logger.print("INFO: Error Message: ${e.message}")
      }

      //todo: skip for continuous monitoring?
      jiraFieldMappingUtil.getLastScanDateCustomField().customFieldValue = jiraFieldMappingUtil.getFormattedScanDateForJira()

      if (jiraFieldMappingUtil.shouldCreateIndividualTickets)
      {
        //todo: other required field mappings
        checkArgument(!isNullOrEmpty(jiraFieldMappingUtil.getViolationIdCustomField().customFieldId),
                      "Custom Field mapping for Policy Violation ID must be set when creating individual tickets. Please validate the field name: ${jiraFieldMappingUtil.getViolationIdCustomField().customFieldName}")

        // Data from IQ Server ("potential") and JIRA ("current") mapped by Fingerprint
        Map<String, PolicyViolation> potentialComponentsMap = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> potentialFindingsMap = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> currentComponentsMap = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> currentFindingsMap = new HashMap<String, PolicyViolation>()

        // Deduplicated findings
        Map<String, PolicyViolation> newIQComponents = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> repeatJiraComponentsWithNewFindings = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> repeatJiraFindingsWithNewComponents = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> newIQFindings = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> repeatJiraComponents = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> repeatJiraFindings = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> oldJiraComponents = new HashMap<String, PolicyViolation>()
        Map<String, PolicyViolation> oldJiraFindings = new HashMap<String, PolicyViolation>()


        /***************************************
              1. Get Policy Findings from IQ
         ***************************************/

        //http://localhost:8060/iq/rest/report/aaaaaaa-testidegrandfathering/a22d44d0209b47358c8dd2532bb7afb3/browseReport/policythreats.json
        def potentialFindings = iqClient.lookupPolcyDetailsFromIQ(iqReportInternalid, jiraFieldMappingUtil.getApplicationCustomField().customFieldValue)

        //http://localhost:8060/iq/api/v2/applications/aaaaaaa-testidegrandfathering/reports/a22d44d0209b47358c8dd2532bb7afb3
        def findingComponents = iqClient.lookupComponentDetailsFromIQ(iqReportInternalid, jiraFieldMappingUtil.getApplicationCustomField().customFieldValue)

        String cveLinkBaseUrl = iqClient.lookupCveLinkBaseUrl()

        logger.println("Parsing findings from the IQ Server Report: ${potentialFindings.aaData.size}")
        potentialFindings.aaData.each { policyFinding ->
          def rawData = findingComponents.components.find({it.hash == policyFinding.hash})

          PolicyViolation.buildFromIQ(potentialComponentsMap,
                                      potentialFindingsMap,
                                      policyFinding,
                                      rawData,
                                      policyEvaluationHealthAction.reportLink,
                                      cveLinkBaseUrl,
                                      jiraFieldMappingUtil.getApplicationCustomField().customFieldValue,
                                      jiraFieldMappingUtil.policyFilterPrefix,
                                      jiraFieldMappingUtil.policyFilterThreatLevel)
        }

        /***************************************
            2. Get Tickets from Jira
         ***************************************/

        lookupJiraTickets(jiraClient, jiraFieldMappingUtil, currentFindingsMap, currentComponentsMap, 0)

        /***************************************
            3. Filter out Existing Tickets
         ***************************************/
        //TODO: filter logging messages, and maybe input based on aggregation and sub-task creation
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
                               repeatJiraFindingsWithNewComponents,
                               newIQFindings,
                               repeatJiraComponents,
                               repeatJiraFindings,
                               oldJiraComponents,
                               oldJiraFindings)

        //TODO: filter logging messages, and maybe input based on aggregation and sub-task creation
        logger.println("######################################")
        logger.println("        Compare Output")
        logger.println("")
        logComponentAndSubTaskConfig(jiraFieldMappingUtil)
        logger.println("")
        logger.println("######################################")
        logger.println("Number of components that do not have tickets      : ${newIQComponents.size()}")
        logger.println("Number of findings   that do not have tickets      : ${newIQFindings.size()}")
        logger.println("Number of components that have existing tickets    : ${repeatJiraComponents.size()} (${repeatJiraComponentsWithNewFindings.size()} have new findings)")
        logger.println("Number of findings   that have existing tickets    : ${repeatJiraFindings.size()} (${repeatJiraFindingsWithNewComponents.size()} have new components)")
        logger.println("Number of tickets    that no longer have components: ${oldJiraComponents.size()}")
        logger.println("Number of tickets    that no longer have findings  : ${oldJiraFindings.size()}")
        logger.println("######################################")
        logger.println("######################################")

        /***************************************
          4. Create New Tickets
         ***************************************/

        //From now on, we can treat the repeat jira findings as new iq findings
        newIQFindings.putAll(repeatJiraFindingsWithNewComponents)

        createJiraTickets(newIQComponents,
                          repeatJiraComponentsWithNewFindings,
                          newIQFindings,
                          iqClient,
                          jiraClient,
                          jiraFieldMappingUtil)

        /***************************************
         5. Update Scan Time on repeat findings
         ***************************************/

        if(shouldUpdateLastScanDate)
        {
          logger.println("Updating ${repeatJiraComponents.size()} repeat component tickets with the new scan date")
          repeatJiraComponents.each {
            updateTicketScanDate(jiraClient, jiraFieldMappingUtil, it.value)
          }

          logger.println("Updating ${repeatJiraFindings.size()} repeat finding tickets with the new scan date")
          repeatJiraFindings.each {
            updateTicketScanDate(jiraClient, jiraFieldMappingUtil, it.value)
          }
        }
        else
        {
          logger.println("Skipping the update to last scan date due to configuration.")
        }

        /***************************************
          6. Close Tickets that have no finding (i.e. they have been fixed)
         ***************************************/
        if (jiraFieldMappingUtil.shouldTransitionJiraTickets) {
          logger.println("Transitioning ${oldJiraFindings.size()} old finding tickets to: ${jiraFieldMappingUtil.transitionStatus} using transition: ${jiraFieldMappingUtil.transitionName}")
          oldJiraFindings.each{
            transitionTicket(jiraClient, jiraFieldMappingUtil.transitionName, it.value)
          }

          //todo: log only if configured to manage component (?aggregated?) tickets
          logger.println("Transitioning ${oldJiraComponents.size()} old component tickets to: ${jiraFieldMappingUtil.transitionStatus} using transition: ${jiraFieldMappingUtil.transitionName}")
          oldJiraComponents.each{
            transitionTicket(jiraClient, jiraFieldMappingUtil.transitionName, it.value)
          }
        }

        /***************************************
           7. Waive findings where the ticket has been closed
         ***************************************/

        //TODO: What tickets in Jira have been closed where we can apply a waiver in IQ
        //      need to deal with the edge cases on this one
        //
        // loop through **repeatJiraFindings**
        //TODO: Futures
        //TODO:   {{iqURL}}/rest/policyWaiver/application/{{iqAppExternalId}}
        //TODO:   service now

      }
      else
      {
        logger.println("Creating just one ticket with a violation summary in the description")
        createSummaryTicket(jiraClient, jiraFieldMappingUtil, policyEvaluationHealthAction, jiraFieldMappingUtil.getApplicationCustomField().customFieldValue, iqReportInternalid)

        if (jiraFieldMappingUtil.shouldTransitionJiraTickets)
        {
          logger.println("Skipping Transitioning tickets when in Summary Mode")
        }
      }
    }
    catch (ex)
    {
      logger.println(ex.message)
      ex.printStackTrace(logger)
      throw new AbortException(ex.message)
    }
  }

  void logComponentAndSubTaskConfig(JiraFieldMappingUtil jiraFieldMappingUtil)
  {
    logger.println("Aggregate by Component: ${jiraFieldMappingUtil.shouldAggregateTicketsByComponent}")
    logger.println("Create Subtasks: ${jiraFieldMappingUtil.shouldCreateSubTasksForAggregatedTickets}")

    if(jiraFieldMappingUtil.shouldAggregateTicketsByComponent)
    {
      logger.println("  * Component results should not be zero, and they will be created as type: ${jiraFieldMappingUtil.issueTypeName}")

      if(jiraFieldMappingUtil.shouldCreateSubTasksForAggregatedTickets)
      {
        logger.println("  * And, Finding results should not be zero, and they will be created as type: ${jiraFieldMappingUtil.subTaskIssueTypeName}")
      }
      else
      {
        logger.println("  * And, Finding results will be zero.")
      }
    }
    else
    {
      logger.println("  * Component results will be zero.")
      logger.println("  * But, you should still have Findings, and they will be created as type: ${jiraFieldMappingUtil.issueTypeName}")
    }
  }

  /**
   * http://localhost:8080/rest/api/2/search?jql=project%3D%22JIRAIQ%22
   *
   * @param jiraClient
   * @param jiraFieldMappingUtil
   * @param currentFindingsMap
   * @param currentComponentsMap
   * @param pStartAtIndex
   */
  private void lookupJiraTickets(JiraClient jiraClient,
                                 JiraFieldMappingUtil jiraFieldMappingUtil,
                                 Map<String, PolicyViolation> currentFindingsMap,
                                 Map<String, PolicyViolation> currentComponentsMap,
                                 int pStartAtIndex)
  {
    def currentFindings = jiraClient.lookupJiraTickets(jiraFieldMappingUtil, pStartAtIndex)

    logger.println("Parsing findings from Jira: ${currentFindings.issues.size} [start: ${pStartAtIndex}, maxResults: ${currentFindings.maxResults}, total: ${currentFindings.total}]")
    currentFindings.issues.each {
      PolicyViolation pv = PolicyViolation.buildFromJira(it, jiraFieldMappingUtil.getViolationIdCustomField().customFieldId)

      if (jiraFieldMappingUtil.shouldAggregateTicketsByComponent)
      {
        if ("Sub-task" == pv.ticketType)
        {
          currentFindingsMap.put(pv.fingerprint, pv)
        } else
        {
          currentComponentsMap.put(pv.fingerprint, pv)
        }
      } else
      {
        if ("Sub-task" == pv.ticketType)
        {
          logger.println("WARNING: Skipping Jira Ticket ${pv.ticketExternalId} since it is a Sub-task and tickets are not being aggregated")
        } else
        {
          currentFindingsMap.put(pv.fingerprint, pv)
        }
      }
    }

//todo: double check this - found the scenario when maxresultsoverride was zero
    int endOfSearchPage = currentFindings.maxResults + currentFindings.startAt

    if (endOfSearchPage == pStartAtIndex && currentFindings.total > 0)
    {
      throw new RuntimeException("Invalid Configuration: Search start and finish are the same.")
    }

    if (currentFindings.total > (currentFindings.maxResults + currentFindings.startAt))
    {
      lookupJiraTickets(jiraClient,
                        jiraFieldMappingUtil,
                        currentFindingsMap,
                        currentComponentsMap,
                        currentFindings.startAt + currentFindings.maxResults)
    }
  }

  private void createJiraTickets(Map<String, PolicyViolation> newIQComponents,
                                 Map<String, PolicyViolation> repeatJiraComponentsWithNewFindings,
                                 Map<String, PolicyViolation> newIQFindings,
                                 IQClient iqClient,
                                 JiraClient jiraClient,
                                 JiraFieldMappingUtil jiraFieldMappingUtil)
  {
    String iqApplicationInternalId = iqClient.lookupApplication(jiraFieldMappingUtil.getApplicationCustomField().customFieldValue)?.applications?.getAt(0)?.id

    if(jiraFieldMappingUtil.shouldAggregateTicketsByComponent)
    {
      Set<String> createdSubTasks = new HashSet<String>()
      def resp

      logger.println("Creating ${newIQComponents.size()} component tickets")

      newIQComponents.each { fingerprint, policyViolation ->
        if (policyViolation.findingFingerprints.isEmpty())
        {
          logger.println("Skipping Jira Ticket for Component: ${policyViolation.fingerprintPrettyPrint} - findings have been filtered out")
        }
        else
        {
          //lookup recommended version from IQ Server
          //Because it's another API call, do it only when creating tickets
          safeLookupRecommendedVersion(jiraFieldMappingUtil, policyViolation, iqClient, iqApplicationInternalId)
          safeLookupCWEAndThreatVector(policyViolation, iqClient)

          resp = createIndividualTicket(jiraClient,
                                        jiraFieldMappingUtil,
                                        policyViolation)

          if (jiraFieldMappingUtil.shouldCreateSubTasksForAggregatedTickets)
          {
            logger.println("Creating ${policyViolation.findingFingerprints.size()} finding tickets")
            policyViolation.findingFingerprints.each {
              PolicyViolation childPolicyViolation = newIQFindings.get(it)

              //copy recommended version from parent
              childPolicyViolation.recommendedRemediation = policyViolation.recommendedRemediation
              safeLookupCWEAndThreatVector(childPolicyViolation, iqClient)

              createSubTask(jiraClient,
                            jiraFieldMappingUtil,
                            resp.key,
                            childPolicyViolation)

              createdSubTasks.add(it)
            }
          }
        }
      }

      if (jiraFieldMappingUtil.shouldCreateSubTasksForAggregatedTickets)
      {
        int moreFindings = newIQFindings.size() - createdSubTasks.size()
        logger.println("Creating ${moreFindings} finding tickets for repeat components")

        newIQFindings.each { fingerprint, policyViolation ->
          if (!createdSubTasks.contains(fingerprint))
          {
            if (repeatJiraComponentsWithNewFindings.containsKey(policyViolation.componentFingerprint))
            {
              //lookup recommended version from IQ Server
              //Because it's another API call, do it only when creating tickets
              safeLookupRecommendedVersion(jiraFieldMappingUtil, policyViolation, iqClient, iqApplicationInternalId)
              safeLookupCWEAndThreatVector(policyViolation, iqClient)

              createSubTask(jiraClient,
                            jiraFieldMappingUtil,
                            repeatJiraComponentsWithNewFindings.get(policyViolation.componentFingerprint).ticketExternalId,
                            policyViolation)
            }
            else
            {
              logger.println("WARNING: skipping creating Jira Sub-task for finding: ${policyViolation.fingerprintPrettyPrint} because I could not find the parent task for fingerprint: ${policyViolation.componentFingerprintPrettyPrint}")
            }
          }
        }
      }
    } else
    {
      logger.println("Creating ${newIQFindings.size()} finding tickets")
      newIQFindings.each { fingerprint, policyViolation ->
        //lookup recommended version from IQ Server
        //Because it's another API call, do it only when creating tickets
        safeLookupRecommendedVersion(jiraFieldMappingUtil, policyViolation, iqClient, iqApplicationInternalId)
        safeLookupCWEAndThreatVector(policyViolation, iqClient)

        createIndividualTicket(jiraClient,
                               jiraFieldMappingUtil,
                               policyViolation)
        }
    }
  }

  private static void safeLookupRecommendedVersion(JiraFieldMappingUtil jiraFieldMappingUtil, PolicyViolation policyViolation, IQClient iqClient, String iqApplicationInternalId)
  {
    //todo: flagging based on PURL is not quite accurate,
    // remediation API was released in 64, PURL was released in 67.
    // at the end of the day, i'm assuming a minimum supported version of 67
    // (I have a version check now in IQClient, but I don't really feel like changing it - and don't want to
    // make another API call for each recommendation lookup, and don't want to code a caching mechanism)
    if (policyViolation.packageUrl)
    {
      policyViolation.recommendedRemediation = new IQVersionRecommendation(iqClient.lookupRecommendedVersion(policyViolation.packageUrl,
                                                                                                             jiraFieldMappingUtil.getScanStageCustomField().customFieldValue,
                                                                                                             iqApplicationInternalId),
                                                                           jiraFieldMappingUtil.getScanStageCustomField().customFieldValue)
    }
  }

  private static void safeLookupCWEAndThreatVector(PolicyViolation policyViolation, IQClient iqClient)
  {
    if (policyViolation.cveCode)
    {
      String[] resp = iqClient.lookupCweAndThreatVector(policyViolation.cveCode)
      policyViolation.cweCode = resp[0]
      policyViolation.threatVector = resp[1]
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
                                      Map<String, PolicyViolation> repeatJiraFindingsWithNewComponents,
                                      Map<String, PolicyViolation> newIQFindings,
                                      Map<String, PolicyViolation> repeatJiraComponents,
                                      Map<String, PolicyViolation> repeatJiraFindings,
                                      Map<String, PolicyViolation> oldJiraComponents,
                                      Map<String, PolicyViolation> oldJiraFindings)
  {
    if(jiraFieldMappingUtil.shouldAggregateTicketsByComponent)
    {
      //todo: other required field mappings

      calculateNewAndRepeatFindings("Component", potentialComponentsMap, currentComponentsMap, newIQComponents, repeatJiraComponents)
      calculateOldFindings("Component", potentialComponentsMap, currentComponentsMap, oldJiraComponents)
    }

    if(jiraFieldMappingUtil.shouldCreateSubTasksForAggregatedTickets || !jiraFieldMappingUtil.shouldAggregateTicketsByComponent)
    {
      //todo: other required field mappings

      calculateNewAndRepeatFindings("Finding", potentialFindingsMap, currentFindingsMap, newIQFindings, repeatJiraFindings)
      calculateOldFindings("Finding", potentialFindingsMap, currentFindingsMap, oldJiraFindings)

      // This is to find new violations for existing components (i.e. a newly discovered security vuln for a component that already had vulns)
      calculateRepeatComponentsWithNewFindings(newIQFindings, repeatJiraComponents, repeatJiraComponentsWithNewFindings)
      //this is for an edge case where the parent ticket is closed, but the sub-task is still open, and we are creating a new component
      calculateRepeatFindingsWithNewComponents(newIQComponents, repeatJiraFindings, potentialFindingsMap, repeatJiraFindingsWithNewComponents)
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

  static private void calculateRepeatFindingsWithNewComponents(Map<String, PolicyViolation> pNewIQComponents,
                                                               Map<String, PolicyViolation> pRepeatJiraFindings,
                                                               Map<String, PolicyViolation> pPotentialFindingsMap,
                                                               Map<String, PolicyViolation> pRepeatJiraFindingsWithNewComponents)
  {
    pRepeatJiraFindings.each {
      PolicyViolation repeatIQFinding = pPotentialFindingsMap.get(it.key)
      if (pNewIQComponents.containsKey(repeatIQFinding.componentFingerprint))
      {
        pRepeatJiraFindingsWithNewComponents.put(repeatIQFinding.fingerprint, repeatIQFinding)
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
    if(lastScanDateField.customFieldId)
    {
      logger.println("Updating Jira Ticket: ${pPolicyViolation.ticketExternalId} - ${pPolicyViolation.ticketSummary} with new scan date in field: ${lastScanDateField.customFieldName} (${lastScanDateField.customFieldId})")
      jiraClient.updateIssueScanDate(jiraFieldMappingUtil, pPolicyViolation.ticketInternalId)
    }
    else
    {
      logger.println("Skipping updating Jira Ticket: ${pPolicyViolation.ticketExternalId} - ${pPolicyViolation.ticketSummary} because scan date field has not been mapped: ${lastScanDateField.customFieldName} (${lastScanDateField.customFieldId})")
    }
  }

  private void transitionTicket(JiraClient jiraClient, String transitionName, PolicyViolation pPolicyViolation)
  {
    logger.println("Transitioning Jira Ticket: ${pPolicyViolation.ticketExternalId} - ${pPolicyViolation.ticketSummary} using transition: ${transitionName}")

    jiraClient.closeTicket(pPolicyViolation.ticketInternalId, transitionName)
  }

  private def createIndividualTicket(JiraClient jiraClient,
                                     JiraFieldMappingUtil jiraFieldMappingUtil,
                                     PolicyViolation pPolicyViolation)
  {
    logger.println("Creating Jira Ticket in Project: ${jiraFieldMappingUtil.projectKey} for Component: ${pPolicyViolation.fingerprintPrettyPrint}")

    pPolicyViolation.detectDateString = jiraFieldMappingUtil.getFormattedScanDateForJira()
    jiraClient.createIssue(jiraFieldMappingUtil, pPolicyViolation)
  }

  private def createSubTask(JiraClient jiraClient,
                            JiraFieldMappingUtil jiraFieldMappingUtil,
                            String pParentIssueKey,
                            PolicyViolation pPolicyViolation)
  {
    logger.println("Creating Jira Sub-task in Project: ${jiraFieldMappingUtil.projectKey} for Finding: ${pPolicyViolation.fingerprintPrettyPrint}")

    pPolicyViolation.detectDateString = jiraFieldMappingUtil.getFormattedScanDateForJira()
    jiraClient.createSubTask(jiraFieldMappingUtil, pParentIssueKey, pPolicyViolation)
  }

  private void createSummaryTicket(JiraClient jiraClient,
                                   JiraFieldMappingUtil jiraFieldMappingUtil,
                                   PolicyEvaluationHealthAction policyEvaluationHealthAction,
                                   String iqAppExternalId,
                                   String iqReportId)
  {
    //TODO: Create a summary ticket from the policyEvaluationHealthAction - what else should be in a summary ticket?
    logger.println("Creating Summary Jira Ticket for Project: ${jiraFieldMappingUtil.projectKey}")

    def detail = "${policyEvaluationHealthAction.affectedComponentCount} components"
    def fprint = "SONATYPEIQ-${iqAppExternalId}-${iqReportId}"

    PolicyViolation policyViolationSummary = new PolicyViolation(reportLink: policyEvaluationHealthAction.reportLink,
                                                                 fingerprintKey: fprint,
                                                                 cvssReason: detail,
                                                                 policyThreatLevel: 1)

    jiraClient.createIssue(jiraFieldMappingUtil, policyViolationSummary)
  }
}
