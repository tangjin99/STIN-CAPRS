/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import java.awt.*;
import java.io.*;
import java.util.Random;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.IOException;
import core.*;

/**
 * External event for creating a message.
 */
public class MessageCreateEvent extends MessageEvent {
	private int size;
	private int responseSize;
	/**------------------------------   对 MessageCreateEvent 添加的参量       --------------------------------*/

	private String fileID; 			// 添加了文件的ID名
	public final static String SelectLabel = "PacketType";
	/** user setting in the sim -setting id ({@value})*/
	public static final String USERSETTINGNAME_S = "userSetting";
	/** user setting in the sim Cache */
	public static final String EnableCache_s = "EnableCache";
    /** number of files */
	public static final String nrofFile_s = "nrofFile";
	/** namespace for host group settings ({@value})*/
	public static final String GROUP_NS = "Group";
	/** retransmission time of message */
	private static final String RETRANS_TIME = "reTransTime";

	public  static String MessageType="messagetype";
	public  static String MessageContentName="contentname";
	public static String MessageOriginalDestination="orgianaldestination";
	public static String isMessagegettobesttonode="istobesttonode";
	public int contentid;
	public static double rate0=0.7;
	public static double rate1=0.2;
	public static double rate2=0.1;
	public static int requestnum=0;





	/**------------------------------   对 MessageCreateEvent 添加的参量       --------------------------------*/

	/**
	 * Creates a message creation event with a optional response request
	 * @param from The creator of the message
	 * @param to Where the message is destined to
	 * @param id ID of the message
	 * @param size Size of the message
	 * @param responseSize Size of the requested response message or 0 if
	 * no response is requested
	 * @param time Time, when the message is created
	 */
	public MessageCreateEvent(int from, int to, String id, int size,
			int responseSize, double time) {
		super(from,to, id, time);
		this.size = size;
		this.responseSize = responseSize;
	}

	@Override
	public String toString() {
		return super.toString() + " [" + fromAddr + "->" + toAddr + "] " +
		"size:" + size + " CREATE";
	}

	/**------------------------------   对 MessageCreateEvent 添加的函数方法       --------------------------------*/

	/** 有关于文件相关修改的部分       */
	public String RandomGetFileID() {
		Settings ss = new Settings(GROUP_NS);					// 每一个主机组有一个配置对象，具体的可能和命名空间有关
		int nrofFile = ss.getInt(nrofFile_s);						// default设定的文件数目
		Random random = new Random();
		int id =random.nextInt(nrofFile);
		return "filename" +id;										// return filename;
	}
	/**
	 * Creates the message this event represents.
	 */
	@Override
	public void processEvent(World world) {
		Settings setting = new Settings(USERSETTINGNAME_S);		//读取设置，判断是否需要分簇
		//String cacheEnable = setting.getSetting(EnableCache_s); // decide whether to enable the cache function
		Message m = createMessage(world);
		DTNHost from = world.getNodeByAddress(this.fromAddr);
		from.createNewMessage(m);
	}

	public void processEvent1(){

	}

	private Message createMessage(World world){


//		if (cacheEnable.indexOf("true") >= 0) {
//	        this.fileID = RandomGetFileID();
//
//	        DTNHost from = world.getNodeByAddress(this.fromAddr);
//			this.toAddr = from.getFiles().get(this.fileID);							// 修改
//			DTNHost to = world.getNodeByAddress(this.toAddr);
//
//			this.responseSize = to.getFileBuffer().get(this.fileID).getSize();		// responseSize设定的是文件的大小，
//
//			Message m = new Message(from, to, this.id, this.size);
//			m.setResponseSize(this.responseSize);
//			m.setFilename(this.fileID);
//			m.updateProperty(SelectLabel, 0);													// 标识为控制包
//
////			System.out.println("当前节点是否包含文件："+ from.getFileBuffer().containsKey(this.fileID) +" " + "当前时刻："+ SimClock.getTime() );
//
//			// 如果目的节点和源节点不同，才创建消息，因为取得文件是随机的；     同时如果节点缓存有文件，不再发生请求。
//			if(this.toAddr!=this.fromAddr && !from.getFileBuffer().containsKey(this.fileID)) {
//				from.createNewMessage(m); 														// 把消息放进缓存中去
////				from.putIntoJudgeForRetransfer(m);												// 需要将消息放入到判断消息是否重传的buffer中
//			}
//		}
//		else{

		DTNHost to = world.getNodeByAddress(this.toAddr);
		DTNHost from = world.getNodeByAddress(this.fromAddr);
		Message m = new Message(from, to, this.id, this.size);

//		double randomnum=Math.random();
//		Random random=new Random();
//		//contentid为1-40的占据rate0 0.7的概率、contentid为41-100的占据rate1 0.2的概率、contentid为101-200的占据rate2 0.1的概率
//		if(randomnum<=rate0){
//			contentid=random.nextInt(400);
//			contentid+=1;
//		}
//		else if(randomnum<=rate1+rate0){
//			contentid=random.nextInt(600);
//			contentid+=401;
//		}
//		else if (randomnum<=rate1+rate0+rate2){
//			contentid=random.nextInt(1000);
//			contentid+=1001;
//		}
		m.addProperty(MessageType,"request");

		contentid=world.getRequestorder().get(requestnum++);
		m.addProperty(MessageContentName,contentid);

		m.addProperty(MessageOriginalDestination,null);

		m.addProperty(isMessagegettobesttonode,false);

		// set the retransmission time
		Settings s = new Settings("Interface");
		int time = s.getInt("reTransmitTime");
		m.updateProperty(RETRANS_TIME, time);
		m.setReceiveTime(SimClock.getTime());
		m.setResponseSize(this.responseSize);
		//		}
		return m;
	}



}
