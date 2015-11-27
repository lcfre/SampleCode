package com.wafersystems.energywise;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.adventnet.snmp.snmp2.SnmpAPI;
import com.adventnet.snmp.snmp2.SnmpException;
import com.adventnet.snmp.snmp2.SnmpNull;
import com.adventnet.snmp.snmp2.SnmpOID;
import com.adventnet.snmp.snmp2.SnmpPDU;
import com.adventnet.snmp.snmp2.SnmpSession;
import com.adventnet.snmp.snmp2.SnmpVar;
import com.adventnet.snmp.snmp2.SnmpVarBind;
import com.adventnet.snmp.snmp2.UDPProtocolOptions;

/**
 * <br>
 * <p>Description: snmp collection</p>
 * <p>Date: 2005-11-17</p>
 * @author heliang
 * @version 1.0  initial
 */
public class AdventNetSnmp {
  private static Log _log = LogFactory.getLog(AdventNetSnmp.class);
  private static ThreadLocal<AdventNetSnmp> _singleton = new ThreadLocal<AdventNetSnmp>();
  private SnmpAPI api = null;
  private SnmpSession session = null;
  private boolean hasOpened = false;

  public static AdventNetSnmp getInstance() {
    if(_singleton.get()==null){
      _singleton.set(new AdventNetSnmp());
      _singleton.get().open();
    }
    return _singleton.get();
  }

  private AdventNetSnmp() {
  }

  public void open() {
    if(hasOpened) return;
    api = new SnmpAPI();
    //api.setDaemon(true);
    session = new SnmpSession(api);
    session.setDaemon(true);
    session.setVersion(SnmpAPI.SNMP_VERSION_2C);
    session.setRetries(2);
    session.setTimeout(2000);
    try {
      session.open();
      hasOpened = true;
    } catch(SnmpException e) {
      throw new RuntimeException("Open snmp session error. " + e.getMessage());
    }
  }

  public void close() {
    if(hasOpened){
      api.close();
      session.close();
    }
    api = null;
    session = null;
  }

  public String synGet(String agentAddr, String readCommunity, String oid) {
    String[][] ret = synGet(agentAddr, readCommunity, new String[] {oid});
    return ret==null?null:ret[0][1];
  }

  public String[][] synGet(String agentAddr, String readCommunity,
                           String[] oids) {
    return synGet(agentAddr, 161, readCommunity, oids);
  }

  /**
   * ͬ��GET����
   * @param agentAddr
   * @param port
   * @param readCommunity
   * @param oids
   * @return
   */
  public String[][] synGet(String agentAddr, int port,
                           String readCommunity, String[] oids) {
    SnmpPDU response = syn(SnmpAPI.GET_REQ_MSG, agentAddr, port,
                           readCommunity, oids, (String[])null, (String[])null);
    String[][] ret = null;
    List<String[]> v = graspResult(response);
    if(v!=null && v.size()>0) ret = (String[][])v.toArray(new String[0][0]);
    return ret;
  }

  // snmpv2 GetBulk
  public String[][] synGetBulk(String agentAddr,String readCommunity, String oid) {
    return synGetBulk(agentAddr,161,readCommunity,new String[]{oid});
  }
  
  // snmpv2 GetBulk
  public String[][] synGetBulk(String agentAddr,String readCommunity, String[] oids) {
    return synGetBulk(agentAddr,161,readCommunity,oids);
  }
  
  //snmpv2 GetBulk
  public String[][] synGetBulk(String agentAddr, int port,
      String readCommunity, String[] oids) {
    SnmpPDU response = syn(SnmpAPI.GETBULK_REQ_MSG, agentAddr, port,
                           readCommunity, oids, (String[])null, (String[])null);
    String[][] ret = null;
    List<String[]> v = graspResult(response);
    if(v!=null && v.size()>0){
    	filterGetBulkResult(v, oids,false);
        ret = (String[][])v.toArray(new String[0][0]);
    }
    return ret;
  }
  
