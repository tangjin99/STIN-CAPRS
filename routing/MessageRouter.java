/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.omg.CORBA.PUBLIC_MEMBER;
import routing.util.RoutingInfo;
import util.Tuple;
import core.Application;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import core.SimError;
import Cache.CacheRouter;


/**
 * Superclass for message routers.
 */
public abstract class MessageRouter {
	/** Message buffer size -setting id ({@value}). Integer value in bytes.*/
	public static final String B_SIZE_S = "bufferSize";
	/**
	 * Message TTL -setting id ({@value}). Value is in minutes and must be
	 * an integer. 
	 */ 
	public static final String MSG_TTL_S = "msgTtl";
	/**
	 * Message/fragment sending queue type -setting id ({@value}). 
	 * This setting affects the order the messages and fragments are sent if the
	 * routing protocol doesn't define any particular order (e.g, if more than 
	 * one message can be sent directly to the final recipient). 
	 * Valid values are<BR>
	 * <UL>
	 * <LI/> 1 : random (message order is randomized every time; default option)
	 * <LI/> 2 : FIFO (most recently received messages are sent last)
	 * </UL>
	 */ 
	public static final String SEND_QUEUE_MODE_S = "sendQueue";
	
	/** Setting value for random queue mode */
	public static final int Q_MODE_RANDOM = 1;
	/** Setting value for FIFO queue mode */
	public static final int Q_MODE_FIFO = 2;
	
    /** indicates that if this node is communication satellites*/
    public boolean CommunicationSatellitesLabel;
    /** record all communication nodes and their orbit plane number*/
    public HashMap<DTNHost, Integer> CommunicationNodesList;//注意：这里记录的平面编号从0开始
    
	/* Return values when asking to start a transmission:
	 * RCV_OK (0) means that the host accepts the message and transfer started, 
	 * values < 0 mean that the  receiving host will not accept this 
	 * particular message (right now), 
	 * values > 0 mean the host will not right now accept any message. 
	 * Values in the range [-100, 100] are reserved for general return values
	 * (and specified here), values beyond that are free for use in 
	 * implementation specific cases */
	/** Receive return value for OK */
	public static final int RCV_OK = 0;
	/** Receive return value for busy receiver */
	public static final int TRY_LATER_BUSY = 1;
	/** Receive return value for an old (already received) message */
	public static final int DENIED_OLD = -1;
	/** Receive return value for not enough space in the buffer for the msg */
	public static final int DENIED_NO_SPACE = -2;
	/** Receive return value for messages whose TTL has expired */
	public static final int DENIED_TTL = -3;
	/** Receive return value for a node low on some resource(s) */
	public static final int DENIED_LOW_RESOURCES = -4;
	/** Receive return value for a node low on some resource(s) */
	public static final int DENIED_POLICY = -5;
	/** Receive return value for unspecified reason */
	public static final int DENIED_UNSPECIFIED = -99;
	
	protected List<MessageListener> mListeners;
	/** The messages being transferred with msgID_hostName keys */
	protected HashMap<String, Message> incomingMessages;

	/** The messages this router has received as the final recipient */
	protected HashMap<String, Message> deliveredMessages;
	/** The messages that Applications on this router have blacklisted */
	private HashMap<String, Object> blacklistedMessages;
	/** Host where this router belongs to */
	private DTNHost host;
	/** size of the buffer */
	private int bufferSize;
	/** TTL for all messages */
	protected int msgTtl;
	/** Queue mode for sending messages */
	protected int sendQueueMode;
	/** applications attached to the host */
	private HashMap<String, Collection<Application>> applications = null;	
	/** The messages this router is carrying */
	protected HashMap<String, Message> messages; 
	
	/**------------------------------   对MessageRouter添加的变量       --------------------------------*/
	/** 用于判断包的类型 */
	public String SelectLabel;
	/** user setting in the sim -setting id ({@value})*/
	public static final String USERSETTINGNAME_S = "userSetting";
	/** user setting in the sim Cache */
	public static final String EnableCache_s = "EnableCache";
	public String cacheEnable;
	/** retransmission time of message */
	private static final String RETRANS_TIME = "reTransTime";
	private int reTranstime;
	protected HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
	protected HashMap<DTNHost, List<Tuple<Integer, Boolean>>> routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();

	/**tangjin*/
	//数据包大小
	public int dataMessageSize=4096;

	//message的一些附加属性
	public static String MessageType = "messagetype";
	public static String MessageContentName = "contentname";
	public static String MessageOriginalDestination="orgianaldestination";
	public static String isMessagegettobesttonode="istobesttonode";
	private static List<Integer> Buffer;

	//本地流行度表
	protected Map<Integer,Popularityinfo> popularityinfoList=new HashMap<Integer,Popularityinfo>();

	//上一周期下发的流行度表
	protected Map<Integer,Popularityinfo> lasttimepopularinfolist=new HashMap<Integer,Popularityinfo>();

	//基于排名预测方案的一些参数
	protected int feedbackinfolength=10;
	protected double[] feedbackinfo=new double [feedbackinfolength];
	private  int T_updatefeedbackinfo=1;

	//计算平均排队时延的参数
	protected List<Tuple<String,Double>> comingtoqueuetime=new ArrayList<Tuple<String,Double>>();
	protected List<Tuple<String,Double>> leavetoqueuetime=new ArrayList<Tuple<String,Double>>();
	protected int  queuetimesize=20;//计算最近20个包的排队时延

	//是否进行预测
	protected static int isprediction;

	//buffer更新的周期数
	private int T_bufferupdateinterval=1;
	//buffer更新周期
	protected static final int updatebuffinterval=5;
	//是否需要更新buffer
	protected boolean toupdatebuffercontent;
	protected   List<Integer> buffercontent=new ArrayList<Integer>();
	//缓存大小
	protected  int BufferSize;
	//缓存节点数
	public int cacheenablednodemin;
	public int cacheenablednodemax;


