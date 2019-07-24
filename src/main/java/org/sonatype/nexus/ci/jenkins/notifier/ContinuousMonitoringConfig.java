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
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * Jenkins Ref for Dynamic Field Lists:
 *     https://reports.jenkins.io/core-taglib/jelly-taglib-ref.html
 *     https://javadoc.jenkins-ci.org/lib/FormTagLib.html
 *     https://www.cloudbees.com/blog/introducing-variability-jenkins-plugins
 *     https://wiki.jenkins.io/display/JENKINS/Create+a+new+Plugin+with+a+custom+build+Step
 */
public class ContinuousMonitoringConfig
        extends AbstractDescribableImpl<ContinuousMonitoringConfig>
{
  private boolean shouldRunWithContinuousMonitoring;
  private String dynamicDataApplicationKey;
  private String dynamicDataStageKey;
  private String applicationName;
  private String stage;

  public boolean getShouldRunWithContinuousMonitoring()
  {
    return shouldRunWithContinuousMonitoring;
  }

  @DataBoundSetter
  public void setShouldRunWithContinuousMonitoring(boolean shouldRunWithContinuousMonitoring)
  {
    this.shouldRunWithContinuousMonitoring = shouldRunWithContinuousMonitoring;
  }

  public String getDynamicDataApplicationKey()
  {
    return dynamicDataApplicationKey;
  }

  @DataBoundSetter
  public void setDynamicDataApplicationKey(String dynamicDataApplicationKey)
  {
    this.dynamicDataApplicationKey = dynamicDataApplicationKey;
  }

  public String getApplicationName()
  {
    return applicationName;
  }

  public String getDynamicDataStageKey()
  {
    return dynamicDataStageKey;
  }

  @DataBoundSetter
  public void setDynamicDataStageKey(String dynamicDataStageKey)
  {
    this.dynamicDataStageKey = dynamicDataStageKey;
  }

  @DataBoundSetter
  public void setApplicationName(String applicationName)
  {
    this.applicationName = applicationName;
  }

  public String getStage()
  {
    return stage;
  }

  @DataBoundSetter
  public void setStage(String stage)
  {
    this.stage = stage;
  }

  @DataBoundConstructor
  public ContinuousMonitoringConfig() { }

  @Extension
  @Symbol("nexusContinuousMonitoringConfig")
  public static final class DescriptorImpl
          extends Descriptor<ContinuousMonitoringConfig>
  {
    @Override
    public String getDisplayName()
    {
      return "Continuous Monitoring Config";
    }

  }
}