  private void filterGetBulkResult(List<String[]> v, String[] oids, boolean alignResult) {
	  boolean stop = false;
	  int len = oids.length;
	  int i = 0;
	  for (Iterator<String[]> itor = v.iterator(); itor.hasNext(); i++) {
		  String[] row = itor.next();
		  if (stop) {
			  itor.remove();
		  } else if (!row[0].startsWith(oids[i % len])) {
			  itor.remove();
			  stop = true;
		  }
	  }
	  
	  int cnt = v.size();
	  if(alignResult && cnt>0){
		  int alignCnt = len * (cnt / len);
		  for(int j=alignCnt;j<cnt;j++){
		    v.remove(j);
	      }
	  }
  }
  
  /**
   * ��ȡ�����ݣ�Ҫ��OID��ͬһ�����
   * @param agentAddr
   * @param port
   * @param readCommunity
   * @param baseOids
   * @return
   */
  public String[][] synFetchTable(String agentAddr,int port,String readCommunity,String[] oids){
	    Vector<String[]> result = new Vector<String[]>();
	    boolean comeOut = false;
	    String[] startOids = new String[oids.length];
	    for(int i=0;i<oids.length;i++){
	    	startOids[i] = oids[i];
	    } 
	    while(!comeOut){
	    	SnmpPDU response = syn(SnmpAPI.GETBULK_REQ_MSG, agentAddr, port,
	    			readCommunity, startOids, (String[])null, (String[])null);
	    	List<String[]> v = graspResult(response);
	    	if(v!=null && v.size()>0){
	    		filterGetBulkResult(v, oids,true);
	    		
	    		if(v.size()>0){
	    		   result.addAll(v);
	    		   String[][] tmp = v.subList(v.size()-oids.length,v.size()).toArray(new String[0][0]);
	    		   for(int i=0;i<startOids.length;i++){
	    			   startOids[i] = tmp[i][0]; 
	    		   }
	    		}else{
	    		   comeOut = true;
	    		}
	    	}
	    }
		return (String[][])result.toArray(new String[0][0]); 
  }
  
  public String synGetNext(String agentAddr, String readCommunity, String oid) {
    String[][] ret = synGetNext(agentAddr, readCommunity, new String[] {oid});
    return ret==null?null:ret[0][1];
  }

  public String[][] synGetNext(String agentAddr, String readCommunity,
                               String[] oids) {
    return synGetNext(agentAddr, 161, readCommunity, oids);
  }

  /**
   * ͬ��GETNEXT����
   * @param agentAddr
   * @param port
   * @param readCommunity
   * @param oids
   * @return
   */
  public String[][] synGetNext(String agentAddr, int port,
                               String readCommunity, String[] oids) {
    SnmpPDU response = syn(SnmpAPI.GETNEXT_REQ_MSG, agentAddr, port,
                           readCommunity, oids, (String[])null, (String[])null);
    String[][] ret = null;
    List<String[]> v = graspResult(response);
    if(v!=null && v.size()>0) ret = (String[][])v.toArray(new String[0][0]);
    return ret;
  }

  public boolean synSet(String agentAddr, String writeCommunity,
                        String[] oids, String[] types, String[] values) {
    return synSet(agentAddr, 161, writeCommunity, oids, types, values);
  }

  /**
   * ͬ��SET����
   * @param agentAddr
   * @param port
   * @param writeCommunity
   * @param oids
   * @return boolean
   */
  public boolean synSet(String agentAddr, int port, String writeCommunity,
                        String[] oids, String[] types, String[] values) {
    SnmpPDU response = syn(SnmpAPI.SET_REQ_MSG, agentAddr, port,
                           writeCommunity, oids, types, values);
    return response != null && response.getErrstat() == 0;
  }

  private SnmpPDU syn(byte operationName, String agentAddr,
                      int port, String community,
                      String[] oids, String[] types, String[] values) {
    SnmpPDU pdu = new SnmpPDU();
    UDPProtocolOptions opt = new UDPProtocolOptions();
    opt.setRemoteHost(agentAddr.trim());
    opt.setRemotePort(port);
    pdu.setProtocolOptions(opt);
    pdu.setCommand(operationName);
    if(operationName == SnmpAPI.SET_REQ_MSG) {
      pdu.setWriteCommunity(community);
      addVariableBinding(pdu, oids, types, values);
    } else {
      pdu.setCommunity(community);
      addOidToPdu(pdu, oids);
    }
    if (operationName == SnmpAPI.GETBULK_REQ_MSG){
       //pdu.setNonRepeaters(65535);
       pdu.setMaxRepetitions(65535);
    }
    SnmpPDU response = doSynSend(pdu);
    return response;
  }

