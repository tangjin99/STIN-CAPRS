/*
 * Copyright 2017 University of Science and Technology of China , Infonet Lab
 * Written by LiJian.
 */
package routing;

import java.util.*;

import util.Tuple;
import routing.SatelliteInterLinkInfo.GEOclusterInfo;
import routing.SatelliteInterLinkInfo.LEOclusterInfo;
import routing.SatelliteInterLinkInfo.MEOclusterInfo;
import core.*;
import movement.MovementModel;
import movement.SatelliteMovement;
import static core.SimClock.getTime;
import static java.lang.Math.abs;


public class DynamicMultiLayerSatelliteRouter extends ActiveRouter {
    /**
     * Label indicates that the message can wait for next hop coming or not -setting id ({@value})
     */
    public static final String MSG_WAITLABEL = "waitLabel";
    /**
     * Label indicates that routing path can contain in the message or not -setting id ({@value})
     */
    public static final String MSG_PATHLABEL = "msgPathLabel";
    /**
     * Router path -setting id ({@value})
     */
    public static final String MSG_ROUTERPATH = "routerPath";
    /**
     * Group name in the group -setting id ({@value})
     */
    public static final String GROUPNAME_S = "Group";
    /**
     * Interface name in the group -setting id ({@value})
     */
    public static final String INTERFACENAME_S = "Interface";
    /**
     * Transmit range -setting id ({@value})
     */
    public static final String TRANSMIT_RANGE_S = "transmitRange";
    /**
     * Cluster check interval -setting id ({@value})
     */
    public static final String CLUSTERCHECKINTERVAL_S = "clusterCheckInterval";
    /**
     * Check interval between MEO nodes -setting id ({@value})
     */
    public static final String MEOCHECKINTERVAL_S = "MEOCheckInterval";
    /**
     * The size of confirm message -setting id ({@value})
     */
    public static final String COMFIRMMESSAGESIZE_S = "comfirmMessageSize";
    /**
     * The TTL of confirm message -setting id ({@value})
     */
    public static final String COMFIRMTTL_S = "comfirmTtl";
    /**
     * Decides the message transmitted through radio link or laser link
     * according to this message size threshold�� -setting id ({@value})
     */
    public static final String MSG_SIZE_THRESHOLD_S = "MessageThreshold";
    /** indicates the type of link*/
    public static final String LASER_LINK = "LaserInterface";
    /** indicates the type of link*/
	public static final String RADIO_LINK = "RadioInterface";
    /** light speed��approximate 3*10^8m/s */
    private static final double LIGHTSPEED = 299792458;

    /** indicate the transmission radius of each satellite -setting id ({@value} */
    private static double transmitRange;
    /** label indicates that routing path can contain in the message or not -setting id ({@value} */
    private static boolean msgPathLabel;
    /** indicates the TTL of confirm message -setting id ({@value} */
    private static int confirmTtl;
    /** the message size threshold, decides the message transmitted 
     *  through radio link or laser link -setting id ({@value}*/
    private static int msgThreshold;
    /** the optimized label, if it turns on, all routing will use the 
     * shortest path search, which is optimized but slow -setting id ({@value}*/
    private static boolean OptimizedRouting;
    
    /** label indicates that the static routing parameters are set or not */
    private static boolean initLabel = false;
    /** to make the random choice */
    private static Random random;
    
    /** total number of LEO satellites*/
    private static int LEO_TOTAL_SATELLITES;//�ܽڵ���
    /** total number of LEO plane*/
    private static int LEO_TOTAL_PLANE;//�ܹ��ƽ����
    /** number of hosts in each LEO plane*/
    private static int LEO_NROF_S_EACHPLANE;//ÿ��ƽ���ϵ�������
    
    /** total number of MEO satellites*/
    private static int MEO_TOTAL_SATELLITES;//�ܽڵ���
    /** total number of MEO plane*/
    private static int MEO_TOTAL_PLANE;//�ܹ��ƽ����
    /** number of hosts in each MEO plane*/
    private static int MEO_NROF_S_EACHPLANE;//ÿ��ƽ���ϵ�������
    
    /** total number of GEO satellites*/
    private static int GEO_TOTAL_SATELLITES;//�ܽڵ���
    /** total number of GEO plane*/
    private static int GEO_TOTAL_PLANE;//�ܹ��ƽ����
    /** number of hosts in each GEO plane*/
    private static int GEO_NROF_S_EACHPLANE;//ÿ��ƽ���ϵ�������

    /** label indicates that if LEO_MEOClustering is initialized*/
    private static boolean LEO_MEOClusteringInitLable;

    /** label indicates that routing algorithm has been executed or not at this time */
    private boolean routerTableUpdateLabel;
    /** maintain the earliest arrival time to other nodes */
    private HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
    /** the router table comes from routing algorithm */
    private HashMap<DTNHost, List<Tuple<Integer, Boolean>>>
            routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();
	/** number of different interface*/
    public int nrofRadioInterface;
	public int nrofSendingLaserInterface;
	public int nrofSendingRadioInterface;
	

    public DynamicMultiLayerSatelliteRouter(Settings s) {
        super(s);
    }

    protected DynamicMultiLayerSatelliteRouter(DynamicMultiLayerSatelliteRouter r) {
        super(r);
    }

    @Override
    public MessageRouter replicate() {
        return new DynamicMultiLayerSatelliteRouter(this);
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
        super.init(host, mListeners);
        
        Settings s1 = new Settings("Interface1");
        nrofRadioInterface = s1.getInt("nrofRadioInterface");
        
        if (!initLabel){ 
        	//LEO
            Settings sat = new Settings("Group");
            LEO_TOTAL_SATELLITES = sat.getInt("nrofLEO");//�ܽڵ���
            LEO_TOTAL_PLANE = sat.getInt("nrofLEOPlanes");//�ܹ��ƽ����
            LEO_NROF_S_EACHPLANE = LEO_TOTAL_SATELLITES/LEO_TOTAL_PLANE;//ÿ�����ƽ���ϵĽڵ���
            //MEO
            Settings s = new Settings("Group");
            if (s.getBoolean("EnableMEO")){
                MEO_TOTAL_SATELLITES = s.getInt("nrofMEO");//�ܽڵ���
                MEO_TOTAL_PLANE = s.getInt("nrofMEOPlane");//�ܹ��ƽ����
                MEO_NROF_S_EACHPLANE = MEO_TOTAL_SATELLITES/MEO_TOTAL_PLANE;//ÿ�����ƽ���ϵĽڵ���
            }
            //GEO
            if (s.getBoolean("EnableGEO")){
                GEO_TOTAL_SATELLITES = s.getInt("nrofGEO");//�ܽڵ���
                GEO_TOTAL_PLANE = s.getInt("nrofGEOPlane");//�ܹ��ƽ����
                GEO_NROF_S_EACHPLANE = GEO_TOTAL_SATELLITES/GEO_TOTAL_PLANE;//ÿ�����ƽ���ϵĽڵ���
            }
                        
            random = new Random();
            s.setNameSpace(INTERFACENAME_S);
            transmitRange = s.getInt(TRANSMIT_RANGE_S);
            msgThreshold = s.getInt(MSG_SIZE_THRESHOLD_S);
            
            s.setNameSpace(GROUPNAME_S);
            msgPathLabel = s.getBoolean(MSG_PATHLABEL);
            confirmTtl = s.getInt(COMFIRMTTL_S);
                    
            s.setNameSpace("DynamicMultiLayerSatelliteRouter");
            OptimizedRouting = s.getBoolean("Optimized");
            initLabel = true;
        }
    }
    /**
     * ��NetworkInterface����ִ����·�жϺ���disconnect()�󣬶�Ӧ�ڵ��router���ô˺���
     */
    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);
