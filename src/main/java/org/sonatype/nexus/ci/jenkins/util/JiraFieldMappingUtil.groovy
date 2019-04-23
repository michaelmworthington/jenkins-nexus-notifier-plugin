package org.sonatype.nexus.ci.jenkins.util

import hudson.AbortException
import hudson.EnvVars
import org.sonatype.nexus.ci.jenkins.jira.JiraClient
import org.sonatype.nexus.ci.jenkins.model.PolicyViolation
import org.sonatype.nexus.ci.jenkins.notifier.JiraCustomFieldMappings
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification

class JiraFieldMappingUtil
{
  private JiraNotification iJiraNotification
  private JiraClient jiraClient
  private EnvVars iEnvVars
  private PrintStream logger
  private Date scanDate

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
  private String violationIdCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String violationDetectDateCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String lastScanDateCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String severityCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String maxSeverityCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String cveCodeCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String maxCveCodeCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String cvssCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String maxCvssCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String iqServerReportLinkCustomFieldName;
  private String iqServerPolicyViolationNameCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String maxIqServerPolicyViolationNameCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String iqServerPolicyViolationThreatLevelCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String maxIqServerPolicyViolationThreatLevelCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String componentGroupCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String componentNameCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String componentVersionCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String componentClassifierCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?
  private String componentExtensionCustomFieldName //TODO: can I prevent Violation Level values from being set on this object?

  String policyFilterPrefix
  String jiraDateFormatOverride
  Boolean shouldAggregateTicketsByComponent
  Boolean shouldCreateSubTasksForAggregatedTickets

  List<JiraCustomFieldMappings> jiraCustomFieldMappings

  //Custom Field IDs
  //  Populated by calling the JIRA REST API to get all of the custom fields,
  //  then looking up by name to get the ID
  private Map<String, JiraCustomFieldMappings> validatedCustomFieldMappings

