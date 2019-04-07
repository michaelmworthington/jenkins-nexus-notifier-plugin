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
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * Jenkins Ref for Dynamic Field Lists:
 *     https://reports.jenkins.io/core-taglib/jelly-taglib-ref.html
 *     https://javadoc.jenkins-ci.org/lib/FormTagLib.html
 */
public class JiraCustomFieldMappings
        extends AbstractDescribableImpl<JiraCustomFieldMappings>
{
  public String customFieldName;
  public String customFieldValue;
  public String customFieldId;
  public String customFieldType;

  @DataBoundConstructor
  public JiraCustomFieldMappings(final String customFieldName,
                                 final String customFieldValue)
  {
    this.customFieldName = customFieldName;
    this.customFieldValue = customFieldValue;
  }

  @Extension
  public static final class DescriptorImpl
          extends Descriptor<JiraCustomFieldMappings>
  {
    @Override
    public String getDisplayName()
    {
      return "Jira Custom Field Mapping";
    }

  }
}
