package org.sonatype.nexus.ci.jenkins.util

import hudson.AbortException
import hudson.EnvVars
import org.sonatype.nexus.ci.jenkins.jira.JiraClient
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
  String applicationCustomFieldName
  String organizationCustomFieldName
  String scanStageCustomFieldName
  String violationIdCustomFieldName
  String violationDetectDateCustomFieldName
  String lastScanDateCustomFieldName
  String severityCustomFieldName
  String cveCodeCustomFieldName
  String cvssCustomFieldName
  String policyFilterPrefix
  Boolean shouldAggregateTicketsByComponent
  Boolean shouldCreateSubTasksForAggregatedTickets
  String scanTypeCustomFieldName
  String scanTypeCustomFieldValue
  String toolNameCustomFieldName
  String toolNameCustomFieldValue
  String findingTemplateCustomFieldName
  String findingTemplateCustomFieldValue

  //Custom Field IDs
  //  Populated by calling the JIRA REST API to get all of the custom fields,
  //  then looking up by name to get the ID
  String applicationCustomFieldId
  String organizationCustomFieldId
  String scanStageCustomFieldId
  String violationIdCustomFieldId
  String violationDetectDateCustomFieldId
  String lastScanDateCustomFieldId
  String severityCustomFieldId
  String cveCodeCustomFieldId
  String cvssCustomFieldId
  String scanTypeCustomFieldId
  String toolNameCustomFieldId
  String findingTemplateCustomFieldId

  JiraFieldMappingUtil(JiraNotification pJiraNotification, JiraClient pJiraClient, EnvVars pEnvVars, PrintStream pLogger)
  {
    iJiraNotification = pJiraNotification
    jiraClient = pJiraClient
    iEnvVars = pEnvVars
    this.logger = pLogger
  }

  void expandEnvVars()
  {
    projectKey = iEnvVars.expand(iJiraNotification.projectKey) //TODO: do I need to expand any other fields?
  }

  void assignFieldsFromConfig()
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
    shouldAggregateTicketsByComponent = iJiraNotification.shouldAggregateTicketsByComponent
    shouldCreateSubTasksForAggregatedTickets = iJiraNotification.shouldCreateSubTasksForAggregatedTickets
    scanTypeCustomFieldName = iJiraNotification.scanTypeCustomFieldName
    scanTypeCustomFieldValue = iJiraNotification.scanTypeCustomFieldValue
    toolNameCustomFieldName = iJiraNotification.toolNameCustomFieldName
    toolNameCustomFieldValue = iJiraNotification.toolNameCustomFieldValue
    findingTemplateCustomFieldName = iJiraNotification.findingTemplateCustomFieldName
    findingTemplateCustomFieldValue = iJiraNotification.findingTemplateCustomFieldValue
  }

  void mapCustomFieldNamesToIds()
  {
    // todo: use the response for some validation and automatic formatting of the custom fields
    jiraClient.lookupMetadataConfigurationForCreateIssue(projectKey, issueTypeName)
    if (shouldCreateSubTasksForAggregatedTickets)
    {
      jiraClient.lookupMetadataConfigurationForCreateIssue(projectKey, subTaskIssueTypeName)
    }

    List customFields = (List) jiraClient.lookupCustomFields()

    applicationCustomFieldId = lookupAndValidateCustomField(customFields, applicationCustomFieldName, "App Name")
    organizationCustomFieldId = lookupAndValidateCustomField(customFields, organizationCustomFieldName, "Org Name")
    scanStageCustomFieldId = lookupAndValidateCustomField(customFields, scanStageCustomFieldName, "Scan Stage")
    violationIdCustomFieldId = lookupAndValidateCustomField(customFields, violationIdCustomFieldName, "Violation ID")
    violationDetectDateCustomFieldId = lookupAndValidateCustomField(customFields, violationDetectDateCustomFieldName,"Detect Date")
    lastScanDateCustomFieldId = lookupAndValidateCustomField(customFields, lastScanDateCustomFieldName,"Last Scan Date")
    severityCustomFieldId = lookupAndValidateCustomField(customFields, severityCustomFieldName, "Severity")
    cveCodeCustomFieldId = lookupAndValidateCustomField(customFields, cveCodeCustomFieldName, "CVE Code")
    cvssCustomFieldId = lookupAndValidateCustomField(customFields, cvssCustomFieldName, "CVSS")
    scanTypeCustomFieldId = lookupAndValidateCustomField(customFields, scanTypeCustomFieldName, "Scan Type")
    toolNameCustomFieldId = lookupAndValidateCustomField(customFields, toolNameCustomFieldName, "Tool Name")
    findingTemplateCustomFieldId = lookupAndValidateCustomField(customFields, findingTemplateCustomFieldName,"Finding Template")
  }

  private String lookupAndValidateCustomField(List<Map<String, Object>> pCustomFields, String pFieldName, String pFieldDescription)
  {
    String returnValue = null
    if(pFieldName)
    {
      returnValue = lookupCustomFieldId(pCustomFields, pFieldName)
      if (returnValue)
      {
        logger.println("Custom Field mapping for field description: ${pFieldDescription} created mapping ${pFieldName} -> ${returnValue}")
      } else
      {
        throw new AbortException("Custom Field mapping for field description: ${pFieldDescription}, not found with field name: ${pFieldName}")
      }
    }
    else
    {
      logger.println("Custom Field mapping not provided for field description: ${pFieldDescription}")
    }

    return returnValue
  }

  private static String lookupCustomFieldId(List<Map<String, Object>> customFields, String fieldName)
  {
    String returnValue = null
    customFields.each {
      if(it.name == fieldName)
      {
        returnValue = it.id
      }
    }

    returnValue
  }

}