  JiraCustomFieldMappings getApplicationCustomField() { return validatedCustomFieldMappings.get(applicationCustomFieldName)  ?: new JiraCustomFieldMappings("Stub App", null) }
  JiraCustomFieldMappings getOrganizationCustomField() { return validatedCustomFieldMappings.get(organizationCustomFieldName) ?: new JiraCustomFieldMappings("Stub Org", null) }
  JiraCustomFieldMappings getScanStageCustomField() { return validatedCustomFieldMappings.get(scanStageCustomFieldName) ?: new JiraCustomFieldMappings("Stub Scan Stage", null) }
  JiraCustomFieldMappings getViolationIdCustomField() { return validatedCustomFieldMappings.get(violationIdCustomFieldName) ?: new JiraCustomFieldMappings("Stub Violation ID", null) }
  JiraCustomFieldMappings getViolationDetectDateCustomField() { return validatedCustomFieldMappings.get(violationDetectDateCustomFieldName) ?: new JiraCustomFieldMappings("Stub Detect Date", null) }
  JiraCustomFieldMappings getLastScanDateCustomField() { return validatedCustomFieldMappings.get(lastScanDateCustomFieldName) ?: new JiraCustomFieldMappings("Stub Last Scan Date", null) }
  JiraCustomFieldMappings getSeverityCustomField() { return validatedCustomFieldMappings.get(severityCustomFieldName) ?: new JiraCustomFieldMappings("Stub Severity", null) }
  JiraCustomFieldMappings getMaxSeverityCustomField() { return validatedCustomFieldMappings.get(maxSeverityCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max Severity", null) }
  JiraCustomFieldMappings getCveCodeCustomField() { return validatedCustomFieldMappings.get(cveCodeCustomFieldName) ?: new JiraCustomFieldMappings("Stub CVE", null) }
  JiraCustomFieldMappings getMaxCveCodeCustomField() { return validatedCustomFieldMappings.get(maxCveCodeCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max CVE", null) }
  JiraCustomFieldMappings getCvssCustomField() { return validatedCustomFieldMappings.get(cvssCustomFieldName) ?: new JiraCustomFieldMappings("Stub CVSS", null) }
  JiraCustomFieldMappings getMaxCvssCustomField() { return validatedCustomFieldMappings.get(maxCvssCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max CVSS", null) }
  JiraCustomFieldMappings getIqServerReportLinkCustomField() { return validatedCustomFieldMappings.get(iqServerReportLinkCustomFieldName) ?: new JiraCustomFieldMappings("Stub Report Link", null) }
  JiraCustomFieldMappings getIqServerPolicyViolationNameCustomField() { return validatedCustomFieldMappings.get(iqServerPolicyViolationNameCustomFieldName) ?: new JiraCustomFieldMappings("Stub Policy Violation Name", null) }
  JiraCustomFieldMappings getMaxIqServerPolicyViolationNameCustomField() { return validatedCustomFieldMappings.get(maxIqServerPolicyViolationNameCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max Policy Violation Name", null) }
  JiraCustomFieldMappings getIqServerPolicyViolationThreatLevelCustomField() { return validatedCustomFieldMappings.get(iqServerPolicyViolationThreatLevelCustomFieldName) ?: new JiraCustomFieldMappings("Stub Policy Violation Threat Level", null) }
  JiraCustomFieldMappings getMaxIqServerPolicyViolationThreatLevelCustomField() { return validatedCustomFieldMappings.get(maxIqServerPolicyViolationThreatLevelCustomFieldName) ?: new JiraCustomFieldMappings("Stub Max Policy Violation Threat Level", null) }
  JiraCustomFieldMappings getComponentGroup() { return validatedCustomFieldMappings.get(componentGroupCustomFieldName) ?: new JiraCustomFieldMappings("Stub Component Group", null) }
  JiraCustomFieldMappings getComponentName() { return validatedCustomFieldMappings.get(componentNameCustomFieldName) ?: new JiraCustomFieldMappings("Stub Component Name", null) }
  JiraCustomFieldMappings getComponentVersion() { return validatedCustomFieldMappings.get(componentVersionCustomFieldName) ?: new JiraCustomFieldMappings("Stub Component Version", null) }
  JiraCustomFieldMappings getComponentClassifier() { return validatedCustomFieldMappings.get(componentClassifierCustomFieldName) ?: new JiraCustomFieldMappings("Stub Component Classifier", null) }
  JiraCustomFieldMappings getComponentExtension() { return validatedCustomFieldMappings.get(componentExtensionCustomFieldName) ?: new JiraCustomFieldMappings("Stub Component Extension", null) }
  JiraCustomFieldMappings getPassthroughCustomField(String pFieldName) { return validatedCustomFieldMappings.get(pFieldName) ?: new JiraCustomFieldMappings("Stub Passthrough ${pFieldName}", null) }

  JiraFieldMappingUtil(JiraNotification pJiraNotification, JiraClient pJiraClient, EnvVars pEnvVars, PrintStream pLogger)
  {
    iJiraNotification = pJiraNotification
    jiraClient = pJiraClient
    iEnvVars = pEnvVars
    this.logger = pLogger

    scanDate = new Date()

    validatedCustomFieldMappings = [:]

    expandEnvVars()
    assignFieldsFromConfig()
    mapCustomFieldNamesToIds()
  }

  void addCustomFieldsToTicket(Map returnValue, String nowFormatted, PolicyViolation pPolicyViolation, boolean pIsParentTicket)
  {
    //TODO: are these available anywhere else (i.e. where they're defined) so i don't have to pass them all over the place
    //TODO: on second thought, JFMU is global. I don't think it's a good idea to set ticket level data on it
    //TODO:                                     ^^^^^^ nope, it's not. the max fields got screwed up
    getViolationIdCustomField().customFieldValue =  pPolicyViolation.fingerprint
    getViolationDetectDateCustomField().customFieldValue = nowFormatted
    getLastScanDateCustomField().customFieldValue =  nowFormatted
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
      getMaxCvssCustomField().customFieldValue =  pPolicyViolation.cvssScore
      getMaxIqServerPolicyViolationNameCustomField().customFieldValue = pPolicyViolation.policyName
      getMaxIqServerPolicyViolationThreatLevelCustomField().customFieldValue = pPolicyViolation.policyThreatLevel
    }
    else
    {
      //This is a finding ticket, set the plain fields
      getSeverityCustomField().customFieldValue =  pPolicyViolation.severity
      getCveCodeCustomField().customFieldValue =  pPolicyViolation.cveCode
      getCvssCustomField().customFieldValue =  pPolicyViolation.cvssScore
      getIqServerPolicyViolationNameCustomField().customFieldValue = pPolicyViolation.policyName
      getIqServerPolicyViolationThreatLevelCustomField().customFieldValue = pPolicyViolation.policyThreatLevel
    }

    validatedCustomFieldMappings.each {
      addCustomFieldToTicket(returnValue, it.value)
    }
  }

  private void expandEnvVars()
  {
    projectKey = iEnvVars.expand(iJiraNotification.projectKey) //TODO: do I need to expand any other fields?
  }

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
    cvssCustomFieldName = iJiraNotification.cvssCustomFieldName
    maxCvssCustomFieldName = iJiraNotification.maxCvssCustomFieldName
    iqServerReportLinkCustomFieldName = iJiraNotification.iqServerReportLinkCustomFieldName
    iqServerPolicyViolationNameCustomFieldName = iJiraNotification.iqServerPolicyViolationNameCustomFieldName
    maxIqServerPolicyViolationNameCustomFieldName = iJiraNotification.maxIqServerPolicyViolationNameCustomFieldName
    iqServerPolicyViolationThreatLevelCustomFieldName = iJiraNotification.iqServerPolicyViolationThreatLevelCustomFieldName
    maxIqServerPolicyViolationThreatLevelCustomFieldName = iJiraNotification.maxIqServerPolicyViolationThreatLevelCustomFieldName
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

    lookupAndValidateCustomField(customFields, applicationCustomFieldName, "App Name")
    lookupAndValidateCustomField(customFields, organizationCustomFieldName, "Org Name")
    lookupAndValidateCustomField(customFields, scanStageCustomFieldName, "Scan Stage")
    lookupAndValidateCustomField(customFields, violationIdCustomFieldName, "Violation ID")
    lookupAndValidateCustomField(customFields, violationDetectDateCustomFieldName,"Detect Date")
    lookupAndValidateCustomField(customFields, lastScanDateCustomFieldName,"Last Scan Date")
    lookupAndValidateCustomField(customFields, severityCustomFieldName, "Severity")
    lookupAndValidateCustomField(customFields, maxSeverityCustomFieldName, "Max Severity")
    lookupAndValidateCustomField(customFields, cveCodeCustomFieldName, "CVE Code")
    lookupAndValidateCustomField(customFields, maxCveCodeCustomFieldName, "Max CVE Code")
    lookupAndValidateCustomField(customFields, cvssCustomFieldName, "CVSS")
    lookupAndValidateCustomField(customFields, maxCvssCustomFieldName, "Max CVSS")
    lookupAndValidateCustomField(customFields, iqServerReportLinkCustomFieldName, "Report Link")
    lookupAndValidateCustomField(customFields, iqServerPolicyViolationNameCustomFieldName, "Policy Violation Name")
    lookupAndValidateCustomField(customFields, maxIqServerPolicyViolationNameCustomFieldName, "Max Policy Violation Name")
    lookupAndValidateCustomField(customFields, iqServerPolicyViolationThreatLevelCustomFieldName, "Policy Violation Threat Level")
    lookupAndValidateCustomField(customFields, maxIqServerPolicyViolationThreatLevelCustomFieldName, "Max Policy Violation Threat Level")
    lookupAndValidateCustomField(customFields, componentGroupCustomFieldName, "Component Group")
    lookupAndValidateCustomField(customFields, componentNameCustomFieldName, "Component Name")
    lookupAndValidateCustomField(customFields, componentVersionCustomFieldName, "Component Version")
    lookupAndValidateCustomField(customFields, componentClassifierCustomFieldName, "Component Classifier")
    lookupAndValidateCustomField(customFields, componentExtensionCustomFieldName, "Component Extension")

    jiraCustomFieldMappings.each {
      lookupAndValidateCustomField(customFields, it.customFieldName, "Passthrough Custom Field: ${it.customFieldName}")
      getPassthroughCustomField(it.customFieldName).customFieldValue = it.customFieldValue
    }
  }

  private void lookupAndValidateCustomField(List<Map<String, Object>> pCustomFields, String pFieldName, String pFieldDescription)
  {
    if (pFieldName)
    {
      JiraCustomFieldMappings returnValue = lookupCustomFieldId(pCustomFields, pFieldName)
      if (returnValue)
      {
        //TODO: show Required, but that's going back to the Issue Type Specific REST API
        logger.println("Custom Field mapping for field description: ${pFieldDescription} created mapping ${pFieldName} -> ${returnValue.customFieldId} (${returnValue.customFieldType})")

        validatedCustomFieldMappings.put(pFieldName, returnValue)
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
        returnValue = new JiraCustomFieldMappings(fieldName, null)
        returnValue.customFieldId = it.id
        returnValue.customFieldType = it.schema?.type //TODO: create an override mapping
      }
    }

    returnValue
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
  }
}
