package org.sonatype.nexus.ci.jenkins.util

import hudson.AbortException
import hudson.EnvVars
import org.sonatype.nexus.ci.jenkins.jira.JiraClient
import org.sonatype.nexus.ci.jenkins.model.PolicyViolation
import org.sonatype.nexus.ci.jenkins.notifier.JiraCustomFieldMappings
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification

class JiraFieldMappingUtil
{
  private def iDynamicDataJsonOne
  private JiraNotification iJiraNotification
  private JiraClient jiraClient
  private EnvVars iEnvVars
  private PrintStream logger
  private Date scanDate
  private String scanInternalId

  String projectKey
  String issueTypeName
  String subTaskIssueTypeName
  String priorityName
  Boolean shouldCreateIndividualTickets
  Boolean shouldTransitionJiraTickets
  String transitionStatus
  String transitionName

  private String applicationCustomFieldName
  private String organizationCustomFieldName
  private String scanStageCustomFieldName
  private String violationIdCustomFieldName
  private String violationDetectDateCustomFieldName
  private String lastScanDateCustomFieldName
  private String severityCustomFieldName
  private String maxSeverityCustomFieldName
  private String cveCodeCustomFieldName
  private String maxCveCodeCustomFieldName
  private String cveLinkCustomFieldName
  private String maxCveLinkCustomFieldName
  private String cweCodeCustomFieldName //todo
  private String maxCweCodeCustomFieldName //todo
  private String threatVectorCustomFieldName //todo
  private String maxThreatVectorCustomFieldName //todo
  private String cvssCustomFieldName
  private String maxCvssCustomFieldName
  private String iqServerReportLinkCustomFieldName
  private String iqServerPolicyViolationNameCustomFieldName
  private String maxIqServerPolicyViolationNameCustomFieldName
  private String iqServerPolicyViolationThreatLevelCustomFieldName
  private String maxIqServerPolicyViolationThreatLevelCustomFieldName
  private String declaredLicensesCustomFieldName
  private String observedLicensesCustomFieldName
  private String effectiveLicensesCustomFieldName
  private String fileOccurrencesCustomFieldName
  private String recommendedRemediationCustomFieldName
  private String purlCustomFieldName
  private String componentCombinedIdentifierCustomFieldName
  private String componentGroupCustomFieldName
  private String componentNameCustomFieldName
  private String componentVersionCustomFieldName
  private String componentClassifierCustomFieldName
  private String componentExtensionCustomFieldName

  String policyFilterPrefix
  int policyFilterThreatLevel
  String jiraDateFormatOverride
  Boolean shouldAggregateTicketsByComponent
  Boolean shouldCreateSubTasksForAggregatedTickets

  private List<JiraCustomFieldMappings> jiraCustomFieldMappings

  //Custom Field IDs
  //  Populated by calling the JIRA REST API to get all of the custom fields,
  //  then looking up by name to get the ID
  private Map<String, JiraCustomFieldMappings> validatedGlobalCustomFieldMappings
  private Map<String, JiraCustomFieldMappings> validatedViolationCustomFieldMappings

