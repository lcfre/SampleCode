package com.wafer.dailyreport;

/*
 * ====================================================================
 * Copyright (c) 2004-2015 Wafer Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.
 * ====================================================================
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/*
 * The following example program demonstrates how you can use SVNRepository to
 * obtain a history for a range of revisions including (for each revision): all
 * changed paths, log message, the author of the commit, the timestamp when the
 * commit was made. It is similar to the "svn log" command supported by the
 * Subversion client library.
 * 
 * As an example here's a part of one of the program layouts (for the default
 * values):
 * 
 * --------------------------------------------- revision: 1240 author: alex
 * date: Tue Aug 02 19:52:49 NOVST 2005 log message: 0.9.0 is now trunk
 * 
 * changed paths: A /trunk (from /branches/0.9.0 revision 1239)
 * --------------------------------------------- revision: 1263 author: sa date:
 * Wed Aug 03 21:19:55 NOVST 2005 log message: updated examples, javadoc files
 * 
 * changed paths: M /trunk/doc/javadoc-files/javadoc.css M
 * /trunk/doc/javadoc-files/overview.html M
 * /trunk/doc/examples/src/org/tmatesoft/svn/examples/wc/StatusHandler.java ...
 * 
 */
public class SVNHistory {
  /**
   * 
   */
  static Logger log = Logger.getLogger(Class.class);

  /**
   * 
   */
  ConfigurationUtil config = ConfigurationUtil.getConfigurationInstance();

  /**
   * 
   */
  private final SimpleDateFormat format = new SimpleDateFormat(config.getNodeValue(new String[] {"dateTimeFormat"}));

  /**
   * 
   * @return
   */
  public HashMap<String, Collection<SVNLogEntry>> getAllDailyHistory() {
    HashMap<String, Collection<SVNLogEntry>> all = new HashMap<String, Collection<SVNLogEntry>>();
    List<String> reposNameList = config.getReposNameList();
    for (Iterator<String> iter = reposNameList.iterator(); iter.hasNext(); ) {
      String reposName = iter.next();
      Collection<SVNLogEntry> collection = this.setDatabaseDailyHistory(reposName);
      all.put(reposName, collection);
    }
    return all;
  }

  /*
   * args parameter is used to obtain a repository location URL, a start
   * revision number, an end revision number, user's account name & password to
   * authenticate him to the server.
   */
  @SuppressWarnings("unchecked")
  public Collection<SVNLogEntry> setDatabaseDailyHistory(String database) {
    /*
     * Default values:
     */
    String url = config.getNodeValue(new String[] {"svn", "url"}) + database;
    String name = config.getNodeValue(new String[] {"svn", "username"});
    String password = config.getNodeValue(new String[] {"svn", "password"});
    String tl = config.getNodeValue(new String[] {"svn", "timeLength"});
    long startRevision = 0;
    long endRevision = -1;// HEAD (the latest) revision
    int timeLength = -1;
    try {
      timeLength = Integer.parseInt(tl);
    } catch(Exception e) {
      log.error("get svn time length error:" + e.getMessage());
    }
    /*
     * Initializes the library (it must be done before ever using the library
     * itself)
     */
    setupLibrary();

    SVNRepository repository = null;

    try {
      /*
       * Creates an instance of SVNRepository to work with the repository. All
       * user's requests to the repository are relative to the repository
       * location used to create this SVNRepository. SVNURL is a wrapper for URL
       * strings that refer to repository locations.
       */
      repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
    } catch (SVNException svne) {
      /*
       * Perhaps a malformed URL is the cause of this exception.
       */
      System.err
          .println("error while creating an SVNRepository for the location '"
              + url + "': " + svne.getMessage());
      System.exit(1);
    }

    /*
     * User's authentication information (name/password) is provided via an
     * ISVNAuthenticationManager instance. SVNWCUtil creates a default
     * authentication manager given user's name and password.
     * 
     * Default authentication manager first attempts to use provided user name
     * and password and then falls back to the credentials stored in the default
     * Subversion credentials storage that is located in Subversion
     * configuration area. If you'd like to use provided user name and password
     * only you may use BasicAuthenticationManager class instead of default
     * authentication manager:
     * 
     * authManager = new BasicAuthenticationsManager(userName, userPassword);
     * 
     * You may also skip this point - anonymous access will be used.
     */
    ISVNAuthenticationManager authManager = SVNWCUtil
        .createDefaultAuthenticationManager(name, password);
    repository.setAuthenticationManager(authManager);

    /*
     * Gets the latest revision number of the repository
     */
    try {
      endRevision = repository.getLatestRevision();
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DATE, timeLength);
      Date date = cal.getTime();
      startRevision = repository.getDatedRevision(date);
      //log.debug("start revision:" + startRevision + "/start day:" + date);
      //log.debug("end revision:" + endRevision);
    } catch (SVNException svne) {
      log.error("error while fetching the latest repository revision: "
              + svne.getMessage());
      System.exit(1);
    } catch (Exception e) {
      e.printStackTrace();
    }

