package org.sonatype.nexus.ci.jenkins.notifier.JiraCustomFieldMappings

import org.sonatype.nexus.ci.jenkins.notifier.JiraCustomFieldMappings

def f = namespace(lib.FormTagLib)
def c = namespace(lib.CredentialsTagLib)

def typedDescriptor = (JiraCustomFieldMappings.DescriptorImpl) descriptor

f.section(title: typedDescriptor.displayName) {
  f.entry(title: _("Custom Field Name"), field: 'customFieldName') {
    f.textbox(clazz: 'required')
  }

  f.entry(title: _("Custom Field Value"), field: 'customFieldValue') {
    f.textbox(clazz: 'required')
  }
}