//		System.out.println("message: "+con);
//		if (!con.isUp()){
//			if(con.isTransferring()){
//				if (con.getOtherNode(this.getHost()).getRouter().isIncomingMessage(con.getMessage().getId()))
//					con.getOtherNode(this.getHost()).getRouter().removeFromIncomingBuffer(con.getMessage().getId(), this.getHost());
//				super.addToMessages(con.getMessage(), false);//������Ϊ��·�ж϶���ʧ����Ϣ�����·Żط��ͷ��Ķ����У�����ɾ���Է��ڵ��incoming��Ϣ
//				System.out.println("message: "+con.getMessage());
//			}
//		}
    }

    @Override
    public void update() {
        super.update();
       
        //����������Ϣ��LEO���з��飬��ȷ������LEO�Ĺ̶�����MEO�ڵ㣬�Ӷ�������������ж�̬�ִ�
//        if (!LEO_MEOClusteringInitLable)
//            initLEO_MEOClusteringRelationship();
        
        //update dynamic clustering information
        if (!clusteringUpdate()){
            //TODO deal with isolate LEO node
            return; // for isolate LEO node, it does noting
        }

//        if (isTransferring()) { // judge the link is occupied or not
//            return; // can't start a new transfer
//        }
        
        //helloProtocol();//ִ��hello����ά������
        if (!canStartTransfer())
            return;

        /** Set router update label to make sure that routing algorithm only execute once at a time */
        routerTableUpdateLabel = false;

        /** sort the messages to transmit */
        List<Message> messageList = this.CollectionToList(this.getMessageCollection());
        List<Message> messages = sortByQueueMode(messageList);

        // try to send the message in the message buffer
        for (Message msg : messages) {
        	if (checkMsgShouldGetRoutingOrNot(msg) == false) 
        		continue;
//            //Confirm message's TTL only has 1 minutes, will be drop by itself
//            if (msg.getId().contains("Confirm") || msg.getId().contains("ClusterInfo"))
//                continue;
            if (findPathToSend(msg) == true)
                return;
        }

    }
    
    /**
     * Try to send the message through a specific connection.
     *
     * @param t
     * @return
     */
    public boolean sendMsg(Tuple<Message, Connection> t) {    	
        if (t == null || t.getValue() == null) {
            //throw new SimError("send msg error!");
            return false;
        } else {
        	// �ж�����ӿڸ���������ͬʱ������·��ĿҪ��
        	nrofSendingLaserInterface = 0;	nrofSendingRadioInterface = 0; 
        	for(Connection con : this.sendingConnections ){       		
        		if(con.getLinkType().equals("RadioInterface")){
        			nrofSendingRadioInterface++;
        		}else if(con.getLinkType().equals("LaserInterface")){
        			nrofSendingLaserInterface++;
        		}
        	}
        	if(t.getValue().getLinkType().equals("RadioInterface")){
        		if(nrofSendingRadioInterface>=nrofRadioInterface){
            		return false;
        		}
        	}else if(t.getValue().getLinkType().equals("LaserInterface")){
        		if(nrofSendingLaserInterface>=1){
            		return false;
        		}
        	}
        	
            if (tryMessageToConnection(t) != null)//�б��һ��Ԫ�ش�0ָ�뿪ʼ������
                return true;//ֻҪ�ɹ���һ�Σ�������ѭ��
            else
                return false;
        }
    }
    
    /**
     * Returns true if this router is transferring something at the moment or
     * some transfer has not been finalized.
     *
     * @return true if this router is transferring something
     */
    @Override
    public boolean isTransferring() {
        //�жϸýڵ��ܷ���д�����Ϣ�������������һ�����ϵģ�ֱ�ӷ��أ�������,�������ŵ��ѱ�ռ�ã�
        //����1�����ڵ��������⴫��
        if (this.sendingConnections.size() > 0) {//protected ArrayList<Connection> sendingConnections;
            return true; // sending something
        }        
        List<Connection> connections = getConnections();
        //����2��û���ھӽڵ�
        if (connections.size() == 0) {
            return false; // not connected
        }
        //����3�����ھӽڵ㣬����������Χ�ڵ����ڴ���
        //ģ�������߹㲥��·�����ھӽڵ�֮��ͬʱֻ����һ�Խڵ㴫������!
        for (int i = 0, n = connections.size(); i < n; i++) {
            Connection con = connections.get(i);
            //isReadyForTransfer����false���ʾ���ŵ��ڱ�ռ�ã���˶��ڹ㲥�ŵ����Բ��ܴ���
            if (!con.isReadyForTransfer()) {
                return true;    // a connection isn't ready for new transfer
            }
        }
        return false;
    }
    
    /**
     * check if msg is being sending or not
     * @param msg
     * @return
     */
    public boolean checkMsgShouldGetRoutingOrNot(Message msg){
    	for (Connection con : this.sendingConnections){
    		if (msg.getId().contains(con.getMessage().getId())){
    			//throw new SimError("error" );
    			return false;//this msg shouldn't be sended again
    		}
    	}
    	return true;
    }
    
    /** transform the message Collection to List
     * @param messages
     * @return
     */
    public List<Message> CollectionToList(Collection<Message> messages){
        List<Message> forMsg = new ArrayList<Message>();
        for (Message msg : messages) {	//���Է��Ͷ��������Ϣ
            forMsg.add(msg);
        }
        return forMsg;
    }

    /**
     * Creates a new Confirm message to the router.
     * The TTL of confirm message setting is different from normal message.
     * @param m The message to create
     * @return True if the creation succeeded, false if not (e.g.
     * the message was too big for the buffer)
     */
    public boolean createNewMessage(Message m, int Ttl) {
        m.setTtl(Ttl);
        addToMessages(m, true);
        return true;
    }

    /**
     * update clustering information
     */
    public boolean clusteringUpdate(){
    	return this.getSatelliteLinkInfo().clusteringUpdate();
    }
    /**
     * periodically send hello packet to neighbor satellite nodes to check their status
     */
    public void helloProtocol(){
        // TODO helloProtocol
    }
    
    /**
     * Update router table, find a routing path and try to send the message
     * @param msg
     * @return
     */
    public boolean findPathToSend(Message msg) {
        if (msgPathLabel == true) {//�����������Ϣ��д��·����Ϣ
            if (msg.getProperty(MSG_ROUTERPATH) == null) {//ͨ����ͷ�Ƿ���д��·����Ϣ���ж��Ƿ���Ҫ��������·��(ͬʱҲ������Ԥ��Ŀ���)
                Tuple<Message, Connection> t =
                        findPathFromRouterTabel(msg);
                return sendMsg(t);
            } else {//������м̽ڵ㣬�ͼ����Ϣ������·����Ϣ
                Tuple<Message, Connection> t =
                        findPathFromMessage(msg);
                assert t != null : "��ȡ·����Ϣʧ�ܣ�";
                return sendMsg(t);
            }
        } else {
            //don't write the routing path into the header
            //routing path will be calculated in each hop
            Tuple<Message, Connection> t =
                    findPathFromRouterTabel(msg);
            return sendMsg(t);
        }
    }

    /**
     * Try to read the path information stored in the header.
     * If the operation fails, the routing table should be re-calculated.
     * @param msg
     * @return
     */
    public Tuple<Message, Connection> findPathFromMessage(Message msg) {
        List<Tuple<Integer, Boolean>> routerPath = null;
        if (msg.getProperty(MSG_ROUTERPATH) instanceof List){
            routerPath = (List<Tuple<Integer, Boolean>>) msg.getProperty(MSG_ROUTERPATH);
        }
        int thisAddress = this.getHost().getAddress();
        if (msg.getTo().getAddress() == thisAddress){
            throw new SimError("Message: " + msg +
                    " already arrive the destination! " + this.getHost());
        }
        if (routerPath == null)
            return null;

        //try to find the next hop from routing path in the message header
        int nextHopAddress = -1;
        boolean waitLable = false;
        for (int i = 0; i < routerPath.size(); i++) {
            if (routerPath.get(i).getKey() == thisAddress) {
            	if (routerPath.size() == i + 1){
            		msg.removeProperty(MSG_ROUTERPATH);
            		return null;
            	}
                nextHopAddress = routerPath.get(i + 1).getKey();//�ҵ���һ���ڵ��ַ
                waitLable = routerPath.get(i + 1).getValue();//�ҵ���һ���Ƿ���Ҫ�ȴ��ı�־λ
                break;
            }
        }

        if (nextHopAddress > -1) {
            Connection nextCon = findConnection(nextHopAddress, msg);
            //the routing path in the message header could be invaild
            if (nextCon == null) {
                if (!waitLable) {
                    msg.removeProperty(MSG_ROUTERPATH);
                    //try to re-routing
                    Tuple<Message, Connection> t =
                            findPathFromRouterTabel(msg);
                    return t;
                }
            } else {
                Tuple<Message, Connection> t = new
                        Tuple<Message, Connection>(msg, nextCon);
                return t;
            }
        }
        msg.removeProperty(MSG_ROUTERPATH);
        return null;
    }

    /**
     * Try to update router table and find the routing path from router table.
     * If 'msgPathLabel' is true, then the routing path should be written into the header.
     * @param message
     * @return
     */
    public Tuple<Message, Connection> findPathFromRouterTabel(Message message) {
        //update router table by using specific routing algorithm
        if (updateRouterTable(message) == false) {
            return null;
        }
        //get the routing path from router table
        List<Tuple<Integer, Boolean>> routerPath =
                this.routerTable.get(message.getTo());

        //write the routing path into the header
        //or not according to the 'msgPathLabel'
        if (msgPathLabel == true) {
            message.updateProperty(MSG_ROUTERPATH, routerPath);
        }
        
        Connection firstHop = findConnection(routerPath.get(0).getKey(), message);
        if (firstHop != null) {
            Tuple<Message, Connection> t =
                    new Tuple<Message, Connection>(message, firstHop);
            return t;
        } else {
            if (routerPath.get(0).getValue()) {
                return null;
            } else {
                //TODO
//                throw new SimError("No such connection: " + routerPath.get(0) +
//                       " at routerTable " + this);
                this.routerTable.remove(message.getTo());
                return null;
            }
        }
    }

    /**
     * Find the DTNHost according to its address
     *
     * @param address
     * @return
     */
    public DTNHost findHostByAddress(int address) {
        for (DTNHost host : getHosts()) {
            if (host.getAddress() == address)
                return host;
        }
        return null;
    }

    /**
     * Find the connection according to DTNHost's address
     * @param address
     * @return
     */
    public Connection findConnectionByAddress(int address) {
        for (Connection con : this.getHost().getConnections()) {
            if (con.getOtherNode(this.getHost()).getAddress() == address)
                return con;
        }
        return null;
    }

    /**
     * Update the router table
     *
     * @param msg
     * @return
     */
    public boolean updateRouterTable(Message msg) {
        switch (getSatelliteType()){
            case "LEO":{
                LEOshortestPathSearch(msg);
                break;
            }
            case "MEO":{
                MEOroutingPathSearch(msg);
                break;
            }
            case "GEO":{
            	//TODO
            	GEOroutingPathSearch(msg);
            	break;
            }
        }

        if (this.routerTable.containsKey(msg.getTo())) {
//            System.out.println("find the path!  " +
//            		this.routerTable.get(msg.getTo())+"   "+ getSatelliteType() 
//            				+" to "+ msg.getTo().getSatelliteType()+"  " + msg);
            return true;
        } else {
            return false;
        }
    }
    /**
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination
     *
     * @param msg
     */
    public void LEOshortestPathSearch(Message msg) {
    	LEOclusterInfo LEOci = this.getSatelliteLinkInfo().getLEOci();
    	
        DTNHost to = msg.getTo();// get destination
        switch (to.getSatelliteType()){
            case "LEO":{
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, this.getHosts());
                	return;
                }
                //Ŀ�Ľڵ��Ƿ��������������ƽ����
                if (LEOci.getAllHostsInSamePlane().contains(to)){ 
//                	System.out.println(this.getHost() +"  "+ to );
                    findPathInSameLEOPlane(this.getHost(), to);
                }
                else{
                	//���ڷ�������ͬһ���ƽ���ϵ���Ϣ��ͳһ�����������ͨ�Žڵ�
                	//1����Ϊͨ�Žڵ�
                	if (this.getHost().getRouter().CommunicationSatellitesLabel){
                        //���Ŀ�Ľڵ��Ƿ����ھӹ��ƽ����
                        List<DTNHost> hostsInNeighborOrbitPlane = LEOci.ifHostsInNeighborOrbitPlane(to);
                        if (hostsInNeighborOrbitPlane != null){//��Ϊ�գ���˵�����ھӹ���ϣ��ҷ��ص����ھӹ�������нڵ�
                        	//�ȳ���ͨ���ھӹ��ͨ�Žڵ�ת��
                        	if(msgFromLEOForwardToNeighborPlane(msg, to))
                        		return;
                        }
                        //����ֱ��ͨ��MEO����ڵ�ת��
                        msgFromCommunicationLEOForwardedByMEO(msg, to);
                    	
                	}
                	//2����ΪLEOң�нڵ㣬ֱ���Ȱ����ݴ���ͨ�Žڵ���
                	else{
                    	DTNHost communicationLEO = findNearestCommunicationLEONodes(this.getHost());
                    	List<Tuple<Integer, Boolean>> path = findPathInSameLEOPlane(this.getHost(), communicationLEO);
                    	
                        if (!path.isEmpty()){
//                        	System.out.println("�Ƚ���ͨ��LEO�ڵ����ת��   to" + to);
                            routerTable.put(to, path);
                        }
                	}
                }
                break;
            }
            case "MEO":{
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, this.getHosts());
                	return;
                }
                
            	if (this.getHost().getRouter().CommunicationSatellitesLabel){
            		//���������ͨ�Žڵ㣬���ȷ��͵����Լ������ӵ�MEO�ڵ���
            		//TODO
                   	List<DTNHost> searchArea = new ArrayList<DTNHost>();                  	
                   	searchArea.addAll(findMEOHosts());
                   	searchArea.add(this.getHost());
                   	//this.getMEOtoMEOTopology();
                	optimzedShortestPathSearch(msg, searchArea);
//                	if (this.routerTable.get(to) != null)
//                		System.out.println(this.getHost()+" �ҵ���LEO to MEO ��·��"+msg);
            	}
            	//��ΪLEOң�нڵ㣬ֱ���Ȱ����ݴ���ͨ�Žڵ���
            	else{
                	DTNHost communicationLEO = findNearestCommunicationLEONodes(this.getHost());
                	List<Tuple<Integer, Boolean>> path = findPathInSameLEOPlane(this.getHost(), communicationLEO);
                	
                    if (!path.isEmpty()){
//                    	System.out.println("�Ƚ���ͨ��LEO�ڵ����ת��   to" + to);
                        routerTable.put(to, path);
                    }
            	}                                          
                break;
            }
            case "GEO":{
            	//TODO
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, this.getHosts());
                	return;
                }
                if (to.getRouter().CommunicationSatellitesLabel){
                    //TODO ����LEO��GEO������
                    HashMap<DTNHost, List<DTNHost>> topologyInfo = 
                    		getGEOtoLEOTopology(msg, to, this.getHost());//optimizedTopologyCalculation(MEOci.MEOList);//localTopologyCalculation(MEOci.MEOList);          
                    //���������㷨
                    DTNHost nearestCLEO = findNearestCommunicationLEONodes(this.getHost());
                    List<DTNHost> localHostsList = new ArrayList<DTNHost>();
                    localHostsList.addAll(findGEOHosts());
                    localHostsList.addAll(findMEOHosts());
                    localHostsList.add(nearestCLEO);
                	this.shortestPathSearch(msg, topologyInfo, localHostsList);
//                	if (this.routerTable.containsKey(to))
//                		System.out.println("LEO to GEO" + this.getHost() + "�ҵ������·��");
                	
                }
                //�����Ƚ���ͨ�Žڵ�
                else{
                	DTNHost communicationLEO = findNearestCommunicationLEONodes(this.getHost());
                	List<Tuple<Integer, Boolean>> path = findPathInSameLEOPlane(this.getHost(), communicationLEO);
                	
                    if (!path.isEmpty()){
//                    	System.out.println("�Ƚ���ͨ��LEO�ڵ����ת��   to" + to);
                        routerTable.put(to, path);
                    }
                }
            	break;
            }
        }
    }
    /**
     * Core routing logic for MEO satellite
     * @param msg
     */
    public void MEOroutingPathSearch(Message msg) {
    	MEOclusterInfo MEOci = this.getSatelliteLinkInfo().getMEOci();   	
        DTNHost to = msg.getTo();// get destination
        switch (to.getSatelliteType()){
            case "LEO":{
            	  if (OptimizedRouting){
                  	optimzedShortestPathSearch(msg, this.getHosts());
                  	return;
            	  }
                //Ŀ�Ľڵ��Ƿ��ǹ���ڵ�
            	if (to.getRouter().CommunicationSatellitesLabel){
            		//ͨ��MEO�ڵ�ֱ�Ӵ���LEO
            		 HashMap<DTNHost, List<DTNHost>> topologyInfo = getMEOtoCommunicationLEOTopology(msg, to);//��������
            		 //�޶������Ľڵ㷶Χ
            		 List<DTNHost> localHostsList = new ArrayList<DTNHost>(findMEOHosts());
            		 localHostsList.add(to);
            		 shortestPathSearch(msg, topologyInfo, localHostsList);
            	}
            	else{
            		DTNHost nearestCLEO = findNearestCommunicationLEONodes(to);
	            	//�ȷ���Ŀ��LEO�����ͨ��LEO�ڵ��ϣ����������д���
	           		HashMap<DTNHost, List<DTNHost>> topologyInfo = getMEOtoCommunicationLEOTopology(msg, nearestCLEO);//��������
	           		//�޶������Ľڵ㷶Χ
	           		List<DTNHost> localHostsList = new ArrayList<DTNHost>(findMEOHosts());
	           		localHostsList.add(nearestCLEO);
	           		shortestPathSearch(msg, topologyInfo, localHostsList);
	           		
	            	if (this.routerTable.containsKey(nearestCLEO)){
//	            		System.out.println("������ͨ��MEOת�������·���� to" + to);
	            		this.routerTable.put(to, this.routerTable.get(nearestCLEO));//���ȥĿ�Ľڵ��·��
	            		return;
	            	}  
            	}
                break;
            }
            case "MEO":{
            	List<DTNHost> allMEOandGEO = new ArrayList<DTNHost>();
            	allMEOandGEO.addAll(findMEOHosts());
            	allMEOandGEO.addAll(findGEOHosts());
            	
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, allMEOandGEO);
                	return;
                }
                //��������·���㷨���������ⳡ������Ҫָ������Դ�ڵ㣬��������������
                HashMap<DTNHost, List<DTNHost>> topologyInfo = getMEOtoMEOTopology();//��������
                //TODO ������Ӧ�����GEO�ڵ�
                //shortestPathSearch(msg, topologyInfo, allMEOandGEO);    
                shortestPathSearch(msg, topologyInfo, findMEOHosts()); 
                break;
            }
            case "GEO":{
               	List<DTNHost> allMEOandGEO = new ArrayList<DTNHost>();
            	allMEOandGEO.addAll(findMEOHosts());
            	allMEOandGEO.addAll(findGEOHosts());
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, allMEOandGEO);
                	return;
                }
              //��������·���㷨���������ⳡ������Ҫָ������Դ�ڵ㣬��������������
                HashMap<DTNHost, List<DTNHost>> topologyInfo = getMEOtoMEOTopology();//��������
                //TODO ����MEO��GEO������
                throw new SimError("MEO to GEO");
                //shortestPathSearch(msg, topologyInfo, allMEOandGEO);  
            }
        }
    }
    /**
     * Core routing logic for GEO satellite
     * @param msg
     */
    public void GEOroutingPathSearch(Message msg) {
        DTNHost to = msg.getTo();// get destination
        switch (to.getSatelliteType()){
            case "LEO":{
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, this.getHosts());
                	return;
                }
                
                //�ҵ�����˽ڵ��MEO�ڵ㣬����������ת��
            	/**�������·�������㷨�ı�����������·��**/          
                HashMap<DTNHost, List<DTNHost>> topologyInfo = 
                		getGEOtoLEOTopology(msg, this.getHost(), to);//optimizedTopologyCalculation(MEOci.MEOList);//localTopologyCalculation(MEOci.MEOList);          
                //���������㷨
                DTNHost nearestCLEO = findNearestCommunicationLEONodes(to);
                List<DTNHost> localHostsList = new ArrayList<DTNHost>();
                localHostsList.addAll(findGEOHosts());
                localHostsList.addAll(findMEOHosts());
                localHostsList.add(nearestCLEO);
            	this.shortestPathSearch(msg, topologyInfo, localHostsList);
//            	if (this.routerTable.containsKey(to))
//            		System.out.println("GEO" + this.getHost() + "�ҵ������·��");
            	
            	if (!to.getRouter().CommunicationSatellitesLabel){
	            	if (this.routerTable.containsKey(nearestCLEO)){
//	            		System.out.println("������ͨ��MEOת�������·���� to" + to);
	            		this.routerTable.put(to, this.routerTable.get(nearestCLEO));//���ȥĿ�Ľڵ��·��
	            		return;
	            	}  
            	}
            	/**�������·�������㷨�ı�����������·��**/
            	
                break;
            }
            case "MEO":{
                if (OptimizedRouting){
                   	List<DTNHost> allMEOandGEO = new ArrayList<DTNHost>();
                	allMEOandGEO.addAll(findMEOHosts());
                	allMEOandGEO.addAll(findGEOHosts());
                	optimzedShortestPathSearch(msg, allMEOandGEO);
                	return;
                }
                //TODO
                //shortestPathSearch(msg, this.getHost(), getGEOtoMEOTopology(this.getHost(), to));
                //shortestPathSearch(msg, MEOci.getMEOList());
                break;
            }
            case "GEO":{ 
                if (OptimizedRouting){
                   	List<DTNHost> allMEOandGEO = new ArrayList<DTNHost>();
                	allMEOandGEO.addAll(findMEOHosts());
                	allMEOandGEO.addAll(findGEOHosts());
                	optimzedShortestPathSearch(msg, allMEOandGEO);
                	return;
                }
                //TODO
            	//shortestPathSearch(msg, this.getHost(), getGEOtoGEOTopology(to));
            	break;
            }
        }
    }
    /**
     * �Ż�������ֱ�Ӷ�ȡ��Ҫ����ڵ��Connection�б��Ӷ����ټ��㿪�����Ż�����Ч��
     * @param allHosts
     */
    public HashMap<DTNHost, List<DTNHost>> optimizedTopologyCalculation(List<DTNHost> allHosts){
        HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();

        //Calculate links between each two satellite nodes
        for (DTNHost h : allHosts) {
        		for (Connection con : h.getConnections()){
        			DTNHost otherNode = con.getOtherNode(h);
                    if (topologyInfo.get(h) == null)
                        topologyInfo.put(h, new ArrayList<DTNHost>());
                    List<DTNHost> neighborList = topologyInfo.get(h);
                    if (neighborList == null) {
                        neighborList = new ArrayList<DTNHost>();
                        neighborList.add(otherNode);
                    } else {
                        neighborList.add(otherNode);
                    }
        		}               
        }
        return topologyInfo;
    }
    /**
     * Return current network topology in forms of current topology graph
     */
    public HashMap<DTNHost, List<DTNHost>> localTopologyGeneration(Message msg, List<DTNHost> allHosts) {
        HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();

        //get all 
        //TODO better
        for (DTNHost h : allHosts) {
        	for (Connection con : h.getConnections()){
        		if (!isRightConnection(msg, con))
        			continue;
        		DTNHost otherNode = con.getOtherNode(h);
                if (topologyInfo.get(h) == null)
                    topologyInfo.put(h, new ArrayList<DTNHost>());
                List<DTNHost> neighborList = topologyInfo.get(h);
                neighborList.add(otherNode);
        	}
        }
        return topologyInfo;
    }
    /**
     * Return current network topology in forms of temporal graph
     */
    public HashMap<DTNHost, List<DTNHost>> localTopologyCalculation(List<DTNHost> allHosts) {
        HashMap<DTNHost, Coord> locationRecord = new HashMap<DTNHost, Coord>();
        HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();

        double radius = transmitRange;//Represent communication Radius

        //Calculate the current coordinate of all satellite nodes in the network
        for (DTNHost h : allHosts) {
            //locationRecord.put(h, movementModel.getCoordinate(h, SimClock.getTime()));
            locationRecord.put(h, h.getLocation());
        }

        //Calculate links between each two satellite nodes
        for (DTNHost h : allHosts) {
            for (DTNHost otherNode : allHosts) {
                if (otherNode == h)
                    continue;
                Coord otherNodeLocation = locationRecord.get(otherNode);
                if (locationRecord.get(h).distance(otherNodeLocation) <= radius) {
                    if (topologyInfo.get(h) == null)
                        topologyInfo.put(h, new ArrayList<DTNHost>());
                    List<DTNHost> neighborList = topologyInfo.get(h);
                    if (neighborList == null) {
                        neighborList = new ArrayList<DTNHost>();
                        neighborList.add(otherNode);
                    } else {
                        neighborList.add(otherNode);
                    }
                }
            }
        }
        return topologyInfo;
    }
 
    /**
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination.
     * It will search the routing path in specific local nodes area.
     * @param msg
     */
    public void shortestPathSearch(Message msg, HashMap<DTNHost, List<DTNHost>> topologyInfo, List<DTNHost> localHostsList) {
        if (localHostsList.isEmpty() || topologyInfo.isEmpty())
            return;

        if (routerTableUpdateLabel == true)
            return;
        this.routerTable.clear();
        this.arrivalTime.clear();

        /**ȫ���Ĵ������ʼٶ�Ϊһ����**/
        double transmitSpeed;
        if(msg.getSize() < msgThreshold ){
        	transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        } else{
        	transmitSpeed = this.getHost().getInterface(2).getTransmitSpeed();
        }
//        double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        /**��ʾ·�ɿ�ʼ��ʱ��**/

        /**�����·��̽�⵽��һ���ھ����񣬲�����·�ɱ�**/
        List<DTNHost> searchedSet = new ArrayList<DTNHost>();
        List<DTNHost> sourceSet = new ArrayList<DTNHost>();
        sourceSet.add(this.getHost());//��ʼʱֻ��Դ�ڵ���
        searchedSet.add(this.getHost());//��ʼʱֻ��Դ�ڵ�

        for (Connection con : this.getHost().getConnections()) {//�����·��̽�⵽��һ���ھӣ�������·�ɱ�
            if (!localHostsList.contains(con.getOtherNode(this.getHost())))
                continue;
            if (!isRightConnection(msg, con))//�ж��Ƿ�����ȷ����·���䱸��ӿں���Ҫ���м��
            	continue;
            DTNHost neiHost = con.getOtherNode(this.getHost());
            sourceSet.add(neiHost);//��ʼʱֻ�б��ڵ����·�ھ�
//            Double time = getTime() + msg.getSize() / this.getHost().getInterface(1).getTransmitSpeed();
            Double time = getTime() + msg.getSize() / transmitSpeed;
            List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
            Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
            path.add(hop);//ע��˳��
            arrivalTime.put(neiHost, time);
            routerTable.put(neiHost, path);
        }
        /**�����·��̽�⵽��һ���ھ����񣬲�����·�ɱ�**/

        int iteratorTimes = 0;
        int size = localHostsList.size();
        boolean updateLabel = true;
        boolean predictLable = false;

        arrivalTime.put(this.getHost(), SimClock.getTime());//��ʼ������ʱ��

        /**���ȼ����У���������**/
        List<Tuple<DTNHost, Double>> PriorityQueue = new ArrayList<Tuple<DTNHost, Double>>();
        //List<GridCell> GridCellListinPriorityQueue = new ArrayList<GridCell>();
        //List<Double> correspondingTimeinQueue = new ArrayList<Double>();
        /**���ȼ����У���������**/

        while (true) {//Dijsktra�㷨˼�룬ÿ������ȫ�֣���ʱ����С�ļ���·�ɱ���֤·�ɱ�����Զ��ʱ����С��·��
            if (iteratorTimes >= size)//|| updateLabel == false)
                break;
            updateLabel = false;

            for (DTNHost c : sourceSet) {
                if (!localHostsList.contains(c)) // limit the search area in the local hosts list
                    continue;
                List<DTNHost> neiList = topologyInfo.get(c);//get neighbor nodes from topology info

                /**�ж��Ƿ��Ѿ�����������Դ���񼯺��е�����**/
                if (searchedSet.contains(c) || neiList == null)
                    continue;

                searchedSet.add(c);
                for (DTNHost eachNeighborNetgrid : neiList) {//startTime.keySet()���������е��ھӽڵ㣬����δ�����ھӽڵ�
                    if (sourceSet.contains(eachNeighborNetgrid))//ȷ������ͷ
                        continue;

                    double time = arrivalTime.get(c) + msg.getSize() / transmitSpeed;
                    /**���·����Ϣ**/
                    List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
                    if (this.routerTable.containsKey(c))
                        path.addAll(this.routerTable.get(c));
                    Tuple<Integer, Boolean> thisHop = new Tuple<Integer, Boolean>(eachNeighborNetgrid.getAddress(), predictLable);
                    
                    path.add(thisHop);//ע��˳��
                    /**���·����Ϣ**/
                    /**ά����С����ʱ��Ķ���**/
                    if (arrivalTime.containsKey(eachNeighborNetgrid)) {
                        /**���������Ƿ�����ͨ���������·��������У����ĸ�ʱ�����**/
                        if (time <= arrivalTime.get(eachNeighborNetgrid)) {
                            if (random.nextBoolean() == true && time - arrivalTime.get(eachNeighborNetgrid) < 0.1) {//���ʱ����ȣ��������ѡ��

                                /**ע�⣬�ڶԶ��н��е�����ʱ�򣬲��ܹ���forѭ������Դ˶��н����޸Ĳ���������ᱨ��**/
                                int index = -1;
                                for (Tuple<DTNHost, Double> t : PriorityQueue) {
                                    if (t.getKey() == eachNeighborNetgrid) {
                                        index = PriorityQueue.indexOf(t);
                                    }
                                }
                                /**ע�⣬�������PriorityQueue���н��е�����ʱ�򣬲��ܹ���forѭ������Դ˶��н����޸Ĳ���������ᱨ��**/
                                if (index > -1) {
                                    PriorityQueue.remove(index);
                                    PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                                    arrivalTime.put(eachNeighborNetgrid, time);
                                    routerTable.put(eachNeighborNetgrid, path);
                                }
                            }
                        }
                        /**���������Ƿ�����ͨ���������·��������У����ĸ�ʱ�����**/
                    } else {
                        PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                        arrivalTime.put(eachNeighborNetgrid, time);
                        routerTable.put(eachNeighborNetgrid, path);
                    }
                    /**�Զ��н�������**/
                    sort(PriorityQueue);
                    updateLabel = true;
                }
            }
            iteratorTimes++;
            for (int i = 0; i < PriorityQueue.size(); i++) {
                if (!sourceSet.contains(PriorityQueue.get(i).getKey())) {
                    sourceSet.add(PriorityQueue.get(i).getKey());//���µ�����������
                    break;
                }
            }
        }
        routerTableUpdateLabel = true;
    }
    /**
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination.
     * It will search the routing path in specific local nodes area.
     * @param msg
     */
    public void optimzedShortestPathSearch(Message msg, List<DTNHost> localHostsList) {
        if (localHostsList.isEmpty())
            return;
        //update the current topology information
        HashMap<DTNHost, List<DTNHost>> topologyInfo = localTopologyGeneration(msg, localHostsList);

        if (routerTableUpdateLabel == true)
            return;
        this.routerTable.clear();
        this.arrivalTime.clear();

        /**ȫ���Ĵ������ʼٶ�Ϊһ����**/
        double transmitSpeed;
        if(msg.getSize() < msgThreshold ){
        	transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        } else{
        	transmitSpeed = this.getHost().getInterface(2).getTransmitSpeed();
        }
//        double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        /**��ʾ·�ɿ�ʼ��ʱ��**/ 

        /**�����·��̽�⵽��һ���ھ����񣬲�����·�ɱ�**/
        List<DTNHost> searchedSet = new ArrayList<DTNHost>();
        List<DTNHost> sourceSet = new ArrayList<DTNHost>();
        sourceSet.add(this.getHost());//��ʼʱֻ��Դ�ڵ���
        searchedSet.add(this.getHost());//��ʼʱֻ��Դ�ڵ�

        for (Connection con : this.getHost().getConnections()) {//�����·��̽�⵽��һ���ھӣ�������·�ɱ�
            if (!localHostsList.contains(con.getOtherNode(this.getHost())))
                continue;
            if (!isRightConnection(msg, con))//�ж��Ƿ�����ȷ����·���䱸��ӿں���Ҫ���м��
            	continue;
            DTNHost neiHost = con.getOtherNode(this.getHost());
            sourceSet.add(neiHost);//��ʼʱֻ�б��ڵ����·�ھ�
//            Double time = getTime() + msg.getSize() / this.getHost().getInterface(1).getTransmitSpeed();
            Double time = getTime() + msg.getSize() / transmitSpeed;
            List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
            Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
            path.add(hop);//ע��˳��
            arrivalTime.put(neiHost, time);
            routerTable.put(neiHost, path);
        }
        /**�����·��̽�⵽��һ���ھ����񣬲�����·�ɱ�**/

        int iteratorTimes = 0;
        int size = localHostsList.size();
        boolean updateLabel = true;
        boolean predictLable = false;

        arrivalTime.put(this.getHost(), SimClock.getTime());//��ʼ������ʱ��

        /**���ȼ����У���������**/
        List<Tuple<DTNHost, Double>> PriorityQueue = new ArrayList<Tuple<DTNHost, Double>>();
        //List<GridCell> GridCellListinPriorityQueue = new ArrayList<GridCell>();
        //List<Double> correspondingTimeinQueue = new ArrayList<Double>();
        /**���ȼ����У���������**/

        while (true) {//Dijsktra�㷨˼�룬ÿ������ȫ�֣���ʱ����С�ļ���·�ɱ���֤·�ɱ�����Զ��ʱ����С��·��
            if (iteratorTimes >= size)//|| updateLabel == false)
                break;
            updateLabel = false;

            for (DTNHost c : sourceSet) {
                if (!localHostsList.contains(c)) // limit the search area in the local hosts list
                    continue;
                List<DTNHost> neiList = topologyInfo.get(c);//get neighbor nodes from topology info

                /**�ж��Ƿ��Ѿ�����������Դ���񼯺��е�����**/
                if (searchedSet.contains(c) || neiList == null)
                    continue;

                searchedSet.add(c);
                for (DTNHost eachNeighborNetgrid : neiList) {//startTime.keySet()���������е��ھӽڵ㣬����δ�����ھӽڵ�
                    if (sourceSet.contains(eachNeighborNetgrid))//ȷ������ͷ
                        continue;

                    double time = arrivalTime.get(c) + msg.getSize() / transmitSpeed;
                    /**���·����Ϣ**/
                    List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
                    if (this.routerTable.containsKey(c))
                        path.addAll(this.routerTable.get(c));
                    Tuple<Integer, Boolean> thisHop = new Tuple<Integer, Boolean>(eachNeighborNetgrid.getAddress(), predictLable);
                    path.add(thisHop);//ע��˳��
                    /**���·����Ϣ**/
                    /**ά����С����ʱ��Ķ���**/
                    if (arrivalTime.containsKey(eachNeighborNetgrid)) {
                        /**���������Ƿ�����ͨ���������·��������У����ĸ�ʱ�����**/
                        if (time <= arrivalTime.get(eachNeighborNetgrid)) {
                            if (random.nextBoolean() == true && time - arrivalTime.get(eachNeighborNetgrid) < 0.1) {//���ʱ����ȣ��������ѡ��

                                /**ע�⣬�ڶԶ��н��е�����ʱ�򣬲��ܹ���forѭ������Դ˶��н����޸Ĳ���������ᱨ��**/
                                int index = -1;
                                for (Tuple<DTNHost, Double> t : PriorityQueue) {
                                    if (t.getKey() == eachNeighborNetgrid) {
                                        index = PriorityQueue.indexOf(t);
                                    }
                                }
                                /**ע�⣬�������PriorityQueue���н��е�����ʱ�򣬲��ܹ���forѭ������Դ˶��н����޸Ĳ���������ᱨ��**/
                                if (index > -1) {
                                    PriorityQueue.remove(index);
                                    PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                                    arrivalTime.put(eachNeighborNetgrid, time);
                                    routerTable.put(eachNeighborNetgrid, path);
                                }
                            }
                        }
                        /**���������Ƿ�����ͨ���������·��������У����ĸ�ʱ�����**/
                    } else {
                        PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                        arrivalTime.put(eachNeighborNetgrid, time);
                        routerTable.put(eachNeighborNetgrid, path);
                    }
                    /**�Զ��н�������**/
                    sort(PriorityQueue);
                    updateLabel = true;
                }
            }
            iteratorTimes++;
            for (int i = 0; i < PriorityQueue.size(); i++) {
                if (!sourceSet.contains(PriorityQueue.get(i).getKey())) {
                    sourceSet.add(PriorityQueue.get(i).getKey());//���µ�����������
                    break;
                }
            }
        }
        routerTableUpdateLabel = true;
    }
    /**
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination.
     * It will search the routing path in specific local nodes area.
     * @param msg
     */
    public void shortestPathSearch(Message msg, DTNHost to, List<DTNHost> localHostsList) {
        if (localHostsList.isEmpty())
            return;
        //update the current topology information
        HashMap<DTNHost, List<DTNHost>> topologyInfo = localTopologyGeneration(msg, localHostsList);

        if (routerTableUpdateLabel == true)
            return;
        this.routerTable.clear();
        this.arrivalTime.clear();
        
        /**ȫ���Ĵ������ʼٶ�Ϊһ����**/
//        double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        double transmitSpeed;
        if(msg.getSize() < msgThreshold ){
        	transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        } else{
        	transmitSpeed = this.getHost().getInterface(2).getTransmitSpeed();
        }
        /**��ʾ·�ɿ�ʼ��ʱ��**/

        /**�����·��̽�⵽��һ���ھ����񣬲�����·�ɱ�**/
        List<DTNHost> searchedSet = new ArrayList<DTNHost>();
        List<DTNHost> sourceSet = new ArrayList<DTNHost>();
        sourceSet.add(this.getHost());//��ʼʱֻ��Դ�ڵ���
        searchedSet.add(this.getHost());//��ʼʱֻ��Դ�ڵ�

        for (Connection con : this.getHost().getConnections()) {//�����·��̽�⵽��һ���ھӣ�������·�ɱ�
            if (!localHostsList.contains(con.getOtherNode(this.getHost())))
                continue;
            if (!isRightConnection(msg, con))//�ж��Ƿ�����ȷ����·���䱸��ӿں���Ҫ���м��
            	continue;
            DTNHost neiHost = con.getOtherNode(this.getHost());
            sourceSet.add(neiHost);//��ʼʱֻ�б��ڵ����·�ھ�
//            Double time = getTime() + msg.getSize() / this.getHost().getInterface(1).getTransmitSpeed();
            Double time = getTime() + msg.getSize() / transmitSpeed;
            
            List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
            Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
            path.add(hop);//ע��˳��
            arrivalTime.put(neiHost, time);
            routerTable.put(neiHost, path);
        }
        /**�����·��̽�⵽��һ���ھ����񣬲�����·�ɱ�**/

        int iteratorTimes = 0;
        int size = localHostsList.size();
        boolean updateLabel = true;
        boolean predictLable = false;

        arrivalTime.put(this.getHost(), SimClock.getTime());//��ʼ������ʱ��

        /**���ȼ����У���������**/
        List<Tuple<DTNHost, Double>> PriorityQueue = new ArrayList<Tuple<DTNHost, Double>>();
        //List<GridCell> GridCellListinPriorityQueue = new ArrayList<GridCell>();
        //List<Double> correspondingTimeinQueue = new ArrayList<Double>();
        /**���ȼ����У���������**/

        while (true) {//Dijsktra�㷨˼�룬ÿ������ȫ�֣���ʱ����С�ļ���·�ɱ���֤·�ɱ�����Զ��ʱ����С��·��
            if (iteratorTimes >= size)//|| updateLabel == false)
                break;
            updateLabel = false;

            for (DTNHost c : sourceSet) {
                if (!localHostsList.contains(c)) // limit the search area in the local hosts list
                    continue;
                List<DTNHost> neiList = topologyInfo.get(c);//get neighbor nodes from topology info

                /**�ж��Ƿ��Ѿ�����������Դ���񼯺��е�����**/
                if (searchedSet.contains(c) || neiList == null)
                    continue;

                searchedSet.add(c);
                for (DTNHost eachNeighborNetgrid : neiList) {//startTime.keySet()���������е��ھӽڵ㣬����δ�����ھӽڵ�
                    if (sourceSet.contains(eachNeighborNetgrid))//ȷ������ͷ
                        continue;
                    
                    double time = arrivalTime.get(c) + msg.getSize() / transmitSpeed;                    
                    /**���·����Ϣ**/
                    List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
                    if (this.routerTable.containsKey(c))
                        path.addAll(this.routerTable.get(c));
                    Tuple<Integer, Boolean> thisHop = new Tuple<Integer, Boolean>(eachNeighborNetgrid.getAddress(), predictLable);
                    path.add(thisHop);//ע��˳��
                    /**���·����Ϣ**/
                    /**ά����С����ʱ��Ķ���**/
                    if (arrivalTime.containsKey(eachNeighborNetgrid)) {
                        /**���������Ƿ�����ͨ���������·��������У����ĸ�ʱ�����**/
                        if (time <= arrivalTime.get(eachNeighborNetgrid)) {
                            if (random.nextBoolean() == true && time - arrivalTime.get(eachNeighborNetgrid) < 0.1) {//���ʱ����ȣ��������ѡ��

                                /**ע�⣬�ڶԶ��н��е�����ʱ�򣬲��ܹ���forѭ������Դ˶��н����޸Ĳ���������ᱨ��**/
                                int index = -1;
                                for (Tuple<DTNHost, Double> t : PriorityQueue) {
                                    if (t.getKey() == eachNeighborNetgrid) {
                                        index = PriorityQueue.indexOf(t);
                                    }
                                }
                                /**ע�⣬�������PriorityQueue���н��е�����ʱ�򣬲��ܹ���forѭ������Դ˶��н����޸Ĳ���������ᱨ��**/
                                if (index > -1) {
                                    PriorityQueue.remove(index);
                                    PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                                    arrivalTime.put(eachNeighborNetgrid, time);
                                    routerTable.put(eachNeighborNetgrid, path);
                                }
                            }
                        }
                        /**���������Ƿ�����ͨ���������·��������У����ĸ�ʱ�����**/
                    } else {
                        PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                        arrivalTime.put(eachNeighborNetgrid, time);
                        routerTable.put(eachNeighborNetgrid, path);
                    }
                    /**�Զ��н�������**/
                    sort(PriorityQueue);
                    updateLabel = true;
                }
            }
            iteratorTimes++;
            for (int i = 0; i < PriorityQueue.size(); i++) {
                if (!sourceSet.contains(PriorityQueue.get(i).getKey())) {
                    sourceSet.add(PriorityQueue.get(i).getKey());//���µ�����������
                    break;
                }
            }
        }
        routerTableUpdateLabel = true;
    }
    /**
     * ��ȡÿ�����������Ĺ��ƽ����
     * @param host
     */
    public int getLEOOrbitPlane(DTNHost host){
        return host.getAddress()/LEO_NROF_S_EACHPLANE + 1;
    }
    /**
     * judge the shortest direction to forward message in the same orbit plane
     * @param to
     */
    public DTNHost chooseOneNeighborHostToSendInSameLEOPlane(DTNHost src, DTNHost to){
    	DynamicMultiLayerSatelliteRouter srcRouter = (DynamicMultiLayerSatelliteRouter)src.getRouter();
    	
    	LEOclusterInfo LEOci = srcRouter.getSatelliteLinkInfo().getLEOci();
    	
        if (LEOci.getNeighborHostsInSamePlane().size() != 2)
            throw new SimError("LEOci.getNeighborHostsInSamePlane() error!");
        DTNHost a = LEOci.getNeighborHostsInSamePlane().get(0);
        DTNHost b = LEOci.getNeighborHostsInSamePlane().get(1);
        DTNHost nextHop = null;
        if (abs(to.getAddress() - a.getAddress()) > abs(to.getAddress() - b.getAddress()))
            nextHop = b;
        else
            nextHop = a;
        return nextHop;
    }
    /**
     * find the path from this node to another LEO node in the same plane
     * @param to
     */
    public List<Tuple<Integer, Boolean>> findPathInSameLEOPlane(DTNHost srcLEO, DTNHost to){
        List<Tuple<Integer, Boolean>> path =
                new ArrayList<Tuple<Integer, Boolean>>();//��¼����·��
        
        DynamicMultiLayerSatelliteRouter srcRouter = (DynamicMultiLayerSatelliteRouter)srcLEO.getRouter();
        
        //���·��λ���ˣ���ֱ���ҵ�����·����Ȼ��д��
        DTNHost nextHop = srcLEO;
    	for (int i = 0; i < srcRouter.getSatelliteLinkInfo().
    			getLEOci().getAllHostsInSamePlane().size() ; i++){//��ֹ�޷������Ĵ���
    		nextHop = chooseOneNeighborHostToSendInSameLEOPlane(nextHop, to);
    		path.add(new Tuple<Integer, Boolean>(nextHop.getAddress(), false));
    		//�Ѿ�������Ŀ�Ľڵ㣬��·����������
    		if (nextHop.getAddress() == to.getAddress()){
    			srcRouter.routerTable.put(to, path); 
    			break;
    		}
    	}
//    	System.out.println(this.getHost()+"  ͬһ��ƽ���ڵ�·���� "+path);
        return path;
    }
 
    /**
     * LEO��Ϣ�����ھӹ��ƽ��
     * @param to
     */
    public boolean msgFromLEOForwardToNeighborPlane(Message msg, DTNHost to){
    	
    	int destinationSerialNumberOfPlane = to.getAddress()/LEO_NROF_S_EACHPLANE + 1;
//    	System.out.println("forward to neighbor plane   "+destinationSerialNumberOfPlane);
    	List<DTNHost> allCommunicationNodes = new ArrayList<DTNHost>();
    	//�ҳ�����Ŀ�Ľڵ���ƽ���ϵĿ���֧�ֿ�ƽ��ͨ�ŵ�����
    	for (DTNHost h : this.CommunicationNodesList.keySet()){//�����CommunicationNodesList���¼�Ĺ��ƽ�����Ǵ�0��ʼ��
    		if (this.CommunicationNodesList.get(h) + 1 == destinationSerialNumberOfPlane)
    			allCommunicationNodes.add(h);
    	}
//    	System.out.println("all communication nodes: "+allCommunicationNodes);
//    	System.out.println("���ھӹ��! �ھӹ���Ͽ�ͨ�Žڵ㣺 "+allCommunicationNodes+" connections: "+this.getConnections());
    	for (DTNHost h : allCommunicationNodes){
    		Connection con = this.findConnection(h.getAddress(), msg);
    		if (con != null){
                List<Tuple<Integer, Boolean>> path =
                        new ArrayList<Tuple<Integer, Boolean>>();
                path.add(new Tuple<Integer, Boolean>(h.getAddress(), false));
                routerTable.put(to, path);
//                System.out.println(this.getHost()+"  ͬһ��ƽ���ڵ�·���� "+path);
                return true;
    		}
    	}   
    	//û����ͨ����������
    	return false;
    }
    /**
     * find the nearest communication LEO nodes in the same orbit plane
     * @param LEO
     * @return
     */
    public DTNHost findNearestCommunicationLEONodes(DTNHost LEO){
    	if (LEO.getRouter().CommunicationSatellitesLabel)
    		return LEO;
    	int min = Integer.MAX_VALUE;
    	DTNHost minHost = null;
    	//ȡ�������ƽ�������е�ͨ�Žڵ�
    	for (DTNHost cLEO : ((SatelliteMovement)LEO.getMovementModel()).
    			getSatelliteLinkInfo().getLEOci().getAllCommunicationNodes()){
    		int distance = Math.abs(cLEO.getAddress() - LEO.getAddress());
    		if (distance < min){
    			min = distance;
    			minHost = cLEO;
    		}
    		else{
    			if (distance == min && random.nextBoolean()){
	    			min = distance;
	    			minHost = cLEO;
    			}
    		}
    	}
    	return minHost;
    }
    /**
     * ִ�д�LEO��Ϣ����MEOת��
     * @param to
     */
    public void msgFromCommunicationLEOForwardedByMEO(Message msg, DTNHost to){
    	LEOclusterInfo LEOci = this.getSatelliteLinkInfo().getLEOci();
    	
    	if (this.getHost().getRouter().CommunicationSatellitesLabel &&
    			LEOci.updateManageHosts(msg).isEmpty()){
//            System.out.println(this.getHost()+" ͨ�Žڵ�LEO ������û��MEO���ӣ�  "+msg);
    		return;
    	}
    	   	
    	if (((SatelliteMovement)to.getMovementModel()).getSatelliteLinkInfo().getLEOci() == null){
//    		System.out.println(msg+" not initiliation LEOci! "+to);
    		throw new SimError("not initiliation LEOci!");
    		//return;
    	}
    	
    	/**�������·�������㷨�ı�����������·��**/
        //��ȡLEOͨ��MEO���絽��Ŀ��LEO������
        //��������·���㷨���������ⳡ������Ҫָ������Դ�ڵ㣬��������������
    	
        //shortestPathSearch(msg, this.getHost(), getLEOtoLEOThroughMEOTopology(msg, this.getHost(), to));
        
    	//�ҵ�����MEO�ڵ�
        List<DTNHost> hostsList = findMEOHosts();
        DTNHost nearestCLEOtoDestination = findNearestCommunicationLEONodes(to);
        hostsList.add(nearestCLEOtoDestination);
        hostsList.add(this.getHost());
        /*һ���Ǵ�ͨ�Žڵ㷢�͵�MEO����ת��*/
        shortestPathSearch(msg, nearestCLEOtoDestination, hostsList);//�����õ�ȥ����Ŀ�Ľڵ������ͨ�Žڵ��·��
    	/**�������·�������㷨�ı�����������·��**/
        
        List<Tuple<Integer, Boolean>> lastPath = findPathInSameLEOPlane(nearestCLEOtoDestination, to);
    	
    	if (to.getRouter().CommunicationSatellitesLabel == false 
    			&& this.routerTable.containsKey(nearestCLEOtoDestination)){
//    		System.out.println(msg+ " ������ͨ��MEOת�������·���� to" + to);
    		List<Tuple<Integer, Boolean>> path = this.routerTable.get(nearestCLEOtoDestination);
    		path.addAll(lastPath);
    		this.routerTable.put(to, path);//���ȥĿ�Ľڵ��·��
    		return;
    	}   	    	
    }
    
    /**
     * �ȵõ���ʼLEO�����ͨ��LEO��·�����ٵõ�ͨ��LEO��MEO�Լ�MEO������ˣ����õ�Ŀ��LEO�������ͨ��LEO�����ˣ������һ��
     * ÿһ��MEO�ڵ���ھӷ���4���ڵ㣬ͬһ����ڵ����������ڵ㣬�ھӹ������������ڵ㣬�Ӷ���������������
     * @param startMEO ��ʼ��
     * @param endMEO   Ŀ�Ľڵ�
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getLEOtoLEOThroughMEOTopology(Message msg, DTNHost startLEO, DTNHost endLEO){
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	
//    	//����Ƕ�̬�ִ�·�ɻ�ִ�и��²���������Ǿ�̬�ִ�·����ֱ�ӷ������ȹ涨��MEO����ڵ�
//    	List<DTNHost> manageHosts = ((SatelliteMovement)endLEO.getMovementModel()).
//    			getSatelliteLinkInfo().getLEOci().updateManageHosts(msg);
//    	if (manageHosts.isEmpty())
//    		return topologyInfo;//����Ϊ��
    	   	
    	topologyInfo = getMEOtoLEOTopology(msg, endLEO);
    	/**�ҵ�startLEO�����ͨ�Žڵ㣬������MEO����ͨ��,�������Ӧ������**/ 	
    	topologyInfo.putAll(getLEOtoNearestCommunicationLEOTopology(startLEO));
    	
