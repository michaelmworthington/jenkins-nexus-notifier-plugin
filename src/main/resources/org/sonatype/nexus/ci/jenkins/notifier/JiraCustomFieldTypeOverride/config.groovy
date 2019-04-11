package org.sonatype.nexus.ci.jenkins.notifier.JiraCustomFieldTypeOverride

import org.sonatype.nexus.ci.jenkins.notifier.JiraCustomFieldTypeOverride

def f = namespace(lib.FormTagLib)

def typedDescriptor = (JiraCustomFieldTypeOverride.DescriptorImpl) descriptor

f.section(title: typedDescriptor.displayName) {
  f.entry(title: _("Original Type Name"), field: 'originalTypeName') {
    f.textbox(clazz: 'required')
  }

  f.entry(title: _("Override With This Type Name"), field: 'overrideWithThisTypeName') {
    f.textbox(clazz: 'required')
  }
}
