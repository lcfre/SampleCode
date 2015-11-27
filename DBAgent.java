package com.wafer.dailyreport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * 
 * @author eric
 * 
 */
public class DBAgent {
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
  public DBAgent() {
  }

  /**
   * 
   * @param begin
   * @param end
   */
  public HashMap<String, ReportGroup> queryReportData(String begin, String end) {
    /**
     * 
     */
    RootUtil results = new RootUtil();

    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      conn = config.getDatabase("portal");
      stmt = conn.createStatement();
      rs = stmt.executeQuery(appendDate(begin, end));
      while (rs.next()) {
        String groupName = rs.getString("workreport_branch_name");
        String recordDay = rs.getString("workreport_date");
        String writer = rs.getString("workreport_author_name");
        String planName = rs.getString("workreport_planname");
        String description = rs.getString("workreport_description");
        String workType = rs.getString("workreport_tasktype_name");
        String workStatus = rs.getString("workreport_percent_value");
        String reportType = rs.getString("workreport_remarktype_name");
        String productName = rs.getString("workreport_productiontype_name");
        String dayWorkTime = rs.getString("workreport_dayworktime");
        String checkPercent = rs.getString("workreport_checkpercent");
        String checkWord = rs.getString("workreport_checkword");
        String checkPerson = rs.getString("workreport_checkperson");
        String author = rs.getString("workreport_author");
        String projectName = rs.getString("workreport_projectname");

        ReportItem report = new ReportItem(groupName, writer, recordDay,
            planName, description, workType, workStatus, reportType,
            productName, dayWorkTime, checkPercent, checkWord, checkPerson,
            author, projectName);
        results.addReport(report);
      }
    } catch (Exception e) {
      System.out.println("Query data exception:" + e.getMessage());
      e.printStackTrace();
    }
    finally {
      try {
        rs.close();
      } catch (SQLException e) {
        System.out.println("Close resultset exception:" + e.getMessage());
      }
      try {
        stmt.close();
      } catch (SQLException e) {
        System.out.println("Close statement exception:" + e.getMessage());
      }
      try {
        conn.close();
      } catch (SQLException e) {
        System.out.println("Close connection exception:" + e.getMessage());
      }
    }
    return results.getMaps();
  }

  /**
   * 
   * @param begin
   * @param end
   * @return
   */
  private String appendDate(String begin, String end) {
    String sql = config.getSQLString("reportSQL");
    if (begin != null) {
      sql += " where workreport_date >= '" + begin + "'";
    }
    if (end != null) {
      sql += " and workreport_date <= '" + end + "'";
    }
    sql += " order by workreport_date desc";
    return sql;
  }

  /**
   * 
   * @return
   */
  public HashMap<String, BugStat> statBugInfo() {
    HashMap<String, BugStat> map = new HashMap<String, BugStat>();
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      conn = config.getDatabase("bugfree");
      stmt = conn.createStatement();
      rs = stmt.executeQuery(config.getSQLString("bugStatSQL"));
      while (rs.next()) {
        String openBy = rs.getString("OpenedBy");
        String resolution = rs.getString("Resolution");
        if (resolution == null || resolution.length() == 0) {
          resolution = "Active";
        }
        int countResolution = rs.getInt("CountResolution");
        if (map.containsKey(openBy)) {
          ((BugStat) map.get(openBy)).addItem(resolution, countResolution);
        } else {
          BugStat stat = new BugStat(openBy);
          stat.addItem(resolution, countResolution);
          map.put(openBy, stat);
        }
      }
    } catch (Exception e) {
      System.out.println("Query data exception:" + e.getMessage());
    }
    finally {
      try {
        rs.close();
      } catch (SQLException e) {
        System.out.println("Close resultset exception:" + e.getMessage());
      }
      try {
        stmt.close();
      } catch (SQLException e) {
        System.out.println("Close statement exception:" + e.getMessage());
      }
      try {
        conn.close();
      } catch (SQLException e) {
        System.out.println("Close connection exception:" + e.getMessage());
      }
    }
    return map;
  }

  /**
   * 
   * @return
   */
  public final Vector<String> getManagerIds() {
    Vector<String> managerIds = new Vector<String>();
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      conn = config.getDatabase("portal");
      stmt = conn.createStatement();
      rs = stmt.executeQuery(config.getSQLString("adminSQL"));
      while (rs.next()) {
        String userId = rs.getString("user_name");
        managerIds.add(userId);
      }
    } catch (Exception e) {
      System.out.println("Query data exception:" + e.getMessage());
      e.printStackTrace();
    }
    finally {
      try {
        rs.close();
      } catch (SQLException e) {
        System.out.println("Close resultset exception:" + e.getMessage());
      }
      try {
        stmt.close();
      } catch (SQLException e) {
        System.out.println("Close statement exception:" + e.getMessage());
      }
      try {
        conn.close();
      } catch (SQLException e) {
        System.out.println("Close connection exception:" + e.getMessage());
      }
    }
    return managerIds;
  }

  /**
   * 
   * @param args
   */
  public static void main(String args[]) {
    DBAgent agent = new DBAgent();
    HashMap<String, BugStat> map = agent.statBugInfo();
    System.out.println(map);
  }
}
