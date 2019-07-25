package org.sonatype.nexus.ci.jenkins.notifier.ContinuousMonitoringConfig

import org.sonatype.nexus.ci.jenkins.notifier.ContinuousMonitoringConfig

def f = namespace(lib.FormTagLib)

def typedDescriptor = (ContinuousMonitoringConfig.DescriptorImpl) descriptor

f.section(title: typedDescriptor.displayName) {

  f.entry(title: _('Should run as Continuous Monitoring'), field: 'shouldRunWithContinuousMonitoring') {
    f.checkbox()
  }

  f.entry(title: _('Application Name Key Field in Dynamic Data'), field: 'dynamicDataApplicationKey') {
    f.textbox()
  }

  f.entry(title: _('Stage Key Field in Dynamic Data'), field: 'dynamicDataStageKey') {
    f.textbox()
  }

  f.entry(title: _('Application Name'), field: 'applicationName') {
    f.textbox()
  }

  f.entry(title: _("Report Stage"), field: 'stage') {
    f.textbox()
  }

  f.entry(title: _('Should Update Last Scan Date'), field: 'shouldUpdateLastScanDate') {
    f.checkbox()
  }

}
