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
  private String cweCodeCustomFieldName
  private String maxCweCodeCustomFieldName
  private String threatVectorCustomFieldName
  private String maxThreatVectorCustomFieldName
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

  private static String application_CUSTOM_FIELD_DESCRIPTION = "App Name"
  private static String organization_CUSTOM_FIELD_DESCRIPTION = "Org Name"
  private static String scanStage_CUSTOM_FIELD_DESCRIPTION = "Scan Stage"
  private static String violationId_CUSTOM_FIELD_DESCRIPTION = "Violation ID"
  private static String violationDetectDate_CUSTOM_FIELD_DESCRIPTION = "Detect Date"
  private static String lastScanDate_CUSTOM_FIELD_DESCRIPTION = "Last Scan Date"
  private static String severity_CUSTOM_FIELD_DESCRIPTION = "Severity"
  private static String maxSeverity_CUSTOM_FIELD_DESCRIPTION = "Max Severity"
  private static String cveCode_CUSTOM_FIELD_DESCRIPTION = "CVE Code"
  private static String maxCveCode_CUSTOM_FIELD_DESCRIPTION = "Max CVE Code"
  private static String cveLink_CUSTOM_FIELD_DESCRIPTION = "CVE Link"
  private static String maxCveLink_CUSTOM_FIELD_DESCRIPTION = "Max CVE Link"
  private static String cweCode_CUSTOM_FIELD_DESCRIPTION = "CWE Code"
  private static String maxCweCode_CUSTOM_FIELD_DESCRIPTION = "Max CWE Code"
  private static String threatVector_CUSTOM_FIELD_DESCRIPTION = "Threat Vector"
  private static String maxThreatVector_CUSTOM_FIELD_DESCRIPTION = "Max Threat Vector"
  private static String cvss_CUSTOM_FIELD_DESCRIPTION = "CVSS"
  private static String maxCvss_CUSTOM_FIELD_DESCRIPTION = "Max CVSS"
  private static String iqServerReportLink_CUSTOM_FIELD_DESCRIPTION = "Report Link"
  private static String iqServerPolicyViolationName_CUSTOM_FIELD_DESCRIPTION = "Policy Violation Name"
  private static String maxIqServerPolicyViolationName_CUSTOM_FIELD_DESCRIPTION = "Max Policy Violation Name"
  private static String iqServerPolicyViolationThreatLevel_CUSTOM_FIELD_DESCRIPTION = "Policy Violation Threat Level"
  private static String maxIqServerPolicyViolationThreatLevel_CUSTOM_FIELD_DESCRIPTION = "Max Policy Violation Threat Level"
  private static String declaredLicenses_CUSTOM_FIELD_DESCRIPTION = "Declared Licenses"
  private static String observedLicenses_CUSTOM_FIELD_DESCRIPTION = "Observed Licenses"
  private static String effectiveLicenses_CUSTOM_FIELD_DESCRIPTION = "Effective Licenses"
  private static String fileOccurrences_CUSTOM_FIELD_DESCRIPTION = "File Occurrences"
  private static String recommendedRemediation_CUSTOM_FIELD_DESCRIPTION = "Recommended Remediation"
  private static String purl_CUSTOM_FIELD_DESCRIPTION = "Package URL (PURL)"
  private static String componentCombinedIdentifier_CUSTOM_FIELD_DESCRIPTION = "Component Combined Identifier"
  private static String componentGroup_CUSTOM_FIELD_DESCRIPTION = "Component Group"
  private static String componentName_CUSTOM_FIELD_DESCRIPTION = "Component Name"
  private static String componentVersion_CUSTOM_FIELD_DESCRIPTION = "Component Version"
  private static String componentClassifier_CUSTOM_FIELD_DESCRIPTION = "Component Classifier"
  private static String componentExtension_CUSTOM_FIELD_DESCRIPTION = "Component Extension"
  private static String PASSTHROUGH_CUSTOM_FIELD_DESCRIPTION_PREFIX = "Passthrough Custom Field: "


  JiraCustomFieldMappings getApplicationCustomField() { return lookupJCFM(true, application_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getOrganizationCustomField() { return lookupJCFM(true, organization_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getScanStageCustomField() { return lookupJCFM(true, scanStage_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getViolationIdCustomField() { return lookupJCFM(false, violationId_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getViolationDetectDateCustomField() { return lookupJCFM(false, violationDetectDate_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getLastScanDateCustomField() { return lookupJCFM(true, lastScanDate_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getSeverityCustomField() { return lookupJCFM(false, severity_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getMaxSeverityCustomField() { return lookupJCFM(false, maxSeverity_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getCveCodeCustomField() { return lookupJCFM(false, cveCode_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getMaxCveCodeCustomField() { return lookupJCFM(false, maxCveCode_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getCveLinkCustomField() { return lookupJCFM(false, cveLink_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getMaxCveLinkCustomField() { return lookupJCFM(false, maxCveLink_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getCweCodeCustomField() { return lookupJCFM(false, cweCode_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getMaxCweCodeCustomField() { return lookupJCFM(false, maxCweCode_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getThreatVectorCustomField() { return lookupJCFM(false, threatVector_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getMaxThreatVectorCustomField() { return lookupJCFM(false, maxThreatVector_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getCvssCustomField() { return lookupJCFM(false, cvss_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getMaxCvssCustomField() { return lookupJCFM(false, maxCvss_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getIqServerReportLinkCustomField() { return lookupJCFM(true, iqServerReportLink_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getIqServerPolicyViolationNameCustomField() { return lookupJCFM(false, iqServerPolicyViolationName_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getMaxIqServerPolicyViolationNameCustomField() { return lookupJCFM(false, maxIqServerPolicyViolationName_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getIqServerPolicyViolationThreatLevelCustomField() { return lookupJCFM(false, iqServerPolicyViolationThreatLevel_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getMaxIqServerPolicyViolationThreatLevelCustomField() { return lookupJCFM(false, maxIqServerPolicyViolationThreatLevel_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getDeclaredLicensesCustomField() { return lookupJCFM(false, declaredLicenses_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getObservedLicensesCustomField() { return lookupJCFM(false, observedLicenses_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getEffectiveLicensesCustomField() { return lookupJCFM(false, effectiveLicenses_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getFileOccurrencesCustomField() { return lookupJCFM(false, fileOccurrences_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getRecommendedRemediationCustomField() { return lookupJCFM(false, recommendedRemediation_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getPurlCustomField() { return lookupJCFM(false, purl_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getComponentCombinedIdentifierCustomField() { return lookupJCFM(false, componentCombinedIdentifier_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getComponentGroup() { return lookupJCFM(false, componentGroup_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getComponentName() { return lookupJCFM(false, componentName_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getComponentVersion() { return lookupJCFM(false, componentVersion_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getComponentClassifier() { return lookupJCFM(false, componentClassifier_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getComponentExtension() { return lookupJCFM(false, componentExtension_CUSTOM_FIELD_DESCRIPTION) }
  JiraCustomFieldMappings getPassthroughCustomField(String pFieldName) { return lookupJCFM(true, "${PASSTHROUGH_CUSTOM_FIELD_DESCRIPTION_PREFIX}${pFieldName}") }

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
      JiraCustomFieldMappings sourceViolationField = violationFields.find { pField.copyValueFromFieldName == it.value.customFieldName}?.value
      JiraCustomFieldMappings sourceGlobalField = globalFields.find { pField.copyValueFromFieldName == it.value.customFieldName}?.value

      if(sourceViolationField)
      {
        pField.customFieldValue = sourceViolationField.customFieldValue
      }
      else if(sourceGlobalField)
      {
        pField.customFieldValue = sourceGlobalField.customFieldValue
      }
    }
  }

  private static void clearValueIfNeeded(JiraCustomFieldMappings pField)
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
    issueTypeName = iJiraNotification.issueTypeName
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

    lookupAndValidateCustomField(customFields, true, applicationCustomFieldName, application_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, true, organizationCustomFieldName, organization_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, true, scanStageCustomFieldName, scanStage_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, violationIdCustomFieldName, violationId_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, violationDetectDateCustomFieldName, violationDetectDate_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, true, lastScanDateCustomFieldName, lastScanDate_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, severityCustomFieldName, severity_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, maxSeverityCustomFieldName, maxSeverity_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, cveCodeCustomFieldName, cveCode_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, maxCveCodeCustomFieldName, maxCveCode_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, cveLinkCustomFieldName, cveLink_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, maxCveLinkCustomFieldName, maxCveLink_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, cweCodeCustomFieldName, cweCode_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, maxCweCodeCustomFieldName, maxCweCode_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, threatVectorCustomFieldName, threatVector_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, maxThreatVectorCustomFieldName, maxThreatVector_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, cvssCustomFieldName, cvss_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, maxCvssCustomFieldName, maxCvss_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, true, iqServerReportLinkCustomFieldName, iqServerReportLink_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, iqServerPolicyViolationNameCustomFieldName, iqServerPolicyViolationName_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, maxIqServerPolicyViolationNameCustomFieldName, maxIqServerPolicyViolationName_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, iqServerPolicyViolationThreatLevelCustomFieldName, iqServerPolicyViolationThreatLevel_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, maxIqServerPolicyViolationThreatLevelCustomFieldName,maxIqServerPolicyViolationThreatLevel_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, declaredLicensesCustomFieldName, declaredLicenses_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, observedLicensesCustomFieldName, observedLicenses_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, effectiveLicensesCustomFieldName, effectiveLicenses_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, fileOccurrencesCustomFieldName, fileOccurrences_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, recommendedRemediationCustomFieldName, recommendedRemediation_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, purlCustomFieldName, purl_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, componentCombinedIdentifierCustomFieldName, componentCombinedIdentifier_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, componentGroupCustomFieldName, componentGroup_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, componentNameCustomFieldName, componentName_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, componentVersionCustomFieldName, componentVersion_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, componentClassifierCustomFieldName, componentClassifier_CUSTOM_FIELD_DESCRIPTION)
    lookupAndValidateCustomField(customFields, false, componentExtensionCustomFieldName, componentExtension_CUSTOM_FIELD_DESCRIPTION)

    jiraCustomFieldMappings.each {
      lookupAndValidateCustomField(customFields, true, it.customFieldName, "${PASSTHROUGH_CUSTOM_FIELD_DESCRIPTION_PREFIX}${it.customFieldName}")

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
    JiraCustomFieldMappings returnValue

    if (pFieldName)
    {
      returnValue = lookupCustomFieldId(pCustomFields, pFieldName)
      if (returnValue)
      {
        //TODO: show Required, but that's going back to the Issue Type Specific REST API
        logger.println("Custom Field mapping for field description: ${pFieldDescription} created mapping ${pFieldName} -> ${returnValue.customFieldId} (${returnValue.customFieldType})")
      }
      else
      {
        throw new AbortException("Custom Field mapping for field description: ${pFieldDescription}, not found with field name: ${pFieldName}")
      }
    }
    else
    {
      logger.println("Custom Field mapping not provided for field description: ${pFieldDescription}")

      //Create a stub
      returnValue = new JiraCustomFieldMappings("Stub ${pFieldDescription}", null, null, null)
    }

    //save this so we can clear the violation values when we iterate the loops
    returnValue.globalField = pIsGlobalField

    if (pIsGlobalField)
    {
      validatedGlobalCustomFieldMappings.put(pFieldDescription, returnValue)
    }
    else
    {
      validatedViolationCustomFieldMappings.put(pFieldDescription, returnValue)
    }
  }

  private JiraCustomFieldMappings lookupJCFM(boolean pIsGlobalField, String pFieldDescription)
  {
    if (pIsGlobalField)
    {
      return validatedGlobalCustomFieldMappings.get(pFieldDescription)
    }
    else
    {
      return validatedViolationCustomFieldMappings.get(pFieldDescription)
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
    }

    // clear the value since the map is reused for each ticket
    clearValueIfNeeded(pField)
  }
}