  private SnmpPDU doSynSend(SnmpPDU pdu) {
	 SnmpPDU response = null;
	 try {
		 response = session.syncSend(pdu);
	 } catch(SnmpException ex) {
		 _log.error("Send synchronous snmp PDU falied.\n" + ex.getMessage());
	 }
	 return response;
  }
  
  private void addVariableBinding(SnmpPDU pdu, String[] oids,
                                  String[] types, String[] values) {
    int fullMatchCnt = Math.min(oids.length, types.length);
    fullMatchCnt = Math.min(fullMatchCnt, values.length);
    SnmpVar var = null;
    for(int i = 0; i < fullMatchCnt; i++) {
      try {
        var = SnmpVar.createVariable(values[i], getVarType(types[i]));
      } catch(SnmpException e) {
        _log.error("Cannot create variable: " + oids[i] + " with value: " +
                   values[i], e);
      }
      pdu.addVariableBinding(new SnmpVarBind(new SnmpOID(oids[i]), var));
    }

  }

  private void addOidToPdu(SnmpPDU pdu, String[] oids) {
    for(int i = 0; i < oids.length; i++) {
      SnmpOID oid = new SnmpOID(oids[i]);
      if(oid != null) {
        pdu.addNull(oid);
      }
    }
  }

  private byte getVarType(String type) {
    byte dataType;
    if(type.equalsIgnoreCase("INTEGER")) {
      dataType = SnmpAPI.INTEGER;
    } else if(type.equalsIgnoreCase("STRING")) {
      dataType = SnmpAPI.STRING;
    } else if(type.equalsIgnoreCase("GAUGE")) {
      dataType = SnmpAPI.GAUGE;
    } else if(type.equalsIgnoreCase("TIMETICKS")) {
      dataType = SnmpAPI.TIMETICKS;
    } else if(type.equalsIgnoreCase("OPAQUE")) {
      dataType = SnmpAPI.OPAQUE;
    } else if(type.equalsIgnoreCase("IPADDRESS")) {
      dataType = SnmpAPI.IPADDRESS;
    } else if(type.equalsIgnoreCase("COUNTER")) {
      dataType = SnmpAPI.COUNTER;
    } else if(type.equalsIgnoreCase("OID")) {
      dataType = SnmpAPI.OBJID;
    } else if(type.equalsIgnoreCase("BITS")) {
      dataType = SnmpAPI.STRING;
    } else {
      _log.error("Can't convert '" + type + "' to a valid snmp variable type.");
      throw new RuntimeException("Invalid variable type: " + type);
    }
    return dataType;
  }

  @SuppressWarnings("unchecked")
  private List<String[]> graspResult(SnmpPDU pdu) {
    if(pdu == null) {
      return null;
    }
    List<String[]> ret = new Vector<String[]>();
    Vector<SnmpVarBind> val = pdu.getVariableBindings();
    for(Iterator<SnmpVarBind> itor = val.iterator(); itor.hasNext();) {
        SnmpVarBind tmp = itor.next();
        SnmpOID oid = tmp.getObjectID();
        SnmpVar var = tmp.getVariable();
        String[] row = new String[2];
        row[0] = oid.toString();
        row[1] = var instanceof SnmpNull?null:var.toString();
        ret.add(row);
    }
    /*int varCount = val.size();
    for(int i=0;i<varCount;i++){
       pdu.removeVariableBinding(i);
    }*/
    return ret;
  }
  
  /*private String getStringVal(SnmpVar var){
	String ret = null;
	byte type = var.getType();
    if(type==SnmpAPI.STRING){;
      ret = ((SnmpString)var).toByteString();
    }else{
      ret = var.toString();	
    }
	return ret;
  }*/
}