	//收集流行度表的周期数
	public  int T=1;
	//流行度表下发周期
	public int collectpoplistinterval;
	//本地的总请求数
	protected double totalrequests=0;
	//本地的请求平均达到速率
	protected double lamta_average=0;
	//全网流行度表哦
	protected  static  Map<DTNHost,Map<Integer,Popularityinfo>> totalpoplist=new HashMap<DTNHost,Map<Integer,Popularityinfo>>();

	//T1 T2 T3 T4分别用来记录 去往bestto node之前就已经命中的次数、去往bestto node命中的次数、去往original命中的此时、在besttonode上未命中去往origina destinationnode的数目
	public static int T1=0; //!!!
	public static int T2=0; //!!
	public static int T3=0; //!
	public static int T4=0; //!-_-
	public static int P_hit=0;
	public static int P_miss=0;
	public static int P_hit1=0;
	public static int P_miss1=0;


	/**tangjin*/

	/** ------------------------------   对MessageRouter添加的变量       --------------------------------*/
	
	

	
	/**
	 * Initializes the router; i.e. sets the host this router is in and
	 * message listeners that need to be informed about message related
	 * events etc.
	 * @param host The host this router is in
	 * @param mListeners The message listeners
	 */
	public void init(DTNHost host, List<MessageListener> mListeners) {
		this.incomingMessages = new HashMap<String, Message>();
		this.messages = new HashMap<String, Message>();
		this.deliveredMessages = new HashMap<String, Message>();
		this.blacklistedMessages = new HashMap<String, Object>();
		this.mListeners = mListeners;
		this.host = host;
		Settings setting = new Settings(USERSETTINGNAME_S);		//读取设置，判断是否需要分簇
		cacheEnable = setting.getSetting(EnableCache_s); // decide whether to enable the cache function
	    Settings s = new Settings("Interface");
	    reTranstime = s.getInt("reTransmitTime");


	}

	
	/**
	 * Updates router.
	 * This method should be called (at least once) on every simulation
	 * interval to update the status of transfer(s). 
	 */
	public void update(){
		if(toupdatebuffercontent&&this.iscachenablednode()){
			updatebuffercontent();
			toupdatebuffercontent=false;
		}

		//每间隔collectpoplistinterval收集一次流行度表
		if(SimClock.getTime()>(T*collectpoplistinterval)&&SimClock.getTime()<((T+1)*collectpoplistinterval)){
			collectpoplist();
			T++;
		}

		//每间隔bufferupdateinterval更新一次buffer
		if(SimClock.getTime()>(T_bufferupdateinterval*updatebuffinterval)&&SimClock.getTime()<((T_bufferupdateinterval+1)*updatebuffinterval)){
			toupdatebuffercontent=true;
			T_bufferupdateinterval++;
		}

		//更新一次反馈信息，与基于排名的内容预测方案相关
		if(SimClock.getTime()>(T_updatefeedbackinfo*collectpoplistinterval)&&SimClock.getTime()<((T_updatefeedbackinfo+1)*collectpoplistinterval)){
			if(this.iscachenablednode()){
				updatefeedbackinfo();
			}
			T_updatefeedbackinfo++;
		}

		for (Collection<Application> apps : this.applications.values()) {
			for (Application app : apps) {
				app.update(this.host);
			}
		}

	}
	
	/**
	 * Informs the router about change in connections state.
	 * @param con The connection that changed
	 */
	public abstract void changedConnection(Connection con);	
	
	/**
	 * Returns a message by ID.
	 * @param id ID of the message
	 * @return The message
	 */
	public Message getMessage(String id) {
		if(this.messages.get(id)==null)
			return this.incomingMessages.get(id);
		return this.messages.get(id);
	}
	
	/**
	 * Checks if this router has a message with certain id buffered.
	 * @param id Identifier of the message
	 * @return True if the router has message with this id, false if not
	 */
	public boolean hasMessage(String id) {
		return this.messages.containsKey(id);
	}
	
	/**
	 * Returns true if a full message with same ID as the given message has been
	 * received by this host as the <strong>final</strong> recipient 
	 * (at least once).
	 * @param m message we're interested of
	 * @return true if a message with the same ID has been received by 
	 * this host as the final recipient.
	 */
	public boolean isDeliveredMessage(Message m) {
		return (this.deliveredMessages.containsKey(m.getId()));
	}
	
	/** 
	 * Returns <code>true</code> if the message has been blacklisted. Messages
	 * get blacklisted when an application running on the node wants to drop it.
	 * This ensures the peer doesn't try to constantly send the same message to
	 * this node, just to get dropped by an application every time.
	 * 
	 * @param id	id of the message
	 * @return <code>true</code> if blacklisted, <code>false</code> otherwise.
	 */
	protected boolean isBlacklistedMessage(String id) {
		return this.blacklistedMessages.containsKey(id);
	}
	
	/**
	 * Returns a reference to the messages of this router in collection.
	 * <b>Note:</b> If there's a chance that some message(s) from the collection
	 * could be deleted (or added) while iterating through the collection, a
	 * copy of the collection should be made to avoid concurrent modification
	 * exceptions. 
	 * @return a reference to the messages of this router in collection
	 */
	public Collection<Message> getMessageCollection() {
		return this.messages.values();
	}

	/**
	 * Returns a reference to the delivered messages of this router in collection.
	 * <b>Note:</b> If there's a chance that some message(s) from the collection
	 * could be deleted (or added) while iterating through the collection, a
	 * copy of the collection should be made to avoid concurrent modification
	 * exceptions.
	 * @return a reference to the delivered messages of this router in collection
	 */
	public Collection<Message> getDeliveredMessageCollection() {
		return this.deliveredMessages.values();
	}
	
