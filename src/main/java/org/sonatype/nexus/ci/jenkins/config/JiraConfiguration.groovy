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
package org.sonatype.nexus.ci.jenkins.config

import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Describable
import hudson.model.Descriptor
import hudson.util.FormValidation
import hudson.util.FormValidation.Kind
import hudson.util.ListBoxModel
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter
import org.sonatype.nexus.ci.jenkins.iq.IQClient
import org.sonatype.nexus.ci.jenkins.iq.IQClientFactory
import org.sonatype.nexus.ci.jenkins.jira.JiraClient
import org.sonatype.nexus.ci.jenkins.jira.JiraClientFactory
import org.sonatype.nexus.ci.jenkins.util.FormUtil

import javax.annotation.Nullable

class JiraConfiguration
    extends AbstractDescribableImpl<JiraConfiguration>
{
  String jiraServerUrl
  String jiraCredentialsId
  String iqServerUrl
  String iqCredentialsId

  @DataBoundConstructor
  JiraConfiguration(final String jiraServerUrl, final String jiraCredentialsId, final String iqServerUrl, final String iqCredentialsId) {
    this.jiraServerUrl = jiraServerUrl
    this.jiraCredentialsId = jiraCredentialsId
    this.iqServerUrl = iqServerUrl
    this.iqCredentialsId = iqCredentialsId
  }

//  @Override
//  Descriptor<JiraConfiguration> getDescriptor() {
//    return Jenkins.get().getDescriptorOrDie(this.getClass())
//  }

  @Extension
  static class DescriptorImpl
      extends Descriptor<JiraConfiguration>
  {
    @Override
    String getDisplayName() {
      Messages.JiraConfiguration_DisplayName()
    }

    @SuppressWarnings('unused')
    FormValidation doCheckJiraServerUrl(@QueryParameter String value) {
      def validation = FormUtil.validateUrl(value)
      if (validation.kind == Kind.OK) {
        validation = FormUtil.validateNotEmpty(value, Messages.Configuration_ServerUrlRequired())
      }
      return validation
    }

    @SuppressWarnings('unused')
    ListBoxModel doFillJiraCredentialsIdItems(@QueryParameter String jiraServerUrl,
                                              @QueryParameter String jiraCredentialsId) {
      return FormUtil.newCredentialsItemsListBoxModel(jiraServerUrl, jiraCredentialsId, null)
    }

    @SuppressWarnings('unused')
    FormValidation doVerifyJiraCredentials(
            @QueryParameter String jiraServerUrl,
            @QueryParameter @Nullable String jiraCredentialsId) throws IOException
    {
      try {
        JiraClient jiraClient = JiraClientFactory.getJiraClientForUrl(jiraCredentialsId, jiraServerUrl)
        List customFields = (List) jiraClient.lookupCustomFields()

        return FormValidation.ok(Messages.JiraConfiguration_ConnectionSucceeded(customFields.size()))
      }
      catch (Exception e) {
        return FormValidation.error(e, Messages.JiraConfiguration_ConnectionFailed())
      }

    }

    @SuppressWarnings('unused')
    FormValidation doCheckIqServerUrl(@QueryParameter String value) {
      def validation = FormUtil.validateUrl(value)
      if (validation.kind == Kind.OK) {
        validation = FormUtil.validateNotEmpty(value, Messages.Configuration_ServerUrlRequired())
      }
      return validation
    }

    @SuppressWarnings('unused')
    ListBoxModel doFillIqCredentialsIdItems(@QueryParameter String iqServerUrl,
                                            @QueryParameter String iqCredentialsId) {
      return FormUtil.newCredentialsItemsListBoxModel(iqServerUrl, iqCredentialsId, null)
    }

    @SuppressWarnings('unused')
    FormValidation doVerifyIqCredentials(
            @QueryParameter String iqServerUrl,
            @QueryParameter @Nullable String iqCredentialsId) throws IOException
    {
      try
      {
        IQClient iqClient = IQClientFactory.getIQClientForUrl(iqCredentialsId, iqServerUrl)
        def applicationsResp = iqClient.lookupApplications()

        return FormValidation.ok(Messages.NxiqConfiguration_ConnectionSucceeded(applicationsResp.applications.size))
      }
      catch (Exception e)
      {
        return FormValidation.error(e, Messages.NxiqConfiguration_ConnectionFailed())
      }

    }

  }
}
