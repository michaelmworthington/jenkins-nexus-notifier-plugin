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
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.sonatype.nexus.ci.jenkins.config.JiraConfiguration;
import org.sonatype.nexus.ci.jenkins.config.NotifierConfiguration;
import org.sonatype.nexus.ci.jenkins.util.FormUtil;

import static com.google.common.base.Preconditions.checkArgument;

public class JiraNotification
    implements Describable<JiraNotification>
{
  private boolean sendJiraNotification;
  private String projectKey;

  private boolean shouldCreateIndividualTickets;

  private boolean shouldTransitionJiraTickets;
  private String jiraTransitionStatus;
  private String applicationCustomFieldName;
  private String organizationCustomFieldName;
  private String violationIdCustomFieldName;
  private String policyFilterPrefix;
  private boolean shouldAggregateTicketsByComponent;

  private String jobJiraCredentialsId;
  private String jobIQCredentialsId;

  public boolean getSendJiraNotification() {
    return sendJiraNotification;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public boolean getShouldCreateIndividualTickets() { return shouldCreateIndividualTickets; }

  public boolean getShouldTransitionJiraTickets() { return shouldTransitionJiraTickets; }

  public String getJiraTransitionStatus() { return jiraTransitionStatus; }

  public String getApplicationCustomFieldName() { return applicationCustomFieldName; }

  public String getOrganizationCustomFieldName() { return organizationCustomFieldName; }

  public String getViolationIdCustomFieldName() { return violationIdCustomFieldName; }

  public String getPolicyFilterPrefix() { return policyFilterPrefix; }

  public boolean getShouldAggregateTicketsByComponent() { return shouldAggregateTicketsByComponent; }

  public String getJobJiraCredentialsId() { return jobJiraCredentialsId; }
  public String getJobIQCredentialsId() { return jobIQCredentialsId; }

  @DataBoundConstructor
  public JiraNotification(final boolean sendJiraNotification,
                          final String projectKey,
                          final boolean shouldCreateIndividualTickets,
                          final boolean shouldTransitionJiraTickets,
                          final String jiraTransitionStatus,
                          final String applicationCustomFieldName,
                          final String organizationCustomFieldName,
                          final String violationIdCustomFieldName,
                          final String policyFilterPrefix,
                          final boolean shouldAggregateTicketsByComponent,
                          final String jobJiraCredentialsId,
                          final String jobIQCredentialsId)
  {
    this.sendJiraNotification = sendJiraNotification;
    this.projectKey = projectKey;
    this.shouldCreateIndividualTickets = shouldCreateIndividualTickets;
    this.shouldTransitionJiraTickets = shouldTransitionJiraTickets;
    this.jiraTransitionStatus = jiraTransitionStatus;
    this.applicationCustomFieldName = applicationCustomFieldName;
    this.organizationCustomFieldName = organizationCustomFieldName;
    this.violationIdCustomFieldName = violationIdCustomFieldName;
    this.policyFilterPrefix = policyFilterPrefix;
    this.shouldAggregateTicketsByComponent = shouldAggregateTicketsByComponent;
    this.jobJiraCredentialsId = jobJiraCredentialsId;
    this.jobIQCredentialsId = jobIQCredentialsId;
  }

  @Override
  public Descriptor<JiraNotification> getDescriptor() {
    return Jenkins.get().getDescriptorOrDie(this.getClass());
  }

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