	/**
	 * Returns the number of messages this router has
	 * @return How many messages this router has
	 */
	public int getNrofMessages() {
		return this.messages.size();
	}
	
	/**
	 * Returns the size of the message buffer.
	 * @return The size or Integer.MAX_VALUE if the size isn't defined.
	 */
	public int getBufferSize() {
		return this.bufferSize;
	}
	
	/**
	 * Returns the amount of free space in the buffer. May return a negative
	 * value if there are more messages in the buffer than should fit there
	 * (because of creating new messages).
	 * @return The amount of free space (Integer.MAX_VALUE if the buffer
	 * size isn't defined)
	 */
	public int getFreeBufferSize() {
		int occupancy = 0;
		
		if (this.getBufferSize() == Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		
		for (Message m : getMessageCollection()) {
			occupancy += m.getSize();
		}
		
		return this.getBufferSize() - occupancy;
	}
	
	/**
	 * Returns the host this router is in
	 * @return The host object
	 */
	protected DTNHost getHost() {
		return this.host;
	}
	
	/**
	 * Start sending a message to another host.
	 * @param id Id of the message to send
	 * @param to The host to send the message to
	 */
	public void sendMessage(String id, DTNHost to) {
		Message m = getMessage(id);
		Message m2;
		if (m == null) throw new SimError("no message for id " +
				id + " to send at " + this.host);
 
		m2 = m.replicate();	// send a replicate of the message
		to.receiveMessage(m2, this.host);
	}
	
	/**
	 * Requests for deliverable message from this router to be sent trough a
	 * connection.
	 * @param con The connection to send the messages trough
	 * @return True if this router started a transfer, false if not
	 */
	public boolean requestDeliverableMessages(Connection con) {
		return false; // default behavior is to not start -- subclasses override
	}
	
	/**
	 * Try to start receiving a message from another host.
	 * @param m Message to put in the receiving buffer
	 * @param from Who the message is from
	 * @return Value zero if the node accepted the message (RCV_OK), value less
	 * than zero if node rejected the message (e.g. DENIED_OLD), value bigger
	 * than zero if the other node should try later (e.g. TRY_LATER_BUSY).
	 */
	public int receiveMessage(Message m, DTNHost from) {
		Message newMessage = m.replicate();
				
		this.putToIncomingBuffer(newMessage, from);		
		newMessage.addNodeOnPath(this.host);
		
		for (MessageListener ml : this.mListeners) {
			ml.messageTransferStarted(newMessage, from, getHost());
		}
		
		return RCV_OK; // superclass always accepts messages
	}

	
	/**
	 * This method should be called (on the receiving host) after a message
	 * was successfully transferred. The transferred message is put to the
	 * message buffer unless this host is the final recipient of the message.
	 * @param id Id of the transferred message
	 * @param from Host the message was from (previous hop)
	 * @return The message that this host received
	 */
	public Message messageTransferred(String id, DTNHost from) {
			Message message = removeFromIncomingBuffer(id, from);
			if(message.getTo()==this.host){
				Message incoming= message;           //将消息从incomingMessages删除
				if(message.getMessageType()=="request"){
					;
					//记录在bestto node上命中的次数
					if((DTNHost) message.getOriginalDestination()==null&&!(boolean)message.istobesttonode()){
						T3++;
						System.out.println(message.getId()+" request= "+ message.getMessageContentName()+" message from: " +message.getFrom()+" message original tonode: "+message.getOriginalDestination()
								+" message found in the original node: "+this.getHost()+" "+T3+" ! ");
						List<Message> dataMessages = createDataMessages(message);
						for (Message dataMsg : dataMessages){
							if(!hasMessage(dataMsg.getId()))
								createNewMessage(dataMsg);
						}
					}

					else if((DTNHost) message.getOriginalDestination()!=null&&(buffercontent.contains((Integer) message.getMessageContentName()))){
						//System.out.println("429 coming in:");
						updatepopularitylist(message);
						T2++;
						System.out.println(message.getId()+" request= "+message.getMessageContentName()+" message from: " +message.getFrom()+" message original tonode: "+message.getOriginalDestination()
								+" message found in the bestto node: "+this.getHost()+" "+T2+"!!");
						List<Message> dataMessages = createDataMessages(message);
						for (Message dataMsg : dataMessages){
							if(!hasMessage(dataMsg.getId()))
								createNewMessage(dataMsg);
						}
					}
					//记录在original node上命中的次数
//					else if((message.getOriginalDestination()!=message.getTo())&&(!popularityinfoList.containsKey((Integer) message.getMessageContentName()))){
//						message.setTo((DTNHost) message.getOriginalDestination());
//						T3++;
//						System.out.println(message.getId()+" message from: " +message.getFrom()+" message original tonode: "+message.getOriginalDestination()
//								+" message found in the original node: "+this.getHost()+" "+T3+" ! ");
//
//					}
					else if ((DTNHost) message.getOriginalDestination()!=null&&!(buffercontent.contains((Integer) message.getMessageContentName()))){
							//System.out.println("450 coming in :");
						    updatepopularitylist(message);
							message.setTo((DTNHost) message.getOriginalDestination());
							message.updateProperty(MessageOriginalDestination,null);
							message.updateProperty(isMessagegettobesttonode,true);
					}

					else if((DTNHost) message.getOriginalDestination()==null&&(boolean)message.istobesttonode()){
						T4++;
						System.out.println(message.getId()+" request= "+message.getMessageContentName()+" message from: " +message.getFrom()+" message original tonode: "+message.getOriginalDestination()
								+" message not hit in the bestto node but found in the original node: "+this.getHost()+" "+T4+" ! -_-");
						List<Message> dataMessages = createDataMessages(message);
						for (Message dataMsg : dataMessages){
							if(!hasMessage(dataMsg.getId()))
								createNewMessage(dataMsg);
						}
					}


					}


				boolean isFinalRecipient;                                        //判断消息传递到目的节点
				boolean isFirstDelivery;                                        //is this first delivered instance of the msg //消息传递到该节点
				if (incoming == null) {
					throw new SimError("No message with ID " + id + " in the incoming " +
							"buffer of " + this.host);
				}

				incoming.setReceiveTime(SimClock.getTime());                    //设置消息接收时间

				// Pass the message to the application (if any) and get outgoing message
				Message outgoing = incoming;
				for (Application app : getApplications(incoming.getAppID())) {
					// Note that the order of applications is significant
					// since the next one gets the output of the previous.
					outgoing = app.handle(outgoing, this.host);
					if (outgoing == null) break;                                // Some app want to drop the message
				}

				Message aMessage = (outgoing == null) ? (incoming) : (outgoing);
				// If the application re-targets the message (changes 'to')
				// then the message is not considered as 'delivered' to this host.
				isFinalRecipient = aMessage.getTo() == this.host;
				isFirstDelivery = isFinalRecipient && !isDeliveredMessage(aMessage);    // 判断是否为目的节点且为第一次到达

				/** put the message into the corresponding buffer*/
				if (!isFinalRecipient && outgoing != null) {            // 不是目的节点，应用层也不想丢掉这个消息
					// when dtnHost receive this message, the retransmission time should be updated
					aMessage.updateProperty(RETRANS_TIME, this.reTranstime);

					addToMessages(aMessage, false);

				}
				else if (isFirstDelivery) {                            // 这是目的节点且是第一次到达
					this.deliveredMessages.put(id, aMessage);

				}
				else if (outgoing == null) {
					// Blacklist messages that an app wants to drop.
					// Otherwise the peer will just try to send it back again.
					this.blacklistedMessages.put(id, null);
				}

				for (MessageListener ml : this.mListeners) {
					ml.messageTransferred(aMessage, from, this.host, isFirstDelivery);
				}

				return aMessage;
			}
			else {
//			Message incoming;
				Message incoming = message;

				boolean isFinalRecipient;                                        //判断消息传递到目的节点
				boolean isFirstDelivery;                                        //is this first delivered instance of the msg //消息传递到该节点
				if (incoming == null) {
					throw new SimError("No message with ID " + id + " in the incoming " +
							"buffer of " + this.host);
				}


				incoming.setReceiveTime(SimClock.getTime());                    //设置消息接收时间

				// Pass the message to the application (if any) and get outgoing message
				Message outgoing = incoming;
				for (Application app : getApplications(incoming.getAppID())) {
					// Note that the order of applications is significant
					// since the next one gets the output of the previous.
					outgoing = app.handle(outgoing, this.host);
					if (outgoing == null) break;                                // Some app want to drop the message
				}

				Message aMessage = (outgoing == null) ? (incoming) : (outgoing);
				// If the application re-targets the message (changes 'to')
				// then the message is not considered as 'delivered' to this host.
				isFinalRecipient = aMessage.getTo() == this.host;
				isFirstDelivery = isFinalRecipient && !isDeliveredMessage(aMessage);    // 判断是否为目的节点且为第一次到达

				/** put the message into the corresponding buffer*/
				if (!isFinalRecipient && outgoing != null) {            // 不是目的节点，应用层也不想丢掉这个消息
					// when dtnHost receive this message, the retransmission time should be updated
					aMessage.updateProperty(RETRANS_TIME, this.reTranstime);

					addToMessages(aMessage, false);

				}
				else if (isFirstDelivery) {                            // 这是目的节点且是第一次到达
					this.deliveredMessages.put(id, aMessage);

				}
				else if (outgoing == null) {
					// Blacklist messages that an app wants to drop.
					// Otherwise the peer will just try to send it back again.
					this.blacklistedMessages.put(id, null);
				}
				for (MessageListener ml : this.mListeners) {
					ml.messageTransferred(aMessage, from, this.host, isFirstDelivery);
				}
				return aMessage;
			}

	}



	/**
	 * Puts a message to incoming messages buffer. Two messages with the
	 * same ID are distinguished by the from host.
	 * @param m The message to put
	 * @param from Who the message was from (previous hop).
	 */
	protected void putToIncomingBuffer(Message m, DTNHost from) {
		this.incomingMessages.put(m.getId() + "_" + from.toString(), m);

	}
	
	/**
	 * Removes and returns a message with a certain ID from the incoming 
	 * messages buffer or null if such message wasn't found. 
	 * @param id ID of the message
	 * @param from The host that sent this message (previous hop)
	 * @return The found message or null if such message wasn't found
	 */
	protected Message removeFromIncomingBuffer(String id, DTNHost from) {
		return this.incomingMessages.remove(id + "_" + from.toString());
	}
	
	/**
	 * Returns true if a message with the given ID is one of the
	 * currently incoming messages, false if not
	 * @param id ID of the message
	 * @return True if such message is incoming right now
	 */
	protected boolean isIncomingMessage(String id) {
		return this.incomingMessages.containsKey(id);
	}
	
	/**
	 * Adds a message to the message buffer and informs message listeners
	 * about new message (if requested).
	 * @param m The message to add
	 * @param newMessage If true, message listeners are informed about a new
	 * message, if false, nothing is informed.
	 */
	public void addToMessages(Message m, boolean newMessage) {		
		this.messages.put(m.getId(), m);
		
		if (newMessage) {
			for (MessageListener ml : this.mListeners) {
				ml.newMessage(m);
			}
		}

		if(comingtoqueuetime.size()>queuetimesize){
			comingtoqueuetime.remove(0);
			Tuple<String,Double> msgid_comingtime=new Tuple<String,Double>(m.getId(),SimClock.getTime());
			comingtoqueuetime.add(msgid_comingtime);
		}
		else {
			Tuple<String,Double> msgid_comingtime=new Tuple<String,Double>(m.getId(),SimClock.getTime());
			comingtoqueuetime.add(msgid_comingtime);
		}



	}
	

	
	/**
	 * This method should be called (on the receiving host) when a message 
	 * transfer was aborted.
	 * @param id Id of the message that was being transferred
	 * @param from Host the message was from (previous hop)
	 * @param bytesRemaining Nrof bytes that were left before the transfer
	 * would have been ready; or -1 if the number of bytes is not known
	 */
	public void messageAborted(String id, DTNHost from, int bytesRemaining) {
		Message incoming = removeFromIncomingBuffer(id, from);
		if (incoming == null) {
			throw new SimError("No incoming message for id " + id + 
					" to abort in " + this.host);
		}		
		
		for (MessageListener ml : this.mListeners) {
			ml.messageTransferAborted(incoming, from, this.host);
		}
	}



	/**
	 * Creates a new message to the router.
	 * @param m The message to create
	 * @return True if the creation succeeded, false if not (e.g.
	 * the message was too big for the buffer)
	 */
	public boolean createNewMessage(Message m) {
		m.setTtl(this.msgTtl);
		addToMessages(m, true);		
		return true;
	}
	
	/**
	 * Deletes a message from the buffer and informs message listeners
	 * about the event
	 * @param id Identifier of the message to delete
	 * @param drop If the message is dropped (e.g. because of full buffer) this 
	 * should be set to true. False value indicates e.g. remove of message
	 * because it was delivered to final destination.  
	 */
	public void deleteMessage(String id, boolean drop) {
		Message removed = removeFromMessages(id); 
		if (removed == null) throw new SimError("no message for id " +
				id + " to remove at " + this.host);
		
		for (MessageListener ml : this.mListeners) {
			ml.messageDeleted(removed, this.host, drop);
		}
	}
	
	/**
	 * Sorts/shuffles the given list according to the current sending queue 
	 * mode. The list can contain either Message or Tuple<Message, Connection> 
	 * objects. Other objects cause error. 
	 * @param list The list to sort or shuffle
	 * @return The sorted/shuffled list
	 */
	@SuppressWarnings(value = "unchecked") /* ugly way to make this generic */
	protected List sortByQueueMode(List list) {
		switch (sendQueueMode) {
		case Q_MODE_RANDOM:
			Collections.shuffle(list, new Random(SimClock.getIntTime()));
			break;
		case Q_MODE_FIFO:
			Collections.sort(list, 
					new Comparator() {
				/** Compares two tuples by their messages' receiving time */
				public int compare(Object o1, Object o2) {
					double diff;
					Message m1, m2;
					
					if (o1 instanceof Tuple) {
						m1 = ((Tuple<Message, Connection>)o1).getKey();
						m2 = ((Tuple<Message, Connection>)o2).getKey();
					}
					else if (o1 instanceof Message) {
						m1 = (Message)o1;
						m2 = (Message)o2;
					}
					else {
						throw new SimError("Invalid type of objects in " + 
								"the list");
					}
					diff = m1.getReceiveTime() - m2.getReceiveTime();
					
					if (diff == 0) {
						return 0;
					}
					return (diff < 0 ? -1 : 1);
				}
			});
			break;
		/* add more queue modes here */
		default:
			throw new SimError("Unknown queue mode " + sendQueueMode);
		}
		return list;
	}

	/**
	 * Gives the order of the two given messages as defined by the current
	 * queue mode 
	 * @param m1 The first message
	 * @param m2 The second message
	 * @return -1 if the first message should come first, 1 if the second 
	 *          message should come first, or 0 if the ordering isn't defined
	 */
	protected int compareByQueueMode(Message m1, Message m2) {
		switch (sendQueueMode) {
		case Q_MODE_RANDOM:
			/* return randomly (enough) but consistently -1, 0 or 1 */
			return (m1.hashCode()/2 + m2.hashCode()/2) % 3 - 1; 
		case Q_MODE_FIFO:
			double diff = m1.getReceiveTime() - m2.getReceiveTime();
			if (diff == 0) {
				return 0;
			}
			return (diff < 0 ? -1 : 1);
		/* add more queue modes here */
		default:
			throw new SimError("Unknown queue mode " + sendQueueMode);
		}
	}
	
	/**
	 * Returns routing information about this router.
	 * @return The routing information.
	 */
	public RoutingInfo getRoutingInfo() {
		RoutingInfo ri = new RoutingInfo(this);
		RoutingInfo incoming = new RoutingInfo(this.incomingMessages.size() + 
				" incoming message(s)");
		RoutingInfo delivered = new RoutingInfo(this.deliveredMessages.size() +
				" delivered message(s)");
		
		RoutingInfo cons = new RoutingInfo(host.getConnections().size() + 
			" connection(s)");
				
		ri.addMoreInfo(incoming);
		ri.addMoreInfo(delivered);
		ri.addMoreInfo(cons);
		
		for (Message m : this.incomingMessages.values()) {
			incoming.addMoreInfo(new RoutingInfo(m));
		}
		
		for (Message m : this.deliveredMessages.values()) {
			delivered.addMoreInfo(new RoutingInfo(m + " path:" + m.getHops()));
		}
		
		for (Connection c : host.getConnections()) {
			cons.addMoreInfo(new RoutingInfo(c));
		}

		return ri;
	}
	
	/** 
	 * Adds an application to the attached applications list.
	 * 
	 * @param app	The application to attach to this router.
	 */
	public void addApplication(Application app) {
		if (!this.applications.containsKey(app.getAppID())) {
			this.applications.put(app.getAppID(),
					new LinkedList<Application>());
		}
		this.applications.get(app.getAppID()).add(app);
	}
	
	/** 
	 * Returns all the applications that want to receive messages for the given
	 * application ID.
	 * 
	 * @param ID	The application ID or <code>null</code> for all apps.
	 * @return		A list of all applications that want to receive the message.
	 */
	public Collection<Application> getApplications(String ID) {
		LinkedList<Application>	apps = new LinkedList<Application>();
		// Applications that match
		Collection<Application> tmp = this.applications.get(ID);
		if (tmp != null) {
			apps.addAll(tmp);
		}
		// Applications that want to look at all messages
		if (ID != null) {
			tmp = this.applications.get(null);
			if (tmp != null) {
				apps.addAll(tmp);
			}
		}
		return apps;
	}

	/**
	 * Creates a replicate of this router. The replicate has the same
	 * settings as this router but empty buffers and routing tables.
	 * @return The replicate
	 */
	public abstract MessageRouter replicate();
	
	/**
	 * Returns a String presentation of this router
	 * @return A String presentation of this router
	 */
	public String toString() {
		return getClass().getSimpleName() + " of " + 
			this.getHost().toString() + " with " + getNrofMessages() 
			+ " messages";
	}
	
	
	
	/** -------------------------- 对代码的修改  --------------------------- */
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object. Size of the message buffer is read from
	 * {@link #B_SIZE_S} setting. Default value is Integer.MAX_VALUE.
	 * @param s The settings object
	 */
	public MessageRouter(Settings s) {
		this.bufferSize = Integer.MAX_VALUE; // defaults to rather large buffer	
		
		this.msgTtl = Message.INFINITE_TTL;
		this.applications = new HashMap<String, Collection<Application>>();

		if (s.contains(B_SIZE_S)) {
			this.bufferSize = s.getInt(B_SIZE_S);
		}
		if (s.contains(MSG_TTL_S)) {
			this.msgTtl = s.getInt(MSG_TTL_S);
		}
		if (s.contains(SEND_QUEUE_MODE_S)) {
			this.sendQueueMode = s.getInt(SEND_QUEUE_MODE_S);
			if (sendQueueMode < 1 || sendQueueMode > 2) {
				throw new SettingsError("Invalid value for " + 
						s.getFullPropertyName(SEND_QUEUE_MODE_S));
			}
		}
		else {
			sendQueueMode = Q_MODE_RANDOM;
		}
		
	}

	
	/**
	 * Copy-constructor.
	 * @param r Router to copy the settings from.
	 */
	protected MessageRouter(MessageRouter r) {
		this.bufferSize = r.bufferSize;
		this.msgTtl = r.msgTtl;
		this.sendQueueMode = r.sendQueueMode;

		this.applications = new HashMap<String, Collection<Application>>();
		for (Collection<Application> apps : r.applications.values()) {
			for (Application app : apps) {
				addApplication(app.replicate());
			}
		}
	}
	
	/**
	 * Removes and returns a message from the message buffer.
	 * @param id Identifier of the message to remove
	 * @return The removed message or null if message for the ID wasn't found
	 */
	public Message removeFromMessages(String id) {
		Message m = this.messages.remove(id);
		if(leavetoqueuetime.size()<=queuetimesize) {
			Tuple<String, Double> msgid_processingtime = new Tuple<String, Double>(m.getId(), SimClock.getTime());
			leavetoqueuetime.add(msgid_processingtime);
		}
		else {
			leavetoqueuetime.remove(0);
			Tuple<String, Double> msgid_processingtime = new Tuple<String, Double>(m.getId(), SimClock.getTime());
			leavetoqueuetime.add(msgid_processingtime);
		}
		return m;
	}



	/**tangjin*/

	public  Map<Integer,Popularityinfo> getPopularityinfoList(){
		return popularityinfoList;
	}

	public  Map<Integer,Popularityinfo> getPopularityinfoListfortotalpoplist(){
		Map<Integer,Popularityinfo> tempmap=new HashMap<Integer,Popularityinfo>();
		for(Map.Entry<Integer,Popularityinfo> entry:popularityinfoList.entrySet()){
			Popularityinfo tobecopiedpopinfo=entry.getValue();
			Popularityinfo tempinfo=tobecopiedpopinfo.replicate(tobecopiedpopinfo);
			tempmap.put(entry.getKey(),tempinfo);
		}
		return tempmap;
	}

	//创建data数据包
	protected List<Message> createDataMessages(Message requestMsg){
		List<Message> dataMessages = new ArrayList<Message>();
		DTNHost from = this.getHost();
		DTNHost to = requestMsg.getFrom();
		int contentSize = 200000;//对应真实命中的内容大小

		int nrofDataMessages =  (int) Math.round(contentSize/this.dataMessageSize);//固定Data数据包的大小，确定回传的Data数据包数量

		for (int i=0; i < nrofDataMessages; i++){
			String id = requestMsg.getId() + "_data"+"_"+i;
			Message m = new Message(from, to, id, this.dataMessageSize);
			m.addProperty(MessageType,"data");//确定data数据包属性

			//contentid=2;
			int contentID = (Integer) requestMsg.getMessageContentName();
			m.addProperty(MessageContentName, contentID);

			dataMessages.add(m);
		}
		return dataMessages;
	}

	//更新本地流行度表，没收到一个请求触发一次
	public void updatepopularitylist(Message msg){

		int contentid=(Integer) msg.getMessageContentName();

			if(!popularityinfoList.containsKey(contentid)) {
				Popularityinfo popinfo = new Popularityinfo(contentid, this.getHost());
				popularityinfoList.put(contentid, popinfo);
				popularityinfoList.get(contentid).updateRequestsinfo(msg.getReceiveTime());
				popularityinfoList.get(contentid).updateContentpopularityinfo(this.totalrequests);
			}

			else {
				popularityinfoList.get(contentid).updateRequestsinfo(msg.getReceiveTime());
				popularityinfoList.get(contentid).updateContentpopularityinfo(this.totalrequests);
			}

//			System.out.println(msg.getId()+" msgfrom: "+msg.getFrom()+" msgto: "+msg.getTo()+" requestwhat:"
//					+msg.getMessageContentName()+" finddatain: "+this.getHost()+"content: "+
//					popularityinfoList.get(contentid).getContentidfrompoplist()+" "+popularityinfoList.get(contentid).Requestsinfo
//					+" thiscontent_popularity_thishost:"+popularityinfoList.get(contentid).getContentpopularity()+" "+"totalrequest"+this.totalrequests);
		}


	//更新自己的缓存
	private void updatebuffercontent(){

		//得到本地的最流行内容
		Map<Integer,Double> contentidcollection=getpopularestcontentid(popularityinfoList);
		Map<Integer,Double> sortedcontentidcollection=sortDescend(contentidcollection);
		int i=1;
		for(Map.Entry<Integer,Double>entry:sortedcontentidcollection.entrySet()){
			if(this.iscachenablednode()){
				if(i>BufferSize){
					break;
				}
				else {
					buffercontent.remove(0);
					buffercontent.add(entry.getKey());
					i++;
				}
			}

		}
	}

	//得到邻居节点的流行度表
	public Map<DTNHost,Map<Integer,Popularityinfo>> getneightotallist(){
		Map<DTNHost,Map<Integer,Popularityinfo>> neighbortatallist=new HashMap<DTNHost,Map<Integer,Popularityinfo>>();
		List<DTNHost>  neighborhosts=this.getHost().getneigh();
		for(DTNHost host:neighborhosts){
			if(host.getRouter().iscachenablednode()){
				neighbortatallist.put(host,host.getRouter().getPopularityinfoList());
			}
		}
		return neighbortatallist;
	}

	//返回最受欢迎的内容及其对应的流行度
	public Map<Integer,Double> getpopularestcontentid(Map<Integer,Popularityinfo> poplist){
		Map<DTNHost,Map<Integer,Popularityinfo>> neighlist=new HashMap<DTNHost,Map<Integer,Popularityinfo>>();
		// Map<DTNHost,Map<Integer,Popularityinfo>> neighlist=getneightotallist();
		//Map<DTNHost,Map<Integer,Popularityinfo>> neighlist=this.getHost().totallist;
		//加上自己的流行度表
		neighlist.put(this.getHost(),poplist);
		List<Integer> mostpopularcontent=new ArrayList<Integer>();
		Map<Integer,Integer> popularestcontent= new HashMap<Integer,Integer>();
		for(Map<Integer,Popularityinfo> popularityinfo:neighlist.values()){
			for(Integer contentid:popularityinfo.keySet()){
				if(popularestcontent.containsKey(contentid)){
					int requesttimes=popularestcontent.get(contentid)+popularityinfo.get(contentid).getContentrequeststimes();
					popularestcontent.put(contentid,requesttimes);
				}
				else {
					popularestcontent.put(contentid,popularityinfo.get(contentid).getContentrequeststimes());
				}

			}
		}
		Map<Integer,Double> popularestcontentpopularity=new HashMap<Integer,Double>();
		double totalreq=0;
		for(Integer contentid:popularestcontent.keySet()){
			totalreq+=popularestcontent.get(contentid);

		}
		if(totalreq==0){
			for(Integer contentid:popularestcontent.keySet()){
				double contentpopularity= 0.0;
				popularestcontentpopularity.put(contentid,contentpopularity);
			}
		}
		else{
			for(Integer contentid:popularestcontent.keySet()){
				double contentpopularity= popularestcontent.get(contentid)/totalreq;
				popularestcontentpopularity.put(contentid,contentpopularity);
			}
		}

		Map<Integer,Double> temp=sortDescend(popularestcontentpopularity);
		for(Integer contentid:temp.keySet()) {
			mostpopularcontent.add(contentid);
		}

		return popularestcontentpopularity;

	}

	//依照map中的vlaue进行降序排序
	public static <K, V extends Comparable<? super V>> Map<K, V> sortDescend(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				int compare = (o1.getValue()).compareTo(o2.getValue());
				return -compare;
			}
		});

