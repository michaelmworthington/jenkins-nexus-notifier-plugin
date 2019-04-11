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
  private String policyFilterPrefix;

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
  private String cveCodeCustomFieldName;
  private String cvssCustomFieldName;
  private String iqServerReportLinkCustomFieldName;
  private String iqServerPolicyViolationNameCustomFieldName;
  private String iqServerPolicyViolationThreatLevelCustomFieldName;
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
  public String getPolicyFilterPrefix() { return policyFilterPrefix; }

  //Advanced Options
  public String getJobJiraCredentialsId() { return jobJiraCredentialsId; }
  public String getJobIQCredentialsId() { return jobIQCredentialsId; }
  public boolean getVerboseLogging() { return verboseLogging; }
  public boolean getDryRun() { return dryRun; } //todo
  public String getJiraDateFormatOverride() { return jiraDateFormatOverride; }
  public boolean getDisableJqlFieldFilter() { return disableJqlFieldFilter; } //todo
  public int getJqlMaxResultsOverride() { return jqlMaxResultsOverride; } //todo
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
  public String getCveCodeCustomFieldName() { return cveCodeCustomFieldName; }
  public String getCvssCustomFieldName() { return cvssCustomFieldName; }
  public String getIqServerReportLinkCustomFieldName() { return iqServerReportLinkCustomFieldName; }
  public String getIqServerPolicyViolationNameCustomFieldName() { return iqServerPolicyViolationNameCustomFieldName; }
  public String getIqServerPolicyViolationThreatLevelCustomFieldName() { return iqServerPolicyViolationThreatLevelCustomFieldName; }

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
                          final String cveCodeCustomFieldName,
                          final String cvssCustomFieldName,
                          final String iqServerReportLinkCustomFieldName,
                          final String iqServerPolicyViolationNameCustomFieldName,
                          final String iqServerPolicyViolationThreatLevelCustomFieldName,
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
    this.cveCodeCustomFieldName = cveCodeCustomFieldName;
    this.cvssCustomFieldName = cvssCustomFieldName;
    this.iqServerReportLinkCustomFieldName = iqServerReportLinkCustomFieldName;
    this.iqServerPolicyViolationNameCustomFieldName = iqServerPolicyViolationNameCustomFieldName;
    this.iqServerPolicyViolationThreatLevelCustomFieldName = iqServerPolicyViolationThreatLevelCustomFieldName;
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
