package org.sonatype.nexus.ci.jenkins.util

import hudson.AbortException
import hudson.EnvVars
import org.sonatype.nexus.ci.jenkins.jira.JiraClient
import org.sonatype.nexus.ci.jenkins.notifier.JiraCustomFieldMappings
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification

class JiraFieldMappingUtil
{
  private JiraNotification iJiraNotification
  private JiraClient jiraClient
  private EnvVars iEnvVars
  private PrintStream logger

  String projectKey
  String issueTypeName
  String subTaskIssueTypeName
  String priorityName
  Boolean shouldCreateIndividualTickets
  Boolean shouldTransitionJiraTickets
  String transitionStatus

  private String applicationCustomFieldName //todo: limit access to custom fields via the getter
  private String organizationCustomFieldName
  private String scanStageCustomFieldName
  private String violationIdCustomFieldName
  private String violationDetectDateCustomFieldName
  private String lastScanDateCustomFieldName
  private String severityCustomFieldName
  private String cveCodeCustomFieldName
  private String cvssCustomFieldName

  String policyFilterPrefix
  String jiraDateFormatOverride
  Boolean shouldAggregateTicketsByComponent
  Boolean shouldCreateSubTasksForAggregatedTickets

  String scanTypeCustomFieldName //TODO: Remove
  String scanTypeCustomFieldValue //TODO: Remove
  String toolNameCustomFieldName //TODO: Remove
  String toolNameCustomFieldValue //TODO: Remove
  String findingTemplateCustomFieldName //TODO: Remove
  String findingTemplateCustomFieldValue //TODO: Remove
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
  JiraCustomFieldMappings getCveCodeCustomField() { return validatedCustomFieldMappings.get(cveCodeCustomFieldName) ?: new JiraCustomFieldMappings("Stub CVE", null) }
  JiraCustomFieldMappings getCvssCustomField() { return validatedCustomFieldMappings.get(cvssCustomFieldName) ?: new JiraCustomFieldMappings("Stub CVSS", null) }
  JiraCustomFieldMappings getPassthroughCustomField(String pFieldName) { return validatedCustomFieldMappings.get(pFieldName) ?: new JiraCustomFieldMappings("Stub Passthrough ${pFieldName}", null) }

  JiraCustomFieldMappings getScanTypeCustomField() { return validatedCustomFieldMappings.get(scanTypeCustomFieldName) ?: new JiraCustomFieldMappings("Stub Scan Type", null) } //TODO: Remove
  JiraCustomFieldMappings getToolNameCustomField() { return validatedCustomFieldMappings.get(toolNameCustomFieldName) ?: new JiraCustomFieldMappings("Stub Tool Name", null) } //TODO: Remove
  JiraCustomFieldMappings getFindingTemplateCustomField() { return validatedCustomFieldMappings.get(findingTemplateCustomFieldName) ?: new JiraCustomFieldMappings("Stub Finding TEmplate", null) } //TODO: Remove

  JiraFieldMappingUtil(JiraNotification pJiraNotification, JiraClient pJiraClient, EnvVars pEnvVars, PrintStream pLogger)
  {
    iJiraNotification = pJiraNotification
    jiraClient = pJiraClient
    iEnvVars = pEnvVars
    this.logger = pLogger

    validatedCustomFieldMappings = [:]

    expandEnvVars()
    assignFieldsFromConfig()
    mapCustomFieldNamesToIds()
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
    shouldTransitionJiraTickets = iJiraNotification.shouldTransitionJiraTickets
    transitionStatus = iJiraNotification.jiraTransitionStatus

    //Custom Fields
    applicationCustomFieldName = iJiraNotification.applicationCustomFieldName
    organizationCustomFieldName = iJiraNotification.organizationCustomFieldName
    scanStageCustomFieldName = iJiraNotification.scanStageCustomFieldName
    violationIdCustomFieldName = iJiraNotification.violationIdCustomFieldName
    violationDetectDateCustomFieldName = iJiraNotification.violationDetectDateCustomFieldName
    lastScanDateCustomFieldName = iJiraNotification.lastScanDateCustomFieldName
    severityCustomFieldName = iJiraNotification.severityCustomFieldName
    cveCodeCustomFieldName = iJiraNotification.cveCodeCustomFieldName
    cvssCustomFieldName = iJiraNotification.cvssCustomFieldName
    policyFilterPrefix = iJiraNotification.policyFilterPrefix
    jiraDateFormatOverride = iJiraNotification.jiraDateFormatOverride
    shouldAggregateTicketsByComponent = iJiraNotification.shouldAggregateTicketsByComponent
    shouldCreateSubTasksForAggregatedTickets = iJiraNotification.shouldCreateSubTasksForAggregatedTickets

    scanTypeCustomFieldName = iJiraNotification.scanTypeCustomFieldName //TODO: Remove
    scanTypeCustomFieldValue = iJiraNotification.scanTypeCustomFieldValue //TODO: Remove
    toolNameCustomFieldName = iJiraNotification.toolNameCustomFieldName //TODO: Remove
    toolNameCustomFieldValue = iJiraNotification.toolNameCustomFieldValue //TODO: Remove
    findingTemplateCustomFieldName = iJiraNotification.findingTemplateCustomFieldName //TODO: Remove
    findingTemplateCustomFieldValue = iJiraNotification.findingTemplateCustomFieldValue //TODO: Remove
    jiraCustomFieldMappings = iJiraNotification.jiraCustomFieldMappings ?: []
  }

