/**
 * 
 */
package com.wafersystems.wse.sms;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/***
 * ���Ų�����
 */
public class MessageProcessorAT implements MessageProcessInterface {
  /**
   * 
   */
  Logger log = Logger.getLogger(this.getClass().getName());

  private MessageObject commonsms;

  private static char symbol1 = 13;

  private static String strReturn = "", atCommand = "";

  /**
   * 
   */
  ComPort port;

  public void releasePort() {
    this.port.close();
  }
  
  public MessageProcessorAT(String portName, int baud) {
    port = new ComPort(portName, baud);
  }

  public boolean SendSms(ComPort myport) {
    if (!myport.isIsused()) {
      log.error("Cannt open COM port.");
      return false;
    }
    // ����Ϊ����������Ϣ��ʽ
    setMessageMode(1);

    // �ո�
    char symbol2 = 34;
    // ctrl~z ����ָ��
    char symbol3 = 26;
    try {
      atCommand = "AT+CSMP=17,169,0,08" + String.valueOf(symbol1);
      strReturn = myport.sendAT(atCommand);
      log.info("Rerurn result of AT Command:" + strReturn);
      if (strReturn.indexOf("OK", 0) != -1) {
        atCommand = "AT+CMGS=" + commonsms.getReceiver()
            + String.valueOf(symbol1);
        strReturn = myport.sendAT(atCommand);
        atCommand = StringUtil.encodeHex(commonsms.getContent().trim())
            + String.valueOf(symbol3) + String.valueOf(symbol1);
        strReturn = myport.sendAT(atCommand);
        if (strReturn.indexOf("OK") != -1 && strReturn.indexOf("+CMGS") != -1) {
          log.info("Send message success, reciever:" + commonsms.getReceiver()
              + ", content:" + commonsms.getContent());
          return true;
        }
      }
    } catch (Exception ex) {
      log.error("Send message fail, reciever:" + commonsms.getReceiver()
          + ", content:" + commonsms.getContent(), ex);
    }
    return false;
  }

  /**
   * ������Ϣģʽ
   * 
   * @param op
   *          0-pdu 1-text(Ĭ��1 �ı���ʽ )
   * @return
   */
  public boolean setMessageMode(int op) {
    try {
      String atCommand = "AT+CMGF=" + String.valueOf(op)
          + String.valueOf(symbol1);
      String strReturn = port.sendAT(atCommand);
      if (strReturn.indexOf("OK", 0) != -1) {
        if (op == 0) {
          log.info("Set modem mode to English mode.");
        } else {
          log.info("Set modem mode to Chinese mode.");
        }
        return true;
      }
      return false;
    } catch (Exception ex) {
      log.error("Error occured when set modem mode to ascii", ex);
      return false;
    }
  }

  /**
   * ��ȡ���ж���
   * 
   * @return CommonSms����
   */
  public List<MessageObject> RecvSmsList() {
    if (!port.isIsused()) {
      log.error("Cannt open COM port.");
      return null;
    }
    List<MessageObject> listMes = new ArrayList<MessageObject>();
    try {
      atCommand = "AT+CMGL=\"ALL\"";
      strReturn = port.sendAT(atCommand);
      listMes = StringUtil.analyseArraySMS(strReturn);
    } catch (Exception ex) {
      log.error("Get all message error", ex);
    }
    return listMes;
  }

  /**
   * ɾ������
   * 
   * @param index
   *          ���Ŵ洢��λ��
   * @return boolean
   */

  public boolean DeleteSMS(int index) {
    if (!port.isIsused()) {
      log.error("Cannt open COM port.");
      return false;
    }
    try {
      atCommand = "AT+CMGD=" + index;
      strReturn = port.sendAT(atCommand);
      if (strReturn.indexOf("OK") != -1) {
        return true;
      }
    } catch (Exception ex) {
      log.error("Delete message error", ex);
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wafersystems.wse.sms.MessageProcessInterface#DeleteAllSMS(com.wafersystems
   * .wse.sms.ComPort)
   */
  @Override
  public List<MessageObject> DeleteAllSMS() throws Exception {
    List<MessageObject> list = RecvSmsList();
    if (list != null && !list.equals("") && list.size() > 0) {
      for (int i = 0; i < list.size(); i++) {
        MessageObject tempcomsms = (MessageObject) list.get(i);
        if (!DeleteSMS(tempcomsms.getId())) {
          throw new Exception("Delete SMS Error, Message ID="
              + tempcomsms.getId() + ", Sender=" + tempcomsms.getSender()
              + ", Content=" + tempcomsms.getContent());
        } else {
          log.info("Delete message success, , Message ID=" + tempcomsms.getId()
              + ", Sender=" + tempcomsms.getSender() + ", Content="
              + tempcomsms.getContent());
        }
      }
    }
    return list;
  }

  public MessageObject getCommonsms() {
    return commonsms;
  }

  public void setCommonsms(MessageObject commonsms) {
    this.commonsms = commonsms;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wafersystems.wse.sms.MessageProcessInterface#sendmsn(java.lang.String,
   * java.lang.String, com.wafersystems.wse.sms.ComPort)
   */
  @Override
  public void sendmsn(String phone, String countstring) {
    // ���Ͳ���
    MessageObject cs = new MessageObject();
    cs.setReceiver(phone);
    cs.setContent(countstring);
    this.setCommonsms(cs);
    this.SendSms((ComPort) port);
  }

  public static void main(String[] args) throws Exception {
    MessageProcessorAT messageProxy = new MessageProcessorAT("COM1", 9600);
    messageProxy.sendmsn("18602978277",
        "All port have been opene. Wafer Smart Energy Team.");
    //messageProxy.releasePort();
  }

}