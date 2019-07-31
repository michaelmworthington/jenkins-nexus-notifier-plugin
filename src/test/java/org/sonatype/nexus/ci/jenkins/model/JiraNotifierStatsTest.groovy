package org.sonatype.nexus.ci.jenkins.model

import spock.lang.Specification

class JiraNotifierStatsTest
        extends Specification
{
  def ONE_SECOND_IN_MILLIS = 1000

  def 'print elapsed time'()
  {
    setup:
    JiraNotifierStats stats = new JiraNotifierStats()

    sleep(ONE_SECOND_IN_MILLIS * 5)

    def res = stats.getElapsedTime(new Date())
    println res

    expect:
    res != null
  }

  def 'print stats'()
  {
    setup:
    JiraNotifierStats stats = new JiraNotifierStats(System.out, 1)
    stats.totalApplications = 2

    stats.startApplication()
    stats.totalApplicationNewJiraTickets = 2
    stats.startTicket()
    sleep(ONE_SECOND_IN_MILLIS * 1)
    stats.finishTicket()
    println "Status: ${stats.getMessageApplicationCounter()}"
    stats.startTicket()
    sleep(ONE_SECOND_IN_MILLIS * 2)
    stats.finishTicket()
    stats.finishApplication()

    println "######"
    stats.printFinishedApplicationMessage()
    stats.printFinishedApplicationNewJiraTickets()
    println "######"

    stats.startApplication()
    stats.totalApplicationNewJiraTickets = 5
    stats.startTicket()
    sleep(ONE_SECOND_IN_MILLIS * 1)
    stats.finishTicket()
    stats.startTicket()
    sleep(ONE_SECOND_IN_MILLIS * 2)
    stats.finishTicket()
    println "Status: ${stats.getMessageApplicationCounter()}"
    stats.startTicket()
    sleep(ONE_SECOND_IN_MILLIS * 10)
    stats.finishTicket()
    stats.startTicket()
    sleep(ONE_SECOND_IN_MILLIS * 1)
    stats.finishTicket()
    println "Status: ${stats.getMessageApplicationCounter()}"
    stats.startTicket()
    sleep(ONE_SECOND_IN_MILLIS * 1)
    stats.finishTicket()
    stats.finishApplication()

    println "######"
    stats.printFinishedApplicationMessage()
    stats.printFinishedApplicationNewJiraTickets()
    println "######"

    println "Application Elapsed Times:"
    stats.getApplicationElapsedTimes().each{
      println it
    }

    stats.printGrandTotals()

    expect:
    stats != null
  }
}