  private void mapCustomFieldNamesToIds()
  {
    // todo: use the response for some validation and automatic formatting of the custom fields
    jiraClient.lookupMetadataConfigurationForCreateIssue(projectKey, issueTypeName)
    if (shouldCreateSubTasksForAggregatedTickets)
    {
      jiraClient.lookupMetadataConfigurationForCreateIssue(projectKey, subTaskIssueTypeName)
    }

    List customFields = (List) jiraClient.lookupCustomFields()

    //todo: can we use a getter here?
    lookupAndValidateCustomField(customFields, applicationCustomFieldName, "App Name")
    lookupAndValidateCustomField(customFields, organizationCustomFieldName, "Org Name")
    lookupAndValidateCustomField(customFields, scanStageCustomFieldName, "Scan Stage")
    lookupAndValidateCustomField(customFields, violationIdCustomFieldName, "Violation ID")
    lookupAndValidateCustomField(customFields, violationDetectDateCustomFieldName,"Detect Date")
    lookupAndValidateCustomField(customFields, lastScanDateCustomFieldName,"Last Scan Date")
    lookupAndValidateCustomField(customFields, severityCustomFieldName, "Severity")
    lookupAndValidateCustomField(customFields, cveCodeCustomFieldName, "CVE Code")
    lookupAndValidateCustomField(customFields, cvssCustomFieldName, "CVSS")

    lookupAndValidateCustomField(customFields, scanTypeCustomFieldName, "Scan Type") //TODO: Remove
    lookupAndValidateCustomField(customFields, toolNameCustomFieldName, "Tool Name") //TODO: Remove
    lookupAndValidateCustomField(customFields, findingTemplateCustomFieldName,"Finding Template") //TODO: Remove

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

  void addCustomFieldsToTicket(Map returnValue,
                               String iqAppExternalId,
                               String iqOrgExternalId,
                               String scanStage,
                               String nowFormatted,
                               String severityString,
                               String cveCode,
                               Double cvss,
                               String violationUniqueId)
  {
    //TODO: are these available anywhere else (i.e. where they're defined) so i don't have to pass them all over the place
    getApplicationCustomField().customFieldValue = iqAppExternalId
    getOrganizationCustomField().customFieldValue = iqOrgExternalId
    getScanStageCustomField().customFieldValue = scanStage
    getViolationIdCustomField().customFieldValue =  violationUniqueId
    getViolationDetectDateCustomField().customFieldValue = nowFormatted
    getLastScanDateCustomField().customFieldValue =  nowFormatted
    getSeverityCustomField().customFieldValue =  severityString
    getCveCodeCustomField().customFieldValue =  cveCode
    getCvssCustomField().customFieldValue =  cvss

    getScanTypeCustomField().customFieldValue =  scanTypeCustomFieldValue //TODO: remove
    getToolNameCustomField().customFieldValue =  toolNameCustomFieldValue //TODO: remove
    getFindingTemplateCustomField().customFieldValue =  findingTemplateCustomFieldValue  //TODO: remove

    validatedCustomFieldMappings.each {
      addCustomFieldToTicket(returnValue, it.value)
    }
  }

  //TODO: JSON formatting for custom fields: https://developer.atlassian.com/server/jira/platform/jira-rest-api-examples/#creating-an-issue-using-custom-fields
  //       todo; review fields - https://mail.google.com/mail/u/0/#inbox/QgrcJHsHsJScdtsQNFlKnWrWCwwblmVFScB
  private static addCustomFieldToTicket(Map ticketFieldsArray, JiraCustomFieldMappings pField)
  {
    if (pField.customFieldId && pField.customFieldValue)
    {
      def returnValue
      switch (pField.customFieldType)
      {
        case "date": //todo: short date
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
