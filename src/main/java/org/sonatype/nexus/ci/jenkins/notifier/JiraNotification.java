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
import org.sonatype.nexus.ci.jenkins.config.BitbucketConfiguration;
import org.sonatype.nexus.ci.jenkins.config.JiraConfiguration;
import org.sonatype.nexus.ci.jenkins.config.NotifierConfiguration;
import org.sonatype.nexus.ci.jenkins.util.FormUtil;

import static com.google.common.base.Preconditions.checkArgument;

public class JiraNotification
    implements Describable<JiraNotification>
{
  private boolean sendJiraNotification;

  private String projectKey;

  private String jobCredentialsId;

  public boolean getSendJiraNotification() {
    return sendJiraNotification;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public String getJobCredentialsId() {
    return jobCredentialsId;
  }

  @DataBoundConstructor
  public JiraNotification(final boolean sendJiraNotification,
                          final String projectKey,
                          final String jobCredentialsId)
  {
    this.sendJiraNotification = sendJiraNotification;
    this.projectKey = projectKey;
    this.jobCredentialsId = jobCredentialsId;
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

    public ListBoxModel doFillJobCredentialsIdItems(@AncestorInPath final Job job) {
      NotifierConfiguration configuration = NotifierConfiguration.getNotifierConfiguration();
      checkArgument(configuration != null, Messages.JiraClientFactory_NoConfiguration());
      checkArgument(configuration.getBitbucketConfigs() != null, Messages.JiraClientFactory_NoConfiguration());
      checkArgument(configuration.getBitbucketConfigs().size() > 0, Messages.JiraClientFactory_NoConfiguration());

      JiraConfiguration jiraConfiguration = configuration.getJiraConfigs().get(0);
      return FormUtil.newCredentialsItemsListBoxModel(jiraConfiguration.getServerUrl(),
          jiraConfiguration.getCredentialsId(), job);
    }
  }
}