    Collection<SVNLogEntry> logEntries = null;
    try {
      /*
       * Collects SVNLogEntry objects for all revisions in the range defined by
       * its start and end points [startRevision, endRevision]. For each
       * revision commit information is represented by SVNLogEntry.
       * 
       * the 1st parameter (targetPaths - an array of path strings) is set when
       * restricting the [startRevision, endRevision] range to only those
       * revisions when the paths in targetPaths were changed.
       * 
       * the 2nd parameter if non-null - is a user's Collection that will be
       * filled up with found SVNLogEntry objects; it's just another way to
       * reach the scope.
       * 
       * startRevision, endRevision - to define a range of revisions you are
       * interested in; by default in this program - startRevision=0,
       * endRevision= the latest (HEAD) revision of the repository.
       * 
       * the 5th parameter - a boolean flag changedPath - if true then for each
       * revision a corresponding SVNLogEntry will contain a map of all paths
       * which were changed in that revision.
       * 
       * the 6th parameter - a boolean flag strictNode - if false and a changed
       * path is a copy (branch) of an existing one in the repository then the
       * history for its origin will be traversed; it means the history of
       * changes of the target URL (and all that there's in that URL) will
       * include the history of the origin path(s). Otherwise if strictNode is
       * true then the origin path history won't be included.
       * 
       * The return value is a Collection filled up with SVNLogEntry Objects.
       */
      logEntries = repository.log(new String[] { "" }, null, startRevision,
          endRevision, true, true);
      return logEntries;
    } catch (SVNException svne) {
      System.out.println("error while collecting log information for '" + url
          + "': " + svne.getMessage());
      System.exit(1);
    }
    return null;
  }

  /*
   * Initializes the library to work with a repository via different protocols.
   */
  private static void setupLibrary() {
    /*
     * For using over http:// and https://
     */
    DAVRepositoryFactory.setup();
    /*
     * For using over svn:// and svn+xxx://
     */
    SVNRepositoryFactoryImpl.setup();

    /*
     * For using over file:///
     */
    FSRepositoryFactory.setup();
  }

  /**
   * 
   * @return
   */
  public HashMap<String, String> getUserRecords() {
    HashMap<String, String> userRecord = new HashMap<String, String>();
    HashMap<String, Collection<SVNLogEntry>> map = getAllDailyHistory();
    Set<String> set = map.keySet();
    for (Iterator<String> iter = set.iterator(); iter.hasNext();) {
      Object key = iter.next();
      Collection<SVNLogEntry> logEntries = (Collection<SVNLogEntry>) map
          .get(key);

      String reposTitle = "Repos:" + key + "\n";
      for (Iterator<SVNLogEntry> entries = logEntries.iterator(); entries
          .hasNext();) {
        StringBuffer buffer = new StringBuffer();

        /*
         * gets a next SVNLogEntry
         */
        SVNLogEntry logEntry = (SVNLogEntry) entries.next();

        /*
         * gets the revision number
         */
        buffer.append("Revision: " + logEntry.getRevision() + "\n");
        /*
         * gets the author of the changes made in that revision
         */
        // System.out.println("author: " + logEntry.getAuthor());
        /*
         * gets the time moment when the changes were committed
         */
        buffer.append("Date: " + format.format(logEntry.getDate()) + "\n");
        /*
         * gets the commit log message
         */
        buffer.append("Message: " + logEntry.getMessage() + "\n\n");
        /*
         * displaying all paths that were changed in that revision; cahnged path
         * information is represented by SVNLogEntryPath.
         */
        if (logEntry.getChangedPaths().size() > 0) {
          buffer.append("changed paths:\n");

          // * keys are changed paths

          Set<?> changedPathsSet = logEntry.getChangedPaths().keySet();

          for (Iterator<?> changedPaths = changedPathsSet.iterator(); changedPaths
              .hasNext();) {

            // * obtains a next SVNLogEntryPath

            SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry
                .getChangedPaths().get(changedPaths.next());
            /*
             * SVNLogEntryPath.getPath returns the changed path itself;
             * 
             * SVNLogEntryPath.getType returns a charecter describing how the
             * path was changed ('A' - added, 'D' - deleted or 'M' - modified);
             * 
             * If the path was copied from another one (branched) then
             * SVNLogEntryPath.getCopyPath & SVNLogEntryPath.getCopyRevision
             * tells where it was copied from and what revision the origin path
             * was at.
             */
            buffer.append(" "
                + entryPath.getType()
                + " "
                + entryPath.getPath()
                + ((entryPath.getCopyPath() != null) ? " (from "
                    + entryPath.getCopyPath() + " revision "
                    + entryPath.getCopyRevision() + ")" : "\n"));
          }
          buffer.append("...............\n");
        }
        String author = logEntry.getAuthor();
        if (userRecord.containsKey(author)) {
          String newContent = (String) userRecord.get(author);
          String append = new String(buffer);
          if (newContent.indexOf("repos:" + key) < 0) {
            append = reposTitle + append;
          }
          userRecord.put(author, newContent + append);
        } else {

          userRecord.put(author, reposTitle + new String(buffer));
        }
      }
    }
    return userRecord;
  }

  /**
   * 
   * @param args
   */
  public static void main(String[] args) {
    SVNHistory history = new SVNHistory();
    HashMap<String, String> map = history.getUserRecords();
    Set<String> set = map.keySet();
    for (Iterator<String> iter = set.iterator(); iter.hasNext();) {
      Object key = iter.next();
      String content = (String) map.get(key);
      log.debug(key);
      log.debug("///////////////////////////////////////////////");
      log.debug(content);
    }
  }
}
