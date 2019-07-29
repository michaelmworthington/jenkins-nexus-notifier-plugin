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
package org.sonatype.nexus.ci.jenkins.notifier;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.sonatype.nexus.ci.jenkins.config.JiraConfiguration;
import org.sonatype.nexus.ci.jenkins.config.NotifierConfiguration;
import org.sonatype.nexus.ci.jenkins.util.FormUtil;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class JiraNotification
    extends AbstractDescribableImpl<JiraNotification>
{
  private boolean sendJiraNotification;
  private String projectKey;
  private String issueTypeName;
  private String subTaskIssueTypeName;
  private String priorityName;
  private boolean shouldCreateIndividualTickets;
  private boolean shouldAggregateTicketsByComponent;
  private boolean shouldCreateSubTasksForAggregatedTickets;
  private boolean shouldTransitionJiraTickets;
  private String jiraTransitionStatus;
  private String jiraTransitionName;
  private String policyFilterPrefix;
  private int policyFilterThreatLevel; //todo

  //Advanced Options
  private String jobJiraCredentialsId;
  private String jobIQCredentialsId;
  private boolean verboseLogging;
  private boolean dryRun;
  private String jiraDateFormatOverride;
  private boolean disableJqlFieldFilter;
  private int jqlMaxResultsOverride;
  private List<JiraCustomFieldTypeOverride> jiraCustomFieldTypeOverrideMapping;

  //Custom Field Options
  private String applicationCustomFieldName;
  private String organizationCustomFieldName;
  private String scanStageCustomFieldName;
  private String violationIdCustomFieldName;
  private String violationDetectDateCustomFieldName;
  private String lastScanDateCustomFieldName;
  private String severityCustomFieldName;
  private String maxSeverityCustomFieldName;
  private String cveCodeCustomFieldName;
  private String maxCveCodeCustomFieldName;
  private String cveLinkCustomFieldName;          //todo
  private String maxCveLinkCustomFieldName;       //todo
  private String cweCodeCustomFieldName;          //todo - move to experimental section
  private String maxCweCodeCustomFieldName;       //todo - move to experimental section
  private String threatVectorCustomFieldName;     //todo - move to experimental section
  private String maxThreatVectorCustomFieldName;  //todo - move to experimental section
  private String cvssCustomFieldName;
  private String maxCvssCustomFieldName;
  private String iqServerReportLinkCustomFieldName;
  private String iqServerPolicyViolationNameCustomFieldName;
  private String maxIqServerPolicyViolationNameCustomFieldName;
  private String iqServerPolicyViolationThreatLevelCustomFieldName;
  private String maxIqServerPolicyViolationThreatLevelCustomFieldName;
  private String declaredLicensesCustomFieldName;
  private String observedLicensesCustomFieldName;
  private String effectiveLicensesCustomFieldName;
  private String fileOccurrencesCustomFieldName;
  private String recommendedRemediationCustomFieldName;
  private String purlCustomFieldName;
  private String componentCombinedIdentifierCustomFieldName;
  private String componentGroupCustomFieldName;
  private String componentNameCustomFieldName;
  private String componentVersionCustomFieldName;
  private String componentClassifierCustomFieldName;
  private String componentExtensionCustomFieldName;
  private List<JiraCustomFieldMappings> jiraCustomFieldMappings;

  public boolean getSendJiraNotification() { return sendJiraNotification; }
  public String getProjectKey() { return projectKey; }
  public String getIssueTypeName() { return issueTypeName; }
  public String getSubTaskIssueTypeName() { return subTaskIssueTypeName; }
  public String getPriorityName() { return priorityName; }
  public boolean getShouldCreateIndividualTickets() { return shouldCreateIndividualTickets; }
  public boolean getShouldAggregateTicketsByComponent() { return shouldAggregateTicketsByComponent; }
  public boolean getShouldCreateSubTasksForAggregatedTickets() { return shouldCreateSubTasksForAggregatedTickets; }
  public boolean getShouldTransitionJiraTickets() { return shouldTransitionJiraTickets; }
  public String getJiraTransitionStatus() { return jiraTransitionStatus; }
  public String getJiraTransitionName() { return jiraTransitionName; }
  public String getPolicyFilterPrefix() { return policyFilterPrefix; }

  //Advanced Options
  public String getJobJiraCredentialsId() { return jobJiraCredentialsId; }
  public String getJobIQCredentialsId() { return jobIQCredentialsId; }
  public boolean getVerboseLogging() { return verboseLogging; }
  public boolean getDryRun() { return dryRun; }
  public String getJiraDateFormatOverride() { return jiraDateFormatOverride; }
  public boolean getDisableJqlFieldFilter() { return disableJqlFieldFilter; }
  public int getJqlMaxResultsOverride() { return jqlMaxResultsOverride; }
  public List<JiraCustomFieldTypeOverride> getJiraCustomFieldTypeOverrideMapping() { return jiraCustomFieldTypeOverrideMapping; } //todo

  //Custom Field Options
  public List<JiraCustomFieldMappings> getJiraCustomFieldMappings() { return jiraCustomFieldMappings; }
  public String getApplicationCustomFieldName() { return applicationCustomFieldName; }
  public String getOrganizationCustomFieldName() { return organizationCustomFieldName; }
  public String getScanStageCustomFieldName() { return scanStageCustomFieldName; }
  public String getViolationIdCustomFieldName() { return violationIdCustomFieldName; }
  public String getViolationDetectDateCustomFieldName() { return violationDetectDateCustomFieldName; }
  public String getLastScanDateCustomFieldName() { return lastScanDateCustomFieldName; }
  public String getSeverityCustomFieldName() { return severityCustomFieldName; }
  public String getMaxSeverityCustomFieldName() { return maxSeverityCustomFieldName; }
  public String getCveCodeCustomFieldName() { return cveCodeCustomFieldName; }
  public String getMaxCveCodeCustomFieldName() { return maxCveCodeCustomFieldName; }
  public String getCveLinkCustomFieldName() { return cveLinkCustomFieldName; }
  public String getMaxCveLinkCustomFieldName() { return maxCveLinkCustomFieldName; }
  public String getCweCodeCustomFieldName() { return cweCodeCustomFieldName; }
  public String getMaxCweCodeCustomFieldName() { return maxCweCodeCustomFieldName; }
  public String getThreatVectorCustomFieldName() { return threatVectorCustomFieldName; }
  public String getMaxThreatVectorCustomFieldName() { return maxThreatVectorCustomFieldName; }
  public String getCvssCustomFieldName() { return cvssCustomFieldName; }
  public String getMaxCvssCustomFieldName() { return maxCvssCustomFieldName; }
  public String getIqServerReportLinkCustomFieldName() { return iqServerReportLinkCustomFieldName; }
  public String getIqServerPolicyViolationNameCustomFieldName() { return iqServerPolicyViolationNameCustomFieldName; }
  public String getMaxIqServerPolicyViolationNameCustomFieldName() { return maxIqServerPolicyViolationNameCustomFieldName; }
  public String getIqServerPolicyViolationThreatLevelCustomFieldName() { return iqServerPolicyViolationThreatLevelCustomFieldName; }
  public String getMaxIqServerPolicyViolationThreatLevelCustomFieldName() { return maxIqServerPolicyViolationThreatLevelCustomFieldName; }
  public String getDeclaredLicensesCustomFieldName() { return declaredLicensesCustomFieldName;  }
  public String getObservedLicensesCustomFieldName() { return observedLicensesCustomFieldName;  }
  public String getEffectiveLicensesCustomFieldName() { return effectiveLicensesCustomFieldName;  }
  public String getFileOccurrencesCustomFieldName()  {    return fileOccurrencesCustomFieldName;  }
  public String getRecommendedRemediationCustomFieldName()  {    return recommendedRemediationCustomFieldName;  }
  public String getPurlCustomFieldName()  {    return purlCustomFieldName;  }
  public String getComponentCombinedIdentifierCustomFieldName()  {    return componentCombinedIdentifierCustomFieldName;  }
  public String getComponentGroupCustomFieldName() { return componentGroupCustomFieldName; }
  public String getComponentNameCustomFieldName() { return componentNameCustomFieldName; }
  public String getComponentVersionCustomFieldName() { return componentVersionCustomFieldName; }
  public String getComponentClassifierCustomFieldName() { return componentClassifierCustomFieldName; }
  public String getComponentExtensionCustomFieldName() { return componentExtensionCustomFieldName; }

  @DataBoundConstructor
  public JiraNotification(final boolean sendJiraNotification,
                          final String projectKey,
                          final String issueTypeName,
                          final String subTaskIssueTypeName,
                          final String priorityName,
                          final boolean shouldCreateIndividualTickets,
                          final boolean shouldAggregateTicketsByComponent,
                          final boolean shouldCreateSubTasksForAggregatedTickets,
                          final boolean shouldTransitionJiraTickets,
                          final String jiraTransitionStatus,
                          final String jiraTransitionName,
                          final String policyFilterPrefix,
                          final String jobJiraCredentialsId,
                          final String jobIQCredentialsId,
                          final boolean verboseLogging,
                          final boolean dryRun,
                          final String jiraDateFormatOverride,
                          final boolean disableJqlFieldFilter,
                          final int jqlMaxResultsOverride,
                          final List<JiraCustomFieldTypeOverride> jiraCustomFieldTypeOverrideMapping,
                          final String applicationCustomFieldName,
                          final String organizationCustomFieldName,
                          final String scanStageCustomFieldName,
                          final String violationIdCustomFieldName,
                          final String violationDetectDateCustomFieldName,
                          final String lastScanDateCustomFieldName,
                          final String severityCustomFieldName,
                          final String maxSeverityCustomFieldName,
                          final String cveCodeCustomFieldName,
                          final String maxCveCodeCustomFieldName,
                          final String cveLinkCustomFieldName,
                          final String maxCveLinkCustomFieldName,
                          final String cweCodeCustomFieldName,
                          final String maxCweCodeCustomFieldName,
                          final String threatVectorCustomFieldName,
                          final String maxThreatVectorCustomFieldName,
                          final String cvssCustomFieldName,
                          final String maxCvssCustomFieldName,
                          final String iqServerReportLinkCustomFieldName,
                          final String iqServerPolicyViolationNameCustomFieldName,
                          final String maxIqServerPolicyViolationNameCustomFieldName,
                          final String iqServerPolicyViolationThreatLevelCustomFieldName,
                          final String maxIqServerPolicyViolationThreatLevelCustomFieldName,
                          final String declaredLicensesCustomFieldName,
                          final String observedLicensesCustomFieldName,
                          final String effectiveLicensesCustomFieldName,
                          final String fileOccurrencesCustomFieldName,
                          final String recommendedRemediationCustomFieldName,
                          final String purlCustomFieldName,
                          final String componentCombinedIdentifierCustomFieldName,
                          final String componentGroupCustomFieldName,
                          final String componentNameCustomFieldName,
                          final String componentVersionCustomFieldName,
                          final String componentClassifierCustomFieldName,
                          final String componentExtensionCustomFieldName,
                          final List<JiraCustomFieldMappings> jiraCustomFieldMappings)
  {
    this.sendJiraNotification = sendJiraNotification;
    this.projectKey = projectKey;
    this.issueTypeName = issueTypeName;
    this.subTaskIssueTypeName = subTaskIssueTypeName;
    this.priorityName = priorityName;
    this.shouldCreateIndividualTickets = shouldCreateIndividualTickets;
    this.shouldAggregateTicketsByComponent = shouldAggregateTicketsByComponent;
    this.shouldCreateSubTasksForAggregatedTickets = shouldCreateSubTasksForAggregatedTickets;
    this.shouldTransitionJiraTickets = shouldTransitionJiraTickets;
    this.jiraTransitionStatus = jiraTransitionStatus;
    this.jiraTransitionName = jiraTransitionName;
    this.policyFilterPrefix = policyFilterPrefix;

    //Advanced Options
    this.jobJiraCredentialsId = jobJiraCredentialsId;
    this.jobIQCredentialsId = jobIQCredentialsId;
    this.verboseLogging = verboseLogging;
    this.dryRun = dryRun;
    this.jiraDateFormatOverride = jiraDateFormatOverride;
    this.disableJqlFieldFilter = disableJqlFieldFilter;
    this.jqlMaxResultsOverride = jqlMaxResultsOverride;
    this.jiraCustomFieldTypeOverrideMapping = jiraCustomFieldTypeOverrideMapping;

    //Custom Field Mappings
    this.applicationCustomFieldName = applicationCustomFieldName;
    this.organizationCustomFieldName = organizationCustomFieldName;
    this.scanStageCustomFieldName = scanStageCustomFieldName;
    this.violationIdCustomFieldName = violationIdCustomFieldName;
    this.violationDetectDateCustomFieldName = violationDetectDateCustomFieldName;
    this.lastScanDateCustomFieldName = lastScanDateCustomFieldName;
    this.severityCustomFieldName = severityCustomFieldName;
    this.maxSeverityCustomFieldName = maxSeverityCustomFieldName;
    this.cveCodeCustomFieldName = cveCodeCustomFieldName;
    this.maxCveCodeCustomFieldName = maxCveCodeCustomFieldName;
    this.cveLinkCustomFieldName = cveLinkCustomFieldName;
    this.maxCveLinkCustomFieldName = maxCveLinkCustomFieldName;
    this.cweCodeCustomFieldName = cweCodeCustomFieldName;
    this.maxCweCodeCustomFieldName = maxCweCodeCustomFieldName;
    this.threatVectorCustomFieldName = threatVectorCustomFieldName;
    this.maxThreatVectorCustomFieldName = maxThreatVectorCustomFieldName;
    this.cvssCustomFieldName = cvssCustomFieldName;
    this.maxCvssCustomFieldName = maxCvssCustomFieldName;
    this.iqServerReportLinkCustomFieldName = iqServerReportLinkCustomFieldName;
    this.iqServerPolicyViolationNameCustomFieldName = iqServerPolicyViolationNameCustomFieldName;
    this.maxIqServerPolicyViolationNameCustomFieldName = maxIqServerPolicyViolationNameCustomFieldName;
    this.iqServerPolicyViolationThreatLevelCustomFieldName = iqServerPolicyViolationThreatLevelCustomFieldName;
    this.maxIqServerPolicyViolationThreatLevelCustomFieldName = maxIqServerPolicyViolationThreatLevelCustomFieldName;
    this.declaredLicensesCustomFieldName = declaredLicensesCustomFieldName;
    this.observedLicensesCustomFieldName = observedLicensesCustomFieldName;
    this.effectiveLicensesCustomFieldName = effectiveLicensesCustomFieldName;
    this.fileOccurrencesCustomFieldName = fileOccurrencesCustomFieldName;
    this.recommendedRemediationCustomFieldName = recommendedRemediationCustomFieldName;
    this.purlCustomFieldName = purlCustomFieldName;
    this.componentCombinedIdentifierCustomFieldName = componentCombinedIdentifierCustomFieldName;
    this.componentGroupCustomFieldName = componentGroupCustomFieldName;
    this.componentNameCustomFieldName = componentNameCustomFieldName;
    this.componentVersionCustomFieldName = componentVersionCustomFieldName;
    this.componentClassifierCustomFieldName = componentClassifierCustomFieldName;
    this.componentExtensionCustomFieldName = componentExtensionCustomFieldName;
    this.jiraCustomFieldMappings = jiraCustomFieldMappings;
  }

//  @Override
//  public Descriptor<JiraNotification> getDescriptor() {
//    return Jenkins.get().getDescriptorOrDie(this.getClass());
//  }

  @Extension
  @Symbol("nexusJiraNotification")
  public static final class DescriptorImpl
      extends Descriptor<JiraNotification>
  {
    @Override
    public String getDisplayName() {
      return Messages.JiraNotification_DisplayName();
    }

    public FormValidation doCheckProjectKey(@QueryParameter Boolean sendJiraNotification, @QueryParameter String projectKey) {
      if(Boolean.FALSE.equals(sendJiraNotification))
      {
        return FormValidation.ok();
      }
      else
      {
        return FormUtil.validateNotEmpty(projectKey, Messages.JiraNotification_ProjectKeyRequired());
      }
    }

    public FormValidation doCheckJiraTransitionStatus(@QueryParameter Boolean shouldTransitionJiraTickets, @QueryParameter String jiraTransitionStatus) {
      if(Boolean.FALSE.equals(shouldTransitionJiraTickets))
      {
        return FormValidation.ok();
      }
      else
      {
        return FormUtil.validateNotEmpty(jiraTransitionStatus, Messages.JiraNotification_TransitionStatusRequired());
      }
    }

    public FormValidation doCheckJiraTransitionName(@QueryParameter Boolean shouldTransitionJiraTickets, @QueryParameter String jiraTransitionName) {
      if(Boolean.FALSE.equals(shouldTransitionJiraTickets))
      {
        return FormValidation.ok();
      }
      else
      {
        return FormUtil.validateNotEmpty(jiraTransitionName, Messages.JiraNotification_TransitionNameRequired());
      }
    }

    //TODO: create more validations up front

    public ListBoxModel doFillJobJiraCredentialsIdItems(@AncestorInPath final Job job) {
      NotifierConfiguration configuration = NotifierConfiguration.getNotifierConfiguration();
      checkArgument(configuration != null, Messages.JiraClientFactory_NoConfiguration());
      checkArgument(configuration.getJiraConfigs() != null, Messages.JiraClientFactory_NoConfiguration());
      checkArgument(configuration.getJiraConfigs().size() > 0, Messages.JiraClientFactory_NoConfiguration());

      JiraConfiguration jiraConfiguration = configuration.getJiraConfigs().get(0);
      return FormUtil.newCredentialsItemsListBoxModel(jiraConfiguration.getJiraServerUrl(),
                                                      jiraConfiguration.getJiraCredentialsId(),
                                                      job);
    }

    public ListBoxModel doFillJobIQCredentialsIdItems(@AncestorInPath final Job job) {
      NotifierConfiguration configuration = NotifierConfiguration.getNotifierConfiguration();
      checkArgument(configuration != null, Messages.JiraClientFactory_NoConfiguration());
      checkArgument(configuration.getJiraConfigs() != null, Messages.JiraClientFactory_NoConfiguration());
      checkArgument(configuration.getJiraConfigs().size() > 0, Messages.JiraClientFactory_NoConfiguration());

      JiraConfiguration jiraConfiguration = configuration.getJiraConfigs().get(0);
      return FormUtil.newCredentialsItemsListBoxModel(jiraConfiguration.getIqServerUrl(),
                                                      jiraConfiguration.getIqCredentialsId(),
                                                      job);
    }
  }
}
