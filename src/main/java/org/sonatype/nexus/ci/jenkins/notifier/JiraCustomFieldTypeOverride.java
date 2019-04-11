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

public class JiraCustomFieldTypeOverride
        extends AbstractDescribableImpl<JiraCustomFieldTypeOverride>
{
  public String originalTypeName;
  public String overrideWithThisTypeName;

  @DataBoundConstructor
  public JiraCustomFieldTypeOverride(final String originalTypeName,
                                     final String overrideWithThisTypeName)
  {
    this.originalTypeName = originalTypeName;
    this.overrideWithThisTypeName = overrideWithThisTypeName;
  }

  @Extension
  public static final class DescriptorImpl
          extends Descriptor<JiraCustomFieldTypeOverride>
  {
    @Override
    public String getDisplayName()
    {
      return "Jira Custom Field Type Override Mapping";
    }

  }
}
