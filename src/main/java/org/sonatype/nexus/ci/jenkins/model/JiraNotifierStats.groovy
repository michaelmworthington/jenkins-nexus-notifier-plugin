package org.sonatype.nexus.ci.jenkins.model

import groovy.time.TimeCategory
import groovy.time.TimeDuration

class JiraNotifierStats
{
  PrintStream logger
  def ticketStatusLogFrequency

  def currentApplicationIndex
  def totalApplications = 1

  def currentTicketIndex
  def totalApplicationNewJiraTickets
  def applicationNewJiraTickets = []

  def jobStartTime = new Date()
  def applicationStartTime
  def applicationElapsedTimes = []

  JiraNotifierStats(PrintStream logger = null, def ticketStatusLogFrequency = 25)
  {
    //start at minus one since i'll increment it when starting the first application
    this.currentApplicationIndex = -1
    this.logger = logger
    this.ticketStatusLogFrequency = ticketStatusLogFrequency
  }

  /*
   * *********************************************************************************
   *         PRETTY PRINT
   * *********************************************************************************
   */
  def cmMessage = ""

  void printRunningApplicationMessage()
  {
    logger.println("Running ${cmMessage}Application: ${getMessageApplicationCounter()}")
    logger.println("Creating ticket: ${getMessageTicketCounter()}")
  }

  void printFinishedApplicationMessage()
  {
    logger.println("Finished ${cmMessage}Application: ${getMessageApplicationCounter()}")
  }

  void printFinishedApplicationNewJiraTickets()
  {
    def i = currentApplicationIndex
    def art = applicationElapsedTimes[i].toMilliseconds() / applicationNewJiraTickets[i]
    logger.println("Created ${applicationNewJiraTickets[i]} Jira tickets for this application (avg time per ticket: ${art} milliseconds vs avg time per ticket for all apps: ${getAverageTicketElapsedMilliseconds()} milliseconds)")
  }

  void printGrandTotals(boolean cmDone = false)
  {
    if(!cmMessage || cmDone)
    {
      //make it human friendly
      def actualAppCount = currentApplicationIndex + 1
      def jobTotalTimeMillis = applicationElapsedTimes.sum()
      def appAverageSeconds = getAverageApplicationElapsedSeconds()

      def actualTicketCount = applicationNewJiraTickets.sum()
      def ticketsPerApp = actualTicketCount / actualAppCount
      def ticketAverageMillis = getAverageTicketElapsedMilliseconds()

      logger.println("Finished ${actualAppCount} ${cmMessage}Applications")
      logger.println("    * Total Time: ${jobTotalTimeMillis}")
      logger.println("    * Average Seconds per App: ${appAverageSeconds}")
      logger.println("Created a total of ${actualTicketCount} tickets")
      logger.println("    * Average Tickets per App: ${ticketsPerApp}")
      logger.println("    * Average Millis per Ticket: ${ticketAverageMillis}")
    }
  }


  /*
   * *********************************************************************************
   *         HELPERS
   * *********************************************************************************
   */

  def getAverageApplicationElapsedSeconds()
  {
    if (applicationElapsedTimes.size() > 0)
    {
      return applicationElapsedTimes.sum().toMilliseconds() / applicationElapsedTimes.size() / 1000
    }
    else
    {
      return "average not yet available"
    }
  }

  def getAverageTicketElapsedMillisecondsForCurrentlyRunningApp()
  {
    if (currentTicketIndex > 0)
    {
      return getElapsedTime(applicationStartTime).toMilliseconds() / currentTicketIndex
    }
    else
    {
      return "average not yet available"
    }
  }

  def getAverageTicketElapsedMilliseconds()
  {
    if (applicationNewJiraTickets.sum() > 0)
    {
      return applicationElapsedTimes.sum().toMilliseconds() / applicationNewJiraTickets.sum()
    }
    else
    {
      return "average not yet available"
    }
  }

  String getMessageApplicationCounter()
  {
    //add one to make it human friendly
    return "[App ${currentApplicationIndex + 1} of ${totalApplications} (app et: ${getElapsedTime(applicationStartTime)} | app avg et: ${getAverageApplicationElapsedSeconds()} seconds | job et: ${getElapsedTime(jobStartTime)})]"
  }

  String getMessageTicketCounter()
  {
    //add one to make it human friendly
    return "[Ticket ${currentTicketIndex + 1} of ${totalApplicationNewJiraTickets} (app ticket avg et: ${getAverageTicketElapsedMillisecondsForCurrentlyRunningApp()} milliseconds | all tickets avg et: ${getAverageTicketElapsedMilliseconds()} milliseconds)]"
  }

  /*
   * *********************************************************************************
   *         ACTIONS
   * *********************************************************************************
   */

  void startApplication()
  {
    applicationStartTime = new Date()
    currentApplicationIndex++
    currentTicketIndex = -1
    totalApplicationNewJiraTickets = 0
  }

  void finishApplication()
  {
    applicationElapsedTimes.add(getElapsedTime(applicationStartTime))

    //make it human friendly
    applicationNewJiraTickets.add(currentTicketIndex + 1)
  }

  void startTicket()
  {
    currentTicketIndex++
    if (currentTicketIndex % ticketStatusLogFrequency == 0)
    {
      logger.println("######################################")
      printRunningApplicationMessage()
      logger.println("######################################")
    }
  }

  void finishTicket()
  {
  }

  /*
   * *********************************************************************************
   *         OTHER HELPERS
   * *********************************************************************************
   */

  static TimeDuration getElapsedTime(Date pStartDate)
  {
    return TimeCategory.minus(new Date(), pStartDate)
  }

  def getApplicationElapsedTimes()
  {
    return applicationElapsedTimes
  }

}
