/**
 * 
 */
package com.wafersystems.wse.sms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.smslib.GatewayException;
import org.smslib.InboundMessage;
import org.smslib.InboundMessage.MessageClasses;
import org.smslib.Message.MessageEncodings;
import org.smslib.OutboundMessage;
import org.smslib.SMSLibException;
import org.smslib.Service;
import org.smslib.TimeoutException;
import org.smslib.modem.SerialModemGateway;

/**
 * @author Eric
 * 
 */
public class MessageProcessorLib implements MessageProcessInterface {
	/**
   * 
   */
	Logger log = Logger.getLogger(this.getClass().getName());

	/**
   * 
   */
	Service srv;

	public MessageProcessorLib(String portName, int baud) {
		srv = new Service();
		SerialModemGateway gateway = new SerialModemGateway("WSE", portName,
				baud, "", "");
		gateway.setInbound(true);
		gateway.setOutbound(true);
		srv.addGateway(gateway);
		try {
			srv.startService();
		} catch (Exception e) {
			log.error("Initial modem error:", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.wafersystems.wse.sms.MessageProcessInterface#DeleteAllSMS(com.
	 * wafersystems .wse.sms.ComPort)
	 */
	@Override
	public List<MessageObject> DeleteAllSMS() {
		List<InboundMessage> smss = new LinkedList<InboundMessage>();
		try {
			srv.readMessages(smss, MessageClasses.ALL, "WSE");
		} catch (Exception e) {
			log.error("Get all message error", e);
		}
		List<MessageObject> messages = new ArrayList<MessageObject>();
		for (InboundMessage sms : smss) {
			MessageObject message = new MessageObject();
			message.setContent(sms.getText());
			message.setId((int) sms.getMessageId());
			message.setReceiver(sms.getOriginator());
			message.setSender(sms.getOriginator());
			message.setSendTime(sms.getDate());
			message.setState(sms.getType().toString());
			messages.add(message);
		}
		for (InboundMessage sms : smss) {
			try {
				srv.deleteMessage(sms);
				log.info("Delete message success, , Message ID="
						+ sms.getMessageId() + ", Sender="
						+ sms.getOriginator() + ", Content="
						+ sms.getText());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error("Delete message error, , Message ID="
						+ sms.getMessageId() + ", Sender="
						+ sms.getOriginator() + ", Content="
						+ sms.getText());
			}
		}

		return messages;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.wafersystems.wse.sms.MessageProcessInterface#sendmsn(java.lang.String
	 * , java.lang.String, com.wafersystems.wse.sms.ComPort)
	 */
	@Override
	public void sendmsn(String phone, String countstring) {
		OutboundMessage msg = new OutboundMessage(phone, countstring);
		msg.setEncoding(MessageEncodings.ENCUCS2);
		try {
			srv.sendMessage(msg);
			log.info("Send message success, reciever:" + phone + ", content:"
					+ countstring);
		} catch (Exception e) { 
			log.error("Send message fail, reciever:" + phone + ", content:"
					+ countstring, e);
		}
	}

	public static void main(String[] args) {
		MessageProcessInterface lib = new MessageProcessorLib("/dev/ttyS0", 9600);
		//MessageProcessInterface lib = new MessageProcessorLib("COM1", 9600);
		try {
			lib.sendmsn("18602978277", "test");
			//System.out.println(lib.DeleteAllSMS());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