//    	for (DTNHost MEO : manageHosts){
//    		List<DTNHost> list = topologyInfo.get(MEO);  	
//            if (list == null) {
//            	list = new ArrayList<DTNHost>();
//                list.add(startLEO);
//            } else {
//            	list.add(startLEO);
//            }
//    	}
    	return topologyInfo;
    }   
    /**
     * ÿһ��GEO��MEO�ڵ���ھӷ���4���ڵ㣬ͬһ����ڵ����������ڵ㣬�ھӹ������������ڵ㣬
     * �Ӷ���������������
     * @param startMEO ��ʼ��
     * @param endMEO   Ŀ�Ľڵ�
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getGEOtoGEOTopology(DTNHost endGEO){
    	GEOclusterInfo GEOci = ((SatelliteMovement)endGEO.getMovementModel()).getSatelliteLinkInfo().getGEOci();   	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	
    	for (DTNHost GEO : GEOci.getGEOList()){
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//ͬһ����ڵ����������ڵ�
    		neighborNodes.addAll(GEOci.getAllowConnectGEOHostsInSamePlane());
    		//�ھӹ������������ڵ�
    		neighborNodes.addAll(GEOci.updateAllowConnectGEOHostsInNeighborPlane());  
    		topologyInfo.put(GEO, neighborNodes);
    	}

    	return topologyInfo;
    }
    /**
     * ÿһ��GEO��MEO�ڵ���ھӷ���4���ڵ㣬ͬһ����ڵ����������ڵ㣬�ھӹ������������ڵ㣬
     * �Ӷ���������������
     * @param startMEO ��ʼ��
     * @param endMEO   Ŀ�Ľڵ�
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getMEOtoGEOTopology(DTNHost sMEO, DTNHost endGEO){
    	GEOclusterInfo endGEOci = ((SatelliteMovement)endGEO.getMovementModel()).getSatelliteLinkInfo().getGEOci();
    	MEOclusterInfo sMEOci = ((SatelliteMovement)sMEO.getMovementModel()).getSatelliteLinkInfo().getMEOci();
    	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	//GEO��Ľڵ�����
    	for (DTNHost GEO : endGEOci.getGEOList()){
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//ͬһ����ڵ����������ڵ�
    		neighborNodes.addAll(endGEOci.getAllowConnectGEOHostsInSamePlane());
    		//�ھӹ������������ڵ�
    		neighborNodes.addAll(endGEOci.updateAllowConnectGEOHostsInNeighborPlane());  
    		topologyInfo.put(GEO, neighborNodes);
    	}
    	//MEO��Ľڵ�����
    	for (DTNHost MEO : sMEOci.getMEOList()){
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//ͬһ����ڵ����������ڵ�
    		neighborNodes.addAll(sMEOci.getAllowConnectMEOHostsInSamePlane());
    		//�ھӹ������������ڵ�
    		neighborNodes.addAll(sMEOci.updateAllowConnectMEOHostsInNeighborPlane());  
    		topologyInfo.put(MEO, neighborNodes);
    	}
    	//���������GEO��Ŀ��MEO����·
    	for (DTNHost MEO : sMEOci.getMEOList()){
    		List<DTNHost> list = topologyInfo.get(MEO);  
        	MEOclusterInfo MI = ((SatelliteMovement)MEO.
        			getMovementModel()).getSatelliteLinkInfo().getMEOci();
            if (list == null) {
            	list = new ArrayList<DTNHost>();
                list.addAll(MI.getConnectedGEOHosts());
            } else {
            	list.addAll(MI.getConnectedGEOHosts());
            }
    	}
    	return topologyInfo;
    }
    /**
     * ÿһ��GEO��MEO�ڵ���ھӷ���4���ڵ㣬ͬһ����ڵ����������ڵ㣬�ھӹ������������ڵ㣬
     * �Ӷ���������������
     * @param startMEO ��ʼ��
     * @param endMEO   Ŀ�Ľڵ�
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getGEOtoMEOTopology(DTNHost sGEO, DTNHost endMEO){
    	GEOclusterInfo GEOci = ((SatelliteMovement)sGEO.getMovementModel()).getSatelliteLinkInfo().getGEOci();
    	MEOclusterInfo MEOci = ((SatelliteMovement)endMEO.getMovementModel()).getSatelliteLinkInfo().getMEOci();
    	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	//GEO��Ľڵ�����
    	for (DTNHost GEO : GEOci.getGEOList()){
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//ͬһ����ڵ����������ڵ�
    		neighborNodes.addAll(GEOci.getAllowConnectGEOHostsInSamePlane());
    		//�ھӹ������������ڵ�
    		neighborNodes.addAll(GEOci.updateAllowConnectGEOHostsInNeighborPlane());  
    		topologyInfo.put(GEO, neighborNodes);
    	}
    	//MEO��Ľڵ�����
    	for (DTNHost MEO : MEOci.getMEOList()){
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//ͬһ����ڵ����������ڵ�
    		neighborNodes.addAll(MEOci.getAllowConnectMEOHostsInSamePlane());
    		//�ھӹ������������ڵ�
    		neighborNodes.addAll(MEOci.updateAllowConnectMEOHostsInNeighborPlane());  
    		topologyInfo.put(MEO, neighborNodes);
    	}
    	//���������GEO��Ŀ��MEO����·
    	for (DTNHost GEO : GEOci.getGEOList()){
    		List<DTNHost> list = topologyInfo.get(GEO);  
        	GEOclusterInfo GI = ((SatelliteMovement)GEO.
        			getMovementModel()).getSatelliteLinkInfo().getGEOci();
            if (list == null) {
            	list = new ArrayList<DTNHost>();
                list.addAll(GI.getConnectedMEOHosts());
            } else {
            	list.addAll(GI.getConnectedMEOHosts());
            }
    	}
    	return topologyInfo;
    }
    /**
     * ÿһ��GEO/MEO�ڵ���ھӷ���4���ڵ㣬ͬһ����ڵ����������ڵ㣬�ھӹ������������ڵ㣬
     * �Ӷ���������������
     * @param startMEO ��ʼMEO��
     * @param endMEO   Ŀ��LEO�ڵ�
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getGEOtoLEOTopology(Message msg, DTNHost sGEO, DTNHost endLEO){
    	DTNHost nearestCLEO = this.findNearestCommunicationLEONodes(endLEO);
    	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	LEOclusterInfo nearestCLEOci = ((SatelliteMovement)nearestCLEO.getMovementModel()).getSatelliteLinkInfo().getLEOci();
    	GEOclusterInfo sGEOci = ((SatelliteMovement)sGEO.getMovementModel()).getSatelliteLinkInfo().getGEOci();

    	topologyInfo = getMEOtoMEOTopology(this.findMEOHosts());//�������MEO���������·
    	//��������ӱ�GEO������������MEO����·
    	for (DTNHost MEO : sGEOci.updateGEOClusterMember()){
    		List<DTNHost> list = topologyInfo.get(MEO);  	
            if (list == null) {
            	list = new ArrayList<DTNHost>();
                list.add(sGEO);
            } else {
            	list.add(sGEO);
            }
    	}
    	
    	topologyInfo.putAll(getGEOtoGEOTopology(sGEO));//���GEO�������
    	
    	//���������MEO��Ŀ��LEO����·
    	for (DTNHost MEO : nearestCLEOci.updateManageHosts(msg)){
    		List<DTNHost> list = topologyInfo.get(MEO);  	
            if (list == null) {
            	list = new ArrayList<DTNHost>();
                list.add(endLEO);
            } else {
            	list.add(endLEO);
            }
    	}	
    	//���LEO��MEO����·
    	topologyInfo.put(nearestCLEO, nearestCLEOci.updateManageHosts(msg));
    	return topologyInfo;
    }
    /**
     * ÿһ��MEO�ڵ���ھӷ���4���ڵ㣬ͬһ����ڵ����������ڵ㣬�ھӹ������������ڵ㣬
     * �Ӷ���������������
     * @param startMEO ��ʼ��
     * @param endMEO   Ŀ�Ľڵ�
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getMEOtoMEOTopology(List<DTNHost> MEOHosts){    	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	for (DTNHost MEO : MEOHosts){
    		MEOclusterInfo MEOci = ((SatelliteMovement)MEO.getMovementModel()).getSatelliteLinkInfo().getMEOci();
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//ͬһ����ڵ����������ڵ�
    		neighborNodes.addAll(MEOci.getAllowConnectMEOHostsInSamePlane());
    		//�ھӹ������������ڵ�
    		neighborNodes.addAll(MEOci.updateAllowConnectMEOHostsInNeighborPlane());  
    		topologyInfo.put(MEO, neighborNodes);
    	}
    	return topologyInfo;
    }
    /**
     * ÿһ��MEO�ڵ���ھӷ���4���ڵ㣬ͬһ����ڵ����������ڵ㣬�ھӹ������������ڵ㣬
     * �Ӷ���������������
     * @param startMEO ��ʼ��
     * @param endMEO   Ŀ�Ľڵ�
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getMEOtoMEOTopology(){
    	if (MEO_TOTAL_SATELLITES <= 0)
    		return null;
    	//���ҵ�һ��MEO�ڵ�
    	DTNHost sMEO = null;
    	for (DTNHost h : this.getHosts()){
    		if (h.getSatelliteType().contains("MEO"))
    			sMEO = h;
    	}
    	MEOclusterInfo sMEOci = ((SatelliteMovement)sMEO.getMovementModel()).getSatelliteLinkInfo().getMEOci();
    	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	//��ÿһ��MEO�ڵ㣬�����������·���Ӷ�����MEO�������
    	for (DTNHost MEO : sMEOci.getMEOList()){
    		MEOclusterInfo MEOci = ((SatelliteMovement)MEO.getMovementModel()).getSatelliteLinkInfo().getMEOci();
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//ͬһ����ڵ����������ڵ�
    		neighborNodes.addAll(MEOci.getAllowConnectMEOHostsInSamePlane());
    		//�ھӹ������������ڵ�
    		neighborNodes.addAll(MEOci.updateAllowConnectMEOHostsInNeighborPlane());  
    		topologyInfo.put(MEO, neighborNodes);
    	}
    	return topologyInfo;
    }
    /**
     * ��ȡ��LEO�ڵ㵽�����ͨ��LEO�ڵ������
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getLEOtoNearestCommunicationLEOTopology(DTNHost src){
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	
    	DTNHost startCommunicationLEO = findNearestCommunicationLEONodes(src);  
    	//����Լ�����ͨ�Žڵ㣬����ӵ�ͨ�Žڵ��·��
    	if (!(startCommunicationLEO.getAddress() == src.getAddress())){
    		List<Tuple<Integer, Boolean>> pathTocLEO = 
    				findPathInSameLEOPlane(src, startCommunicationLEO);//�ҵ�ǰ��ͬһƽ����ͨ�Žڵ��·��
        	
        	int size = pathTocLEO.size();
        	DTNHost previousHop = src;
        	//ǰ����������е���·
        	for (int index = 0; index < size; index++){       		
        		Tuple<Integer, Boolean> t = pathTocLEO.get(index);
        		List<DTNHost> links = new ArrayList<DTNHost>();
        		DTNHost thisHop = findHostByAddress(t.getKey());
        		links.add(thisHop);//���һ������·
        		if (!(index + 1 >= size))
        			links.add(previousHop);//������м�ڵ㣬��Ҫ���˫��������·
        		topologyInfo.put(previousHop, links);
        		previousHop = thisHop;
        	}
    	}
    	return topologyInfo;
    }
    /**
     * ÿһ��MEO�ڵ���ھӷ���4���ڵ㣬ͬһ����ڵ����������ڵ㣬�ھӹ������������ڵ㣬
     * �Ӷ���������������
     * @param startMEO ��ʼMEO��
     * @param endMEO   Ŀ��LEO�ڵ�
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getMEOtoLEOTopology(Message msg, DTNHost endLEO){
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	LEOclusterInfo endLEOci = ((SatelliteMovement)endLEO.getMovementModel()).getSatelliteLinkInfo().getLEOci();
    	
    	topologyInfo = getMEOtoMEOTopology();
    
    	//���ǵ�MEO�ڵ�ֻ����ͨ��LEO�ڵ�֮�����ͨ��
    	topologyInfo.putAll(getLEOtoNearestCommunicationLEOTopology(endLEO));//�ҵ�Ŀ��LEO�������ͨ��LEO�ڵ�֮������ˣ����������
    	
//    	//���������MEO��Ŀ��LEO����·
//    	for (DTNHost MEO : endLEOci.updateManageHosts(msg)){
//    		List<DTNHost> list = topologyInfo.get(MEO);  	
//            if (list == null) {
//            	list = new ArrayList<DTNHost>();
//                list.add(endLEO);
//            } else {
//            	list.add(endLEO);
//            }
//    	}	
    	return topologyInfo;
    }
    /**
     * ÿһ��MEO�ڵ���ھӷ���4���ڵ㣬ͬһ����ڵ����������ڵ㣬�ھӹ������������ڵ㣬
     * �Ӷ���������������
     * @param startMEO ��ʼMEO��
     * @param endMEO   Ŀ��LEO�ڵ�
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getMEOtoCommunicationLEOTopology(Message msg, DTNHost endLEO){
    	if (!endLEO.getRouter().CommunicationSatellitesLabel)
    		throw new SimError(" not a communication LEO! ");
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	
    	topologyInfo = getMEOtoMEOTopology();
    
    	//��ȡĿ��ͨ��LEO�ڵ�Ĺ���MEO�ڵ��б���̬���߾�̬
		List<DTNHost> manageMEO = ((SatelliteMovement) endLEO
				.getMovementModel()).getSatelliteLinkInfo().getLEOci()
				.updateManageHosts(msg);
    	//���MEO��Ŀ��ͨ��LEO�ڵ����·
    	topologyInfo.put(endLEO , manageMEO);
    	for (DTNHost MEO : manageMEO){
    		topologyInfo.get(MEO).add(endLEO);
    	}
    	return topologyInfo;
    }
    /**
     * Bubble sort algorithm
     *
     * @param distanceList
     * @return
     */
    public List<Tuple<DTNHost, Double>> sort(List<Tuple<DTNHost, Double>> distanceList) {
        for (int j = 0; j < distanceList.size(); j++) {
            for (int i = 0; i < distanceList.size() - j - 1; i++) {
                if (distanceList.get(i).getValue() > distanceList.get(i + 1).getValue()) {//��С���󣬴��ֵ���ڶ����Ҳ�
                    Tuple<DTNHost, Double> var1 = distanceList.get(i);
                    Tuple<DTNHost, Double> var2 = distanceList.get(i + 1);
                    distanceList.remove(i);
                    distanceList.remove(i);//ע�⣬һ��ִ��remove֮������List�Ĵ�С�ͱ��ˣ�����ԭ��i+1��λ�����ڱ����i
                    //ע��˳��
                    distanceList.add(i, var2);
                    distanceList.add(i + 1, var1);
                }
            }
        }     
        return distanceList;
    }

    /**
     * Find the DTNHost according to its address
     * @param path
     * @return
     */
    public List<DTNHost> getHostListFromPath(List<Integer> path) {
        List<DTNHost> hostsOfPath = new ArrayList<DTNHost>();
        for (int i = 0; i < path.size(); i++) {
            hostsOfPath.add(this.getHostFromAddress(path.get(i)));//���ݽڵ��ַ�ҵ�DTNHost
        }
        return hostsOfPath;
    }

    /**
     * Find the DTNHost according to its address
     *
     * @param address
     * @return
     */
    public DTNHost getHostFromAddress(int address) {
        for (DTNHost host : getHosts()) {
            if (host.getAddress() == address)
                return host;
        }
        return null;
    }

    /**
     * Calculate the distance between two nodes.
     *
     * @param a
     * @param b
     * @return
     */
    public double getDistance(DTNHost a, DTNHost b) {
        double ax = a.getLocation().getX();
        double ay = a.getLocation().getY();
        double az = a.getLocation().getZ();
        double bx = a.getLocation().getX();
        double by = a.getLocation().getY();
        double bz = a.getLocation().getZ();

        double distance = (ax - bx) * (ax - bx) + (ay - by) * (ay - by) + (az - bz) * (az - bz);
        distance = Math.sqrt(distance);

        return distance;
    }

    /**
     * Find the specific connection according to neighbor node's address
     *
     * @param address
     * @return
     */
    public Connection findConnection(int address, Message msg) {
    	String connectionType = "";
    	if (msg.getSize() > msgThreshold)
    		connectionType = "LaserLink";
    	else
    		connectionType = "RadioLink";
    	
        List<Connection> connections = this.getHost().getConnections();
        
        for (Connection c : connections) {
            if (c.getOtherNode(this.getHost()).getAddress() == address 
            		&& isRightConnection(msg, c)) {    
                return c;
            }
        }
        return null;
    }

    /**
     * Try to send the message through a specific connection
     *
     * @param t
     * @return
     */
    public Message tryMessageToConnection(Tuple<Message, Connection> t) {
        if (t == null)
            throw new SimError("No such tuple: " +
                    " at " + this);
        Message m = t.getKey();
        Connection con = t.getValue();

        int retVal = startTransfer(m, con);
        if (retVal == RCV_OK) {  //accepted a message, don't try others
            return m;
        } else if (retVal > 0) { //ϵͳ���壬ֻ��TRY_LATER_BUSY����0����Ϊ1
            return null;          // should try later -> don't bother trying others
        }
        return null;
    }

    /**
     * Judge the next hop is busy or not.
     *
     * @param t
     * @return
     */
    public boolean nextHopIsBusyOrNot(Tuple<Message, Connection> t) {

        Connection con = t.getValue();
        if (con == null)
            return false;
        /**���������·��������������һ������·�Ѿ���ռ�ã�����Ҫ�ȴ�**/
        if (con.isTransferring() || ((OptimizedClusteringRouter)
                con.getOtherNode(this.getHost()).getRouter()).isTransferring()) {
            return true;//˵��Ŀ�Ľڵ���æ
        }
        return false;
        /**���ڼ�����е���·ռ������������ڵ��Ƿ��ڶ��ⷢ�͵��������update�������Ѿ������ˣ��ڴ������ظ����**/
    }





