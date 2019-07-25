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

import java.io.PrintStream;
import java.util.Optional;

import javax.annotation.Nonnull;

import hudson.tasks.BuildStepMonitor;
import org.sonatype.nexus.ci.jenkins.bitbucket.BitbucketNotifier;
import org.sonatype.nexus.ci.jenkins.jira.JiraNotifier;
import org.sonatype.nexus.ci.jenkins.model.PolicyEvaluationHealthAction;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class NotifierStep
    extends Notifier
    implements SimpleBuildStep
{
  private String dynamicData;
  private ContinuousMonitoringConfig continuousMonitoringConfig;
  private BitbucketNotification bitbucketNotification;
  private JiraNotification jiraNotification;

  @DataBoundSetter
  public void setDynamicData(final String dynamicData) { this.dynamicData = dynamicData; }

  public String getDynamicData() { return dynamicData; }

  @DataBoundSetter
  public void setContinuousMonitoringConfig(final ContinuousMonitoringConfig continuousMonitoringConfig) {
    this.continuousMonitoringConfig = continuousMonitoringConfig;
  }

  public ContinuousMonitoringConfig getContinuousMonitoringConfig() {
    return continuousMonitoringConfig;
  }

  @DataBoundSetter
  public void setBitbucketNotification(final BitbucketNotification bitbucketNotification) {
    this.bitbucketNotification = bitbucketNotification;
  }

  public BitbucketNotification getBitbucketNotification() {
    return bitbucketNotification;
  }

  @DataBoundSetter
  public void setJiraNotification(final JiraNotification jiraNotification) {
    this.jiraNotification = jiraNotification;
  }

  public JiraNotification getJiraNotification() {
    return jiraNotification;
  }

  @DataBoundConstructor
  public NotifierStep() {}

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  @Override
  public void perform(@Nonnull final Run run,
               @Nonnull final FilePath workspace,
               @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws AbortException
  {
    if (continuousMonitoringConfig != null && continuousMonitoringConfig.getShouldRunWithContinuousMonitoring())
    {
      if (jiraNotification != null && jiraNotification.getSendJiraNotification())
      {
        //todo: form validation here or in ContinuousMonitoringConfig.java????
        new JiraNotifier(run, listener).continuousMonitor(dynamicData, continuousMonitoringConfig, jiraNotification);
      }
    }
    else
    {
      PrintStream logger = listener.getLogger();
      Optional<? extends Action> optionalPolicyEvaluation = run.getAllActions().stream()
          .filter(PolicyEvaluationHealthAction::assignableFrom).findFirst();

      if (!optionalPolicyEvaluation.isPresent()) {
        logger.println(Messages.NotifierStep_NoPolicyAction());
        throw new AbortException(Messages.NotifierStep_NoPolicyAction());
      }

      boolean buildPassing = run.getResult() == Result.SUCCESS;

      PolicyEvaluationHealthAction policyEvaluationHealthAction = PolicyEvaluationHealthAction
              .build(optionalPolicyEvaluation.get());

      if (bitbucketNotification != null && bitbucketNotification.getSendBitbucketNotification()) {
        new BitbucketNotifier(run, listener).send(buildPassing, bitbucketNotification, policyEvaluationHealthAction);
      }

      if (jiraNotification != null && jiraNotification.getSendJiraNotification()) {
        new JiraNotifier(run, listener).send(buildPassing, dynamicData, jiraNotification, policyEvaluationHealthAction);
      }
    }
  }

  @Extension
  @Symbol("nexusPolicyResultNotifier")
  public static final class DescriptorImpl
      extends BuildStepDescriptor<Publisher>
  {
    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return Messages.NotifierStep_DisplayName();
    }
  }
}