		Map<K, V> returnMap = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			returnMap.put(entry.getKey(), entry.getValue());
		}
		return returnMap;
	}

	//依照map中的value进行升序排序
	public static <K1, K2, V> Map<K1, Map<K2, V>> deepCopy(Map<K1, Map<K2, V>> original){
		Map<K1, Map<K2, V>> copy = new HashMap<K1, Map<K2, V>>();
		for(Map.Entry<K1, Map<K2, V>> entry : original.entrySet()){
			copy.put(entry.getKey(), new HashMap<K2, V>(entry.getValue()));
		}
		return copy;
	}

	//使用对象的序列化进而实现深拷贝,
	private <T extends Serializable> T clone(T obj) {
		T cloneObj = null;
		try {
			ByteOutputStream bos = new ByteOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(obj);
			oos.close();
			ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bis);
			cloneObj = (T) ois.readObject();
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cloneObj;
	}

	//收集全网的流行度表，这里用的是深拷贝！非常关键
	public void collectpoplist(){

		for(DTNHost host1:this.host.getHostsList()){
			if(host1.getRouter().iscachenablednode()){
//				Popularityinfo tempinfo=clone(host1.getRouter().getPopularityinfoList());
//				clone(templist);
				totalpoplist.put(host1,host1.getRouter().getPopularityinfoListfortotalpoplist());
				//totalpoplist.put(host1,host1.getRouter().getPopularityinfoList());
			}
		}

		//totalpoplist=deepCopy(templist);
	}


	//更新feedback信息
	public void updatefeedbackinfo(){
		Map<Integer,Double> temppopularconten=new HashMap<Integer,Double>();
		temppopularconten=getpopularestcontentid(lasttimepopularinfolist);
		temppopularconten=sortDescend(temppopularconten);
		Iterator<Map.Entry<Integer,Double>> it=temppopularconten.entrySet().iterator();
		int rank=1;
		double[] stillinbuff = new double[feedbackinfolength];
		while(it.hasNext()&&rank<=200){
			Map.Entry<Integer,Double> entry=it.next();
			int contentid=entry.getKey();

			if(rank<=0.1*BufferSize){
				if(buffercontent.contains(contentid)){
					stillinbuff[0]++;
				}
			}
			else if(rank>0.1*BufferSize&&rank<=0.2*BufferSize){
				if(buffercontent.contains(contentid)){
					stillinbuff[1]++;

				}
			}
			else if(rank>0.2*BufferSize&&rank<=0.3*BufferSize){
				if(buffercontent.contains(contentid)){
					stillinbuff[2]++;

				}
			}
			else if(rank>0.3*BufferSize&&rank<=0.4*BufferSize){
				if(buffercontent.contains(contentid)){
					stillinbuff[3]++;

				}
			}
			else if(rank>0.4*BufferSize&&rank<=0.5*BufferSize){
				if(buffercontent.contains(contentid)){
					stillinbuff[4]++;

				}
			}
			else if(rank>0.5*BufferSize&&rank<=0.6*BufferSize){
				if(buffercontent.contains(contentid)){
					stillinbuff[5]++;

				}
			}
			else if(rank>0.6*BufferSize&&rank<=0.7*BufferSize){
				if(buffercontent.contains(contentid)){
					stillinbuff[6]++;

				}
			}
			else if(rank>0.7*BufferSize&&rank<=0.8*BufferSize){
				if(buffercontent.contains(contentid)){
					stillinbuff[7]++;

				}
			}
			else if(rank>0.8*BufferSize&&rank<=0.9*BufferSize){
				if(buffercontent.contains(contentid)){
					stillinbuff[8]++;

				}
			}
			else if(rank>0.9*BufferSize){
				if(buffercontent.contains(contentid)){
					stillinbuff[9]++;

				}
			}
			rank++;
		}

        double [] tempfeedbackinfo=feedbackinfo;
		if(!lasttimepopularinfolist.isEmpty()){
			for(int i=0;i<feedbackinfolength;i++){
				feedbackinfo[i]=((stillinbuff[i]/BufferSize*feedbackinfolength)+tempfeedbackinfo[i])/2;
			}
		}

		lasttimepopularinfolist.putAll(popularityinfoList);
	}

	//返回上一周期各个排名分段的内容现在依旧被缓存的比例
	public double gettherelationbetweenpoplistandrank(double[] feedinfo,int rank){
		double p=0;
        if(rank<=0.1*BufferSize){
           p=feedinfo[0];
        }
        else if(rank>0.1*BufferSize&&rank<=0.2*BufferSize){
            p=feedinfo[1];
        }
        else if(rank>0.2*BufferSize&&rank<=0.3*BufferSize){
            p=feedinfo[2];
        }
        else if(rank>0.3*BufferSize&&rank<=0.4*BufferSize){
            p=feedinfo[3];
        }
        else if(rank>0.4*BufferSize&&rank<=0.5*BufferSize){
            p=feedinfo[4];
        }
        else if(rank>0.5*BufferSize&&rank<=0.6*BufferSize){
            p=feedinfo[5];
        }
        else if(rank>0.6*BufferSize&&rank<=0.7*BufferSize){
            p=feedinfo[6];
        }
        else if(rank>0.7*BufferSize&&rank<=0.8*BufferSize){
            p=feedinfo[7];
        }
        else if(rank>0.8*BufferSize&&rank<=0.9*BufferSize){
            p=feedinfo[8];
        }
        else if(rank>0.9*BufferSize&&rank<BufferSize){
            p=feedinfo[9];
        }
		return p;
	}

	//计算在该节点上的平均排队时间
	public double getaveragequetime(){
		int length=0;
		double totalque=0;
		double[] totalqueuetime=new double[queuetimesize];
		for(Tuple<String,Double> tuple:comingtoqueuetime){
			String msgid=tuple.getKey();
			double comingtime=tuple.getValue();

				Tuple<String,Double> tuple1=new Tuple<String,Double>(msgid,comingtime);
				for(Tuple<String,Double> tupletemp:leavetoqueuetime){
					if (tupletemp.getKey()==msgid){
						tuple1=tupletemp;
						break;
					}
				}
				//记录每个message的排队延迟
			if(length>=queuetimesize-1){
				totalqueuetime[length%(queuetimesize-1)]=tuple1.getValue()-tuple.getValue();
			}
			else {
				totalqueuetime[length++]=tuple1.getValue()-tuple.getValue();
			}

		}

		for(Double t1:totalqueuetime){
			totalque+=t1;
		}

		return totalque/totalqueuetime.length;
	}

	//计算某一内容的到达速率
	public  double getlamta_content(Map<Integer,Popularityinfo> poplist,Integer contentid){
		Popularityinfo popinfo=poplist.get(contentid);
		int T1=(T-2)*collectpoplistinterval;
		return popinfo.getlamtaofcontent(T1);
	}

	//计算节点在上一个流行度分发时的所有请求数
	public int gettotalrequestuntilllast_T(Map<Integer,Popularityinfo> poplist){
		int totalrequestuntilllast_T=0;
		for(Popularityinfo popinfo:poplist.values()){
			totalrequestuntilllast_T+=popinfo.getContentrequeststimes();
		}

		return totalrequestuntilllast_T;
	}

	//请求的平均到达速率
	public double getlamta_average(DTNHost host){
		return host.getRouter().lamta_average;
	}

	//判断是否启用缓存功能的节点
	public boolean iscachenablednode(){
		if(this.getHost().getAddress()>=cacheenablednodemin&&this.getHost().getAddress()<=cacheenablednodemax){
			return true;
		}
		else return false;
	}
   /**tangjin*/


}