//    /**
//     * ����д������֤�ڴ������֮��Դ�ڵ����Ϣ��messages������ɾ��
//     */
//    @Override
//    protected void transferDone(Connection con) {
//        String msgId = con.getMessage().getId();
//        removeFromMessages(msgId);
//    }

    /**
     * get all satellite nodes info in the movement model
     *
     * @return all satellite nodes in the network
     */
    public List<DTNHost> getHosts() {
        return new ArrayList<DTNHost>(((SatelliteMovement) this.getHost().getMovementModel()).getHosts());
    }

    /**
     * get satellite movement model
     * @return
     */
    public MovementModel getMovementModel(){
        return this.getHost().getMovementModel();
    }
    /**
     * @return satellite type in multi-layer satellite networks: LEO, MEO or GEO
     */
    public String getSatelliteType(){
        return this.getHost().getSatelliteType();
    }
    /**
     * @return SatelliteInterLinkInfo for getting cluster information
     */
    public SatelliteInterLinkInfo getSatelliteLinkInfo(){
    	return ((SatelliteMovement)this.getHost().getMovementModel()).getSatelliteLinkInfo();
    }
    
    /**
     * 
     * @return all MEO nodes
     */
    public List<DTNHost> getMEO_ClusterList(){
    	if (this.getSatelliteLinkInfo().getMEOci() != null)
    		return this.getSatelliteLinkInfo().getMEOci().getClusterList();
    	return null;
    }
    /**
     * find all MEO hosts
     * @return
     */
    public List<DTNHost> findMEOHosts(){
    	List<DTNHost> MEOHosts = new ArrayList<DTNHost>();
    	for (DTNHost h : getHosts()){
    		if (h.getSatelliteType().contains("MEO"))
    			MEOHosts.add(h);
    	}
    	return MEOHosts;
    }
    /**
     * find all GEO hosts
     * @return
     */
    public List<DTNHost> findGEOHosts(){
    	List<DTNHost> GEOHosts = new ArrayList<DTNHost>();
    	for (DTNHost h : getHosts()){
    		if (h.getSatelliteType().contains("GEO"))
    			GEOHosts.add(h);
    	}
    	return GEOHosts;
    }
    /**
     * if the connection type is the matched with this type of message
     * @param msg
     * @param con
     * @return
     */
    public boolean isRightConnection(Message msg, Connection con){
    	if (msg.getSize() > msgThreshold && con.getLinkType().contains(LASER_LINK))
    		return true;
    	if (msg.getSize() <= msgThreshold && con.getLinkType().contains(RADIO_LINK))
    		return true;
    	
    	return false;
    }
}