  JiraCustomFieldMappings getApplicationCustomField() { return validatedGlobalCustomFieldMappings.get(applicationCustomFieldName)  ?: new JiraCustomFieldMappings("Stub App", null, null, null) }
  JiraCustomFieldMappings getOrganizationCustomField() { return validatedGlobalCustomFieldMappings.get(organizationCustomFieldName) ?: new JiraCustomFieldMappings("Stub Org", null, null, null) }
  JiraCustomFieldMappings getScanStageCustomField() { return validatedGlobalCustomFieldMappings.get(scanStageCustomFieldName) ?: new JiraCustomFieldMappings("Stub Scan Stage", null, null, null) }
  JiraCustomFieldMappings getViolationIdCustomField() { return validatedViolationCustomFieldMappings.get(violationIdCustomFieldName) ?: new JiraCustomFieldMappings("Stub Violation ID", null, null, null) }
  JiraCustomFieldMappings getViolationDetectDateCustomField() { return validatedViolationCustomFieldMappings.get(violationDetectDateCustomFieldName) ?: new JiraCustomFieldMappings("Stub Detect Date", null, null, null) }
  JiraCustomFieldMappings getLastScanDateCustomField() { return validatedGlobalCustomFieldMappings.get(lastScanDateCustomFieldName) ?: new JiraCustomFieldMappings("Stub Last Scan Date", null, null, null) }
  JiraCustomFieldMappings getSeverityCustomField() { return validatedViolationCustomFieldMappings.get(severityCustomFieldName) ?: new JiraCustomFieldMappings("Stub Severity", null, null, null) }
  JiraCustomFieldMappings getMaxSeverityCustomField() { return validatedViolationCustomFieldMappings.get(maxSeverityCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max Severity", null, null, null) }
  JiraCustomFieldMappings getCveCodeCustomField() { return validatedViolationCustomFieldMappings.get(cveCodeCustomFieldName) ?: new JiraCustomFieldMappings("Stub CVE Code", null, null, null) }
  JiraCustomFieldMappings getMaxCveCodeCustomField() { return validatedViolationCustomFieldMappings.get(maxCveCodeCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max CVE Code", null, null, null) }
  JiraCustomFieldMappings getCveLinkCustomField() { return validatedViolationCustomFieldMappings.get(cveLinkCustomFieldName) ?: new JiraCustomFieldMappings("Stub CVE Link", null, null, null) }
  JiraCustomFieldMappings getMaxCveLinkCustomField() { return validatedViolationCustomFieldMappings.get(maxCveLinkCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max CVE Link", null, null, null) }
  JiraCustomFieldMappings getCweCodeCustomField() { return validatedViolationCustomFieldMappings.get(cweCodeCustomFieldName) ?: new JiraCustomFieldMappings("Stub CWE Code", null, null, null) }
  JiraCustomFieldMappings getMaxCweCodeCustomField() { return validatedViolationCustomFieldMappings.get(maxCweCodeCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max CWE Code", null, null, null) }
  JiraCustomFieldMappings getThreatVectorCustomField() { return validatedViolationCustomFieldMappings.get(threatVectorCustomFieldName) ?: new JiraCustomFieldMappings("Stub Threat Vector", null, null, null) }
  JiraCustomFieldMappings getMaxThreatVectorCustomField() { return validatedViolationCustomFieldMappings.get(maxThreatVectorCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max Threat Vector", null, null, null) }
  JiraCustomFieldMappings getCvssCustomField() { return validatedViolationCustomFieldMappings.get(cvssCustomFieldName) ?: new JiraCustomFieldMappings("Stub CVSS", null, null, null) }
  JiraCustomFieldMappings getMaxCvssCustomField() { return validatedViolationCustomFieldMappings.get(maxCvssCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max CVSS", null, null, null) }
  JiraCustomFieldMappings getIqServerReportLinkCustomField() { return validatedGlobalCustomFieldMappings.get(iqServerReportLinkCustomFieldName) ?: new JiraCustomFieldMappings("Stub Report Link", null, null, null) }
  JiraCustomFieldMappings getIqServerPolicyViolationNameCustomField() { return validatedViolationCustomFieldMappings.get(iqServerPolicyViolationNameCustomFieldName) ?: new JiraCustomFieldMappings("Stub Policy Violation Name", null, null, null) }
  JiraCustomFieldMappings getMaxIqServerPolicyViolationNameCustomField() { return validatedViolationCustomFieldMappings.get(maxIqServerPolicyViolationNameCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max Policy Violation Name", null, null, null) }
  JiraCustomFieldMappings getIqServerPolicyViolationThreatLevelCustomField() { return validatedViolationCustomFieldMappings.get(iqServerPolicyViolationThreatLevelCustomFieldName) ?: new JiraCustomFieldMappings("Stub Policy Violation Threat Level", null, null, null) }
  JiraCustomFieldMappings getMaxIqServerPolicyViolationThreatLevelCustomField() { return validatedViolationCustomFieldMappings.get(maxIqServerPolicyViolationThreatLevelCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max Policy Violation Threat Level", null, null, null) }
  JiraCustomFieldMappings getDeclaredLicensesCustomField() { return validatedViolationCustomFieldMappings.get(declaredLicensesCustomFieldName) ?: new JiraCustomFieldMappings("Stub Declared Licenses", null, null, null) }
  JiraCustomFieldMappings getObservedLicensesCustomField() { return validatedViolationCustomFieldMappings.get(observedLicensesCustomFieldName) ?: new JiraCustomFieldMappings("Stub Observed Licenses", null, null, null) }
  JiraCustomFieldMappings getEffectiveLicensesCustomField() { return validatedViolationCustomFieldMappings.get(effectiveLicensesCustomFieldName) ?: new JiraCustomFieldMappings("Stub Effective Licenses", null, null, null) }
  JiraCustomFieldMappings getFileOccurrencesCustomField() { return validatedViolationCustomFieldMappings.get(fileOccurrencesCustomFieldName) ?: new JiraCustomFieldMappings("Stub File Occurrences", null, null, null) }
  JiraCustomFieldMappings getRecommendedRemediationCustomField() { return validatedViolationCustomFieldMappings.get(recommendedRemediationCustomFieldName) ?: new JiraCustomFieldMappings("Stub Recommended Remediation", null, null, null) }
  JiraCustomFieldMappings getPurlCustomField() { return validatedViolationCustomFieldMappings.get(purlCustomFieldName) ?: new JiraCustomFieldMappings("Stub PURL", null, null, null) }
  JiraCustomFieldMappings getComponentCombinedIdentifierCustomField() { return validatedViolationCustomFieldMappings.get(componentCombinedIdentifierCustomFieldName) ?: new JiraCustomFieldMappings("Stub Component Combined Identifier", null, null, null) }
  JiraCustomFieldMappings getComponentGroup() { return validatedViolationCustomFieldMappings.get(componentGroupCustomFieldName) ?: new JiraCustomFieldMappings("Stub Component Group", null, null, null) }
  JiraCustomFieldMappings getComponentName() { return validatedViolationCustomFieldMappings.get(componentNameCustomFieldName) ?: new JiraCustomFieldMappings("Stub Component Name", null, null, null) }
  JiraCustomFieldMappings getComponentVersion() { return validatedViolationCustomFieldMappings.get(componentVersionCustomFieldName) ?: new JiraCustomFieldMappings("Stub Component Version", null, null, null) }
  JiraCustomFieldMappings getComponentClassifier() { return validatedViolationCustomFieldMappings.get(componentClassifierCustomFieldName) ?: new JiraCustomFieldMappings("Stub Component Classifier", null, null, null) }
  JiraCustomFieldMappings getComponentExtension() { return validatedViolationCustomFieldMappings.get(componentExtensionCustomFieldName) ?: new JiraCustomFieldMappings("Stub Component Extension", null, null, null) }
  JiraCustomFieldMappings getPassthroughCustomField(String pFieldName) { return validatedGlobalCustomFieldMappings.get(pFieldName) ?: new JiraCustomFieldMappings("Stub Passthrough ${pFieldName}", null, null, null) }

  JiraFieldMappingUtil(def pDynamicDataJsonOne, JiraNotification pJiraNotification, JiraClient pJiraClient, EnvVars pEnvVars, PrintStream pLogger)
  {
    iDynamicDataJsonOne = pDynamicDataJsonOne
    iJiraNotification = pJiraNotification
    jiraClient = pJiraClient
    iEnvVars = pEnvVars
    this.logger = pLogger

    scanDate = new Date()

    validatedGlobalCustomFieldMappings = [:]
    validatedViolationCustomFieldMappings = [:]

    expandEnvVars()
    assignFieldsFromConfig()
    mapCustomFieldNamesToIds()
  }

  /**
   * This is where it all comes together.
   *
   * pPolicyViolation is a simple DTO object containing the values for each policy violation. Unfortunately,
   * it doesn't know anything about Jira.
   *
   * This class knows about jira and all the custom field information. The problem is that some of those fields
   * are global and some of those fields are specific to each violation.
   *
   * This grew organically. At first it wasn't a problem, since all the field values were the same. However,
   * with the max fields being on the parent ticket and the other fields being on the sub-tasks, the global
   * state started to cause problems.
   *
   * I still want to centralize all the custom field logic so that the validation of the field mapping
   * and logging and adding to the jira ticket json can be managed together. That leaves me with this method
   * that has to gracefully manage the global state, as well as the getters and setters in the rest of this
   * class that need to declare the global vs. violation fields properly.
   *
   * @param returnValue
   * @param pPolicyViolation
   * @param pIsParentTicket
   */
  void addCustomFieldsToTicket(Map returnValue, PolicyViolation pPolicyViolation, boolean pIsParentTicket)
  {
    //TODO: Split this into two private functions: 1) ETL policy and component data to custom fields and 2) assign custom field to the ticket

    getViolationIdCustomField().customFieldValue =  pPolicyViolation.fingerprint
    getViolationDetectDateCustomField().customFieldValue = pPolicyViolation.detectDateString

    getDeclaredLicensesCustomField().customFieldValue = pPolicyViolation.licenseData?.declaredLicenseNames
    getObservedLicensesCustomField().customFieldValue = pPolicyViolation.licenseData?.observedLicenseNames
    getEffectiveLicensesCustomField().customFieldValue = pPolicyViolation.licenseData?.effectiveLicenseNames
    getFileOccurrencesCustomField().customFieldValue = pPolicyViolation.occurrences?.join("\n")
    getRecommendedRemediationCustomField().customFieldValue = pPolicyViolation.recommendedRemediation?.getRecommendationText(pPolicyViolation.componentIdentifier?.version)
    getPurlCustomField().customFieldValue = pPolicyViolation.packageUrl
    getComponentCombinedIdentifierCustomField().customFieldValue = pPolicyViolation.componentIdentifier?.prettyName
    getComponentGroup().customFieldValue = pPolicyViolation.componentIdentifier?.group
    getComponentName().customFieldValue = pPolicyViolation.componentIdentifier?.artifact
    getComponentVersion().customFieldValue = pPolicyViolation.componentIdentifier?.version
    getComponentClassifier().customFieldValue = pPolicyViolation.componentIdentifier?.classifier
    getComponentExtension().customFieldValue = pPolicyViolation.componentIdentifier?.extension

    if (shouldAggregateTicketsByComponent && pIsParentTicket)
    {
      //This is an aggregated component ticket, set the "max" fields
      getMaxSeverityCustomField().customFieldValue = pPolicyViolation.severity
      getMaxCveCodeCustomField().customFieldValue =  pPolicyViolation.cveCode
      getMaxCveLinkCustomField().customFieldValue =  pPolicyViolation.cveLink
      getMaxCweCodeCustomField().customFieldValue =  pPolicyViolation.cweCode
      getMaxThreatVectorCustomField().customFieldValue = pPolicyViolation.threatVector
      getMaxCvssCustomField().customFieldValue =  pPolicyViolation.cvssScore
      getMaxIqServerPolicyViolationNameCustomField().customFieldValue = pPolicyViolation.policyName
      getMaxIqServerPolicyViolationThreatLevelCustomField().customFieldValue = pPolicyViolation.policyThreatLevel
    }
    else
    {
      //This is a finding ticket, set the plain fields
      getSeverityCustomField().customFieldValue =  pPolicyViolation.severity
      getCveCodeCustomField().customFieldValue =  pPolicyViolation.cveCode
      getCveLinkCustomField().customFieldValue =  pPolicyViolation.cveLink
      getCweCodeCustomField().customFieldValue =  pPolicyViolation.cweCode
      getThreatVectorCustomField().customFieldValue = pPolicyViolation.threatVector
      getCvssCustomField().customFieldValue =  pPolicyViolation.cvssScore
      getIqServerPolicyViolationNameCustomField().customFieldValue = pPolicyViolation.policyName
      getIqServerPolicyViolationThreatLevelCustomField().customFieldValue = pPolicyViolation.policyThreatLevel
    }

    //add global fields to the jira request
    validatedGlobalCustomFieldMappings.each { fieldName, customFieldMapping ->

      copyValueIfNeeded(customFieldMapping, validatedGlobalCustomFieldMappings, validatedViolationCustomFieldMappings)
      addCustomFieldToTicket(returnValue, customFieldMapping)
    }

    //add violation fields to the jira request
    validatedViolationCustomFieldMappings.each {
      addCustomFieldToTicket(returnValue, it.value)
    }
  }

  private static void copyValueIfNeeded(JiraCustomFieldMappings pField, Map<String, JiraCustomFieldMappings> globalFields, Map<String, JiraCustomFieldMappings> violationFields)
  {
    if(pField.copyValueFromFieldName)
    {
      if(violationFields.containsKey(pField.copyValueFromFieldName))
      {
        pField.customFieldValue = violationFields.get(pField.copyValueFromFieldName).customFieldValue
      }
      else if(globalFields.containsKey(pField.copyValueFromFieldName))
      {
        pField.customFieldValue = globalFields.get(pField.copyValueFromFieldName).customFieldValue
      }
    }
  }

  private static void clearCopiedValueIfNeeded(JiraCustomFieldMappings pField)
  {
    //clear violation fields (NOT a global field)
    //also, clear copied values
    if(!pField.globalField || pField.copyValueFromFieldName)
    {
      pField.customFieldValue = null
    }
  }

  private void expandEnvVars()
  {
    projectKey = iEnvVars.expand(iJiraNotification.projectKey) //TODO: do I need to expand any other fields?
  }

  /**
   * This used to be spread out over a couple classes until i made this util class.
   * i'm storing the JiraNotification on this class, so it'd probably be easier to just use that
   * rather than copying all the values to this class
   */
  private void assignFieldsFromConfig()
  {
    issueTypeName = iJiraNotification.issueTypeName //TODO: this appears to be required on the API - the default value only comes in through the UI
    subTaskIssueTypeName = iJiraNotification.subTaskIssueTypeName
    priorityName = iJiraNotification.priorityName
    shouldCreateIndividualTickets = iJiraNotification.shouldCreateIndividualTickets
    shouldAggregateTicketsByComponent = iJiraNotification.shouldAggregateTicketsByComponent
    shouldCreateSubTasksForAggregatedTickets = iJiraNotification.shouldCreateSubTasksForAggregatedTickets
    shouldTransitionJiraTickets = iJiraNotification.shouldTransitionJiraTickets
    transitionStatus = iJiraNotification.jiraTransitionStatus
    transitionName = iJiraNotification.jiraTransitionName
    policyFilterPrefix = iJiraNotification.policyFilterPrefix
    policyFilterThreatLevel = iJiraNotification.policyFilterThreatLevel

    //Advanced Options
//only passed to JiraClient               this.jobJiraCredentialsId = jobJiraCredentialsId;
//only passed to IQClient                 this.jobIQCredentialsId = jobIQCredentialsId;
//only passed to IQClient & JiraClient    this.verboseLogging = verboseLogging;
//only passed to JiraClient               this.dryRun = dryRun;
    jiraDateFormatOverride = iJiraNotification.jiraDateFormatOverride
////only passed to JiraClient               this.disableJqlFieldFilter = disableJqlFieldFilter;
////only passed to JiraClient               this.jqlMaxResultsOverride = jqlMaxResultsOverride;
//todo here?    this.jiraCustomFieldTypeOverrideMapping = jiraCustomFieldTypeOverrideMapping;

    //Custom Fields
    applicationCustomFieldName = iJiraNotification.applicationCustomFieldName
    organizationCustomFieldName = iJiraNotification.organizationCustomFieldName
    scanStageCustomFieldName = iJiraNotification.scanStageCustomFieldName
    violationIdCustomFieldName = iJiraNotification.violationIdCustomFieldName
    violationDetectDateCustomFieldName = iJiraNotification.violationDetectDateCustomFieldName
    lastScanDateCustomFieldName = iJiraNotification.lastScanDateCustomFieldName
    severityCustomFieldName = iJiraNotification.severityCustomFieldName
    maxSeverityCustomFieldName = iJiraNotification.maxSeverityCustomFieldName
    cveCodeCustomFieldName = iJiraNotification.cveCodeCustomFieldName
    maxCveCodeCustomFieldName = iJiraNotification.maxCveCodeCustomFieldName
    cveLinkCustomFieldName = iJiraNotification.cveLinkCustomFieldName
    maxCveLinkCustomFieldName = iJiraNotification.maxCveLinkCustomFieldName
    cweCodeCustomFieldName = iJiraNotification.cweCodeCustomFieldName
    maxCweCodeCustomFieldName = iJiraNotification.maxCweCodeCustomFieldName
    threatVectorCustomFieldName = iJiraNotification.threatVectorCustomFieldName
    maxThreatVectorCustomFieldName = iJiraNotification.maxThreatVectorCustomFieldName
    cvssCustomFieldName = iJiraNotification.cvssCustomFieldName
    maxCvssCustomFieldName = iJiraNotification.maxCvssCustomFieldName
    iqServerReportLinkCustomFieldName = iJiraNotification.iqServerReportLinkCustomFieldName
    iqServerPolicyViolationNameCustomFieldName = iJiraNotification.iqServerPolicyViolationNameCustomFieldName
    maxIqServerPolicyViolationNameCustomFieldName = iJiraNotification.maxIqServerPolicyViolationNameCustomFieldName
    iqServerPolicyViolationThreatLevelCustomFieldName = iJiraNotification.iqServerPolicyViolationThreatLevelCustomFieldName
    maxIqServerPolicyViolationThreatLevelCustomFieldName = iJiraNotification.maxIqServerPolicyViolationThreatLevelCustomFieldName
    declaredLicensesCustomFieldName = iJiraNotification.declaredLicensesCustomFieldName
    observedLicensesCustomFieldName = iJiraNotification.observedLicensesCustomFieldName
    effectiveLicensesCustomFieldName = iJiraNotification.effectiveLicensesCustomFieldName
    fileOccurrencesCustomFieldName = iJiraNotification.fileOccurrencesCustomFieldName
    recommendedRemediationCustomFieldName = iJiraNotification.recommendedRemediationCustomFieldName
    purlCustomFieldName = iJiraNotification.purlCustomFieldName
    componentCombinedIdentifierCustomFieldName = iJiraNotification.componentCombinedIdentifierCustomFieldName
    componentGroupCustomFieldName = iJiraNotification.componentGroupCustomFieldName
    componentNameCustomFieldName = iJiraNotification.componentNameCustomFieldName
    componentVersionCustomFieldName = iJiraNotification.componentVersionCustomFieldName
    componentClassifierCustomFieldName = iJiraNotification.componentClassifierCustomFieldName
    componentExtensionCustomFieldName = iJiraNotification.componentExtensionCustomFieldName

    jiraCustomFieldMappings = iJiraNotification.jiraCustomFieldMappings ?: []
  }

  private void mapCustomFieldNamesToIds()
  {
    // todo: use the response for some validation and automatic formatting of the custom fields
    // TODO: can i make the same call here for updating tickets? I need a ticket Key first, though
    jiraClient.lookupMetadataConfigurationForCreateIssue(projectKey, issueTypeName)
    if (shouldCreateSubTasksForAggregatedTickets)
    {
      jiraClient.lookupMetadataConfigurationForCreateIssue(projectKey, subTaskIssueTypeName)
    }

    List customFields = (List) jiraClient.lookupCustomFields()

    lookupAndValidateCustomField(customFields, true, applicationCustomFieldName, "App Name")
    lookupAndValidateCustomField(customFields, true, organizationCustomFieldName, "Org Name")
    lookupAndValidateCustomField(customFields, true, scanStageCustomFieldName, "Scan Stage")
    lookupAndValidateCustomField(customFields, false, violationIdCustomFieldName, "Violation ID")
    lookupAndValidateCustomField(customFields, false, violationDetectDateCustomFieldName, "Detect Date")
    lookupAndValidateCustomField(customFields, true, lastScanDateCustomFieldName, "Last Scan Date")
    lookupAndValidateCustomField(customFields, false, severityCustomFieldName, "Severity")
    lookupAndValidateCustomField(customFields, false, maxSeverityCustomFieldName, "Max Severity")
    lookupAndValidateCustomField(customFields, false, cveCodeCustomFieldName, "CVE Code")
    lookupAndValidateCustomField(customFields, false, maxCveCodeCustomFieldName, "Max CVE Code")
    lookupAndValidateCustomField(customFields, false, cveLinkCustomFieldName, "CVE Link")
    lookupAndValidateCustomField(customFields, false, maxCveLinkCustomFieldName, "Max CVE Link")
    lookupAndValidateCustomField(customFields, false, cweCodeCustomFieldName, "CWE Code")
    lookupAndValidateCustomField(customFields, false, maxCweCodeCustomFieldName, "Max CWE Code")
    lookupAndValidateCustomField(customFields, false, threatVectorCustomFieldName, "Threat Vector")
    lookupAndValidateCustomField(customFields, false, maxThreatVectorCustomFieldName, "Max Threat Vector")
    lookupAndValidateCustomField(customFields, false, cvssCustomFieldName, "CVSS")
    lookupAndValidateCustomField(customFields, false, maxCvssCustomFieldName, "Max CVSS")
    lookupAndValidateCustomField(customFields, true, iqServerReportLinkCustomFieldName, "Report Link")
    lookupAndValidateCustomField(customFields, false, iqServerPolicyViolationNameCustomFieldName, "Policy Violation Name")
    lookupAndValidateCustomField(customFields, false, maxIqServerPolicyViolationNameCustomFieldName, "Max Policy Violation Name")
    lookupAndValidateCustomField(customFields, false, iqServerPolicyViolationThreatLevelCustomFieldName, "Policy Violation Threat Level")
    lookupAndValidateCustomField(customFields, false, maxIqServerPolicyViolationThreatLevelCustomFieldName,"Max Policy Violation Threat Level")
    lookupAndValidateCustomField(customFields, false, declaredLicensesCustomFieldName, "Declared Licenses")
    lookupAndValidateCustomField(customFields, false, observedLicensesCustomFieldName, "Observed Licenses")
    lookupAndValidateCustomField(customFields, false, effectiveLicensesCustomFieldName, "Effective Licenses")
    lookupAndValidateCustomField(customFields, false, fileOccurrencesCustomFieldName, "File Occurrences")
    lookupAndValidateCustomField(customFields, false, recommendedRemediationCustomFieldName, "Recommended Remediation")
    lookupAndValidateCustomField(customFields, false, purlCustomFieldName, "Package URL (PURL)")
    lookupAndValidateCustomField(customFields, false, componentCombinedIdentifierCustomFieldName, "Component Combined Identifier")
    lookupAndValidateCustomField(customFields, false, componentGroupCustomFieldName, "Component Group")
    lookupAndValidateCustomField(customFields, false, componentNameCustomFieldName, "Component Name")
    lookupAndValidateCustomField(customFields, false, componentVersionCustomFieldName, "Component Version")
    lookupAndValidateCustomField(customFields, false, componentClassifierCustomFieldName, "Component Classifier")
    lookupAndValidateCustomField(customFields, false, componentExtensionCustomFieldName, "Component Extension")

    jiraCustomFieldMappings.each {
      lookupAndValidateCustomField(customFields, true, it.customFieldName, "Passthrough Custom Field: ${it.customFieldName}")

      if(it.customFieldValue)
      {
        getPassthroughCustomField(it.customFieldName).customFieldValue = it.customFieldValue
      }
      else if (it.dynamicDataCustomFieldValue && iDynamicDataJsonOne)
      {
        getPassthroughCustomField(it.customFieldName).customFieldValue = iDynamicDataJsonOne[it.dynamicDataCustomFieldValue][0]
      }
      else if (it.copyValueFromFieldName)
      {
        logger.println("Copying Custom Field value from ${it.copyValueFromFieldName} to ${it.customFieldName}")
        getPassthroughCustomField(it.customFieldName).copyValueFromFieldName = it.copyValueFromFieldName //i'll copy the actual value later on
      }
    }
  }

  private void lookupAndValidateCustomField(List<Map<String, Object>> pCustomFields, boolean pIsGlobalField, String pFieldName, String pFieldDescription)
  {
    if (pFieldName)
    {
      JiraCustomFieldMappings returnValue = lookupCustomFieldId(pCustomFields, pFieldName)
      if (returnValue)
      {
        //TODO: show Required, but that's going back to the Issue Type Specific REST API
        logger.println("Custom Field mapping for field description: ${pFieldDescription} created mapping ${pFieldName} -> ${returnValue.customFieldId} (${returnValue.customFieldType})")

        //save this so we can clear the violation values when we iterate the loops
        returnValue.globalField = pIsGlobalField

        if (pIsGlobalField)
        {
          validatedGlobalCustomFieldMappings.put(pFieldName, returnValue)
        }
        else
        {
          validatedViolationCustomFieldMappings.put(pFieldName, returnValue)
        }
      }
      else
      {
        throw new AbortException("Custom Field mapping for field description: ${pFieldDescription}, not found with field name: ${pFieldName}")
      }
    }
    else
    {
      logger.println("Custom Field mapping not provided for field description: ${pFieldDescription}")
    }
  }

  private static JiraCustomFieldMappings lookupCustomFieldId(List<Map<String, Object>> customFields, String fieldName)
  {
    JiraCustomFieldMappings returnValue = null
    customFields.each {
      if(it.name == fieldName)
      {
        returnValue = new JiraCustomFieldMappings(fieldName, null, null, null)
        returnValue.customFieldId = it.id
        returnValue.customFieldType = it.schema?.type //TODO: create an override mapping
      }
    }

    returnValue
  }

  String getScanInternalId()
  {
    return scanInternalId
  }

  void setScanInternalId(String scanInternalId)
  {
    this.scanInternalId = scanInternalId
  }

  String getFormattedScanDateForJira()
  {
    if (jiraDateFormatOverride)
    {
      return scanDate.format(jiraDateFormatOverride)
    }
    else
    {
      return scanDate.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }
  }

  String formatDateForJira(Date pDate)
  {
    if (jiraDateFormatOverride)
    {
      return pDate.format(jiraDateFormatOverride)
    }
    else
    {
      return pDate.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }
  }

  /**
   * Jira wants this:
   *      "customfield_10003": "2011-10-19T10:29:29.908+1100"
   * @param pDate
   * @return
   */
  protected String formatDateForJira(Date pDate, TimeZone pTz)
  {
    if (jiraDateFormatOverride)
    {
      return pDate.format(jiraDateFormatOverride, pTz)
    }
    else
    {
      return pDate.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ", pTz)
    }
  }

  protected Date parseDateForJira(String pDateString)
  {
    if (jiraDateFormatOverride)
    {
      return Date.parse(jiraDateFormatOverride, pDateString)
    }
    else
    {
      return Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", pDateString)
    }
  }

  //formatting for custom fields: https://developer.atlassian.com/server/jira/platform/jira-rest-api-examples/#creating-an-issue-using-custom-fields
  private static addCustomFieldToTicket(Map ticketFieldsArray, JiraCustomFieldMappings pField)
  {
    if (pField.customFieldId && pField.customFieldValue)
    {
      def returnValue
      switch (pField.customFieldType)
      {
        case "date": //todo: short date - right now, it's up to the user to format the date correctly (i.e. when calling new Date().format())
        case "datetime":
        case "string":
          returnValue = pField.customFieldValue
          break
        case "option":
          returnValue = [ value: pField.customFieldValue ]
          break
        case "number":
            returnValue = Double.valueOf(pField.customFieldValue)
          break
        default:
          returnValue = null
          break
      }

      if (returnValue)
      {
        ticketFieldsArray.fields.put(pField.customFieldId, returnValue)
      }

      // clear the value since the map is reused for each ticket
      clearCopiedValueIfNeeded(pField)
    }
  }
}
