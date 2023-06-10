/*
 * Copyright 2016 University of Science and Technology of China , Infonet Lab
 * Written by LiJian.
 */
package routing;

import java.awt.*;
import java.awt.image.DataBufferDouble;
import java.util.*;
import java.util.List;


import com.sun.xml.internal.ws.util.xml.CDATA;
import core.*;
import input.ExternalEvent;
import input.ExternalPathMovementReader;
import input.MessageEventGenerator;
import jdk.internal.org.objectweb.asm.tree.InnerClassNode;
import movement.SatelliteMovement;
import org.omg.CORBA.INTF_REPOS;
import org.omg.CORBA.PUBLIC_MEMBER;
import util.Tuple;
import static core.SimClock.getTime;
import core.World;
import report.*;
import javax.management.relation.RelationNotFoundException;
import core.DTNHost;

import java.util.Map.Entry;



public class ShortestPathFirstRouter extends ActiveRouter {
    /**
     * The TTL of confirm message -setting id ({@value})
     */
    public static final String HELLOTTL_S = "HelloTTL";
    /**
     * The size of confirm message -setting id ({@value})
     */
    public static final String HELLOMESSAGESIZE_S = "HelloMessageSize";
    /**
     * label indicates that the message can wait for next hop coming or not -setting id ({@value})
     */
    public static final String MSG_WAITLABEL = "waitLabel";
    /**
     * label indicates that routing path can contain in the message or not -setting id ({@value})
     */
    public static final String MSG_PATHLABEL = "msgPathLabel";
    /**
     * router path -setting id ({@value})
     */
    public static final String MSG_ROUTERPATH = "routerPath";
    /**
     * Group name in the group -setting id ({@value})
     */
    public static final String GROUPNAME_S = "Group";
    /**
     * interface name in the group -setting id ({@value})
     */
    public static final String INTERFACENAME_S = "Interface";
    /**
     * transmit range -setting id ({@value})
     */
    public static final String TRANSMIT_RANGE_S = "transmitRange";

    /**
     * light speed，approximate 3*10^8m/s
     */
    private static final double LIGHTSPEED = 299792458;
    /**
     * the interval of each hello process -setting id ({@value}
     */
    private double helloInterval;
    /**
     * indicates the TTL of hello message -setting id ({@value}
     */
    private static int helloTtl;
    /**
     * indicates the number of hello message -setting id ({@value}
     */
    private static int helloMessageNum;
    /**
     * hello message size -setting id ({@value}
     */
    private static int helloMessageSize;

    /**
     * store the latest hello check time
     */
    private double lastHelloCheckTime;
    /**
     * indicate the transmission radius of each satellite
     */
    private static double transmitRange;
    /**
     * label indicates that routing path can contain in the message or not
     */
    private static boolean msgPathLabel;
    /**
     * label indicates that the static routing parameters are set or not
     */
    private static boolean initLabel = false;
    /**
     * label indicates that routing algorithm has been executed or not at this time
     */
    private boolean routerTableUpdateLabel;
    /**
     * maintain the earliest arrival time to other nodes
     */
    //private HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
    /**
     * the router table comes from routing algorithm
     */
    //private HashMap<DTNHost, List<Tuple<Integer, Boolean>>> routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();
    /**
     * to make the random choice
     */
    private Random random;

    private List<Message> helloMessages;

    public ShortestPathFirstRouter(Settings s) {
        super(s);
    }

    protected ShortestPathFirstRouter(ShortestPathFirstRouter r) {
        super(r);
    }

    @Override
    public MessageRouter replicate() {
        return new ShortestPathFirstRouter(this);
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
        super.init(host, mListeners);
        for(int i=0;i<feedbackinfolength;i++){
            feedbackinfo[i]=1.0;
        }

        Settings setting = new Settings();
        setting.setNameSpace("Events1");
        this.dataMessageSize = setting.getInt("dataMessageSize");
        setting.setNameSpace("userSetting");
        this.collectpoplistinterval=setting.getInt("collectpoplistinterval");
        this.BufferSize=setting.getInt("BufferSize");
        this.cacheenablednodemin=setting.getInt("cacheenablednodemin");
        this.cacheenablednodemax=setting.getInt("cacheenablednodemax");
        this.isprediction=setting.getInt("prediction");

        if(this.iscachenablednode()){
            SetBuffercontent();
            creatpopularityinfolist(buffercontent);
        }




        if (!initLabel) {
            random = new Random();
            Settings settings = new Settings(INTERFACENAME_S);
            transmitRange = settings.getInt(TRANSMIT_RANGE_S);
            settings.setNameSpace(GROUPNAME_S);
            msgPathLabel = settings.getBoolean(MSG_PATHLABEL);
            initLabel = true;
            helloInterval = settings.getDouble("HelloInterval");
            helloTtl = settings.getInt(HELLOTTL_S);
            helloMessageSize = settings.getInt(HELLOMESSAGESIZE_S);

        }
    }

    /**
     * 在Networkinterface类中执行链路中断函数disconnect()后，对应节点的router调用此函数
     */
    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);
//		System.out.println("message: "+con);
//		if (!con.isUp()){
//			if(con.isTransferring()){
//				if (con.getOtherNode(this.getHost()).getRouter().isIncomingMessage(con.getMessage().getId()))
//					con.getOtherNode(this.getHost()).getRouter().removeFromIncomingBuffer(con.getMessage().getId(), this.getHost());
//				super.addToMessages(con.getMessage(), false);//对于因为链路中断而丢失的消息，重新放回发送方的队列中，并且删除对方节点的incoming信息
//				System.out.println("message: "+con.getMessage());
//			}
//		}
    }

    @Override
    public void update() {
        super.update();
//        if(SimClock.getTime()>2000){
//            System.out.println("ddd");
//        }


        List<Connection> connections = this.getConnections();

        if (isTransferring()) {    // judge the link is occupied or not
            return;                // can't start a new transfer
        }
        if (connections.size() > 0) {//有邻居时需要进行hello包发送协议
            //helloProtocol();//执行hello包的维护工作
        }
        if (!canStartTransfer())
            return;

        /**Set router update label to make sure that routing algorithm only execute once at a time**/
        routerTableUpdateLabel = false;

        /** sort the messages to transmit */
        List<Message> messageList = this.CollectionToList(this.getMessageCollection());
        List<Message> messages = sortByQueueMode(messageList);

        for (Message msg : messages) {
            if (findPathToSend(msg) == true) {
                return;
            }
        }


    }

    /**
     * transform the message Collection to List
     *
     * @param messages
     * @return
     */

    public List<Message> CollectionToList(Collection<Message> messages) {
        List<Message> forMsg = new ArrayList<Message>();
        for (Message msg : messages) {    //尝试发送队列里的消息
            forMsg.add(msg);
        }
        return forMsg;
    }

    /**
     * periodically send hello packet to neighbor satellite nodes to check their status
     */
    public void helloProtocol() {
        // TODO helloProtocol
        List<Tuple<Message, Connection>> Collection =
                new ArrayList<Tuple<Message, Connection>>();

        if (SimClock.getTime() > getNextHelloTime()) {
            for (Connection con : this.getConnections()) {
                DTNHost to = con.getOtherNode(this.getHost());
                Message m = createHelloMessage(this.getHost(), to, "Hello, num: " + (helloMessageNum++), helloMessageSize, 0);
                Collection.add(new Tuple<Message, Connection>(m, con));
            }
            //simulate broadcast process
            broadcastHelloMessage(Collection);
        }
    }

    /**
     * Although The ONE does not have MAC layer, so it does not support broadcast,
     * this method can still be used to simulate the broadcast
     *
     * @param
     * @return
     */
    public boolean broadcastHelloMessage(List<Tuple<Message, Connection>> Collections) {
        boolean tryLabel = false;
        //TODO deleteMessage方法在startTransfer方法内部有用过
        //deleteMessage(m.getId(), true);
        for (Tuple<Message, Connection> t : Collections) {
            if (sendMsg(t)) {
                tryLabel = true;
            }
        }

        //TODO check this clear method
        if (tryLabel)
            helloMessages.clear();
        return tryLabel;
    }
    @Override
    public int receiveMessage(Message m, DTNHost from) {
        int recvCheck = checkReceiving(m, from);
        if (recvCheck != RCV_OK) {
            return recvCheck;
        }

        // seems OK, start receiving the message
        return super.receiveMessage(m, from);
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message m = super.messageTransferred(id, from);

        // 1.LEO satellite nodes receive process
        if (m.getProperty("Hello") != null &&
                (DTNHost) m.getProperty("Hello") == this.getHost()) {
//            Object o = m.getProperty("ManageNode");
//		    if (! (o instanceof DTNHost))
//		        throw new SimError("Confirm message error!");
//		  //TODO change the sequence
//		    LEOci.addManageHosts((DTNHost) o);
//		    // send feedback
//            if (sendConfirmFeedback((DTNHost) o)) {
//                // add manage hosts
//            }
        }
        return m;
    }

    /**
     * Creates a new confirm message from MEO node to LEO node
     *
     * @param from
     * @param to
     * @param id
     * @param size
     * @param responseSize
     * @return
     */
    public Message createHelloMessage(DTNHost from, DTNHost to, String id, int size, int responseSize) {
        Message m = new Message(from, to, id, size);
        m.setResponseSize(responseSize);
        m.updateProperty("Hello", to);
        //m.updateProperty("HelloInfo", from);
        ((ShortestPathFirstRouter) from.getRouter()).createNewMessage(m, helloTtl);
        helloMessages.add(m);

        return m;
    }

    /**
     * Creates a new Confirm message to the router.
     * The TTL of confirm message setting is different from normal message.
     *
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
     * @return next time to send confirm message to LEO satellite nodes
     */
    public double getNextHelloTime() {
        if (lastHelloCheckTime <= 0)
            return random.nextDouble() * (helloInterval / 2);
        else
            return lastHelloCheckTime + helloInterval;
    }

    /**
     * Update router table, find a routing path and try to send the message
     *
     * @param msg
     * @return
     */
    public boolean findPathToSend(Message msg) {
        // if user allow routing path information written into the message header
        if (msgPathLabel == true) {
            // if message header contains routing path information, this node should be intermediate node
            if (msg.getProperty(MSG_ROUTERPATH) == null) {
                Tuple<Message, Connection> t =
                        findPathFromRouterTabel(msg);
                //System.out.println(this.getHost()+" sending "+t+"  "+SimClock.getTime());
                return sendMsg(t);
            } else {
                Tuple<Message, Connection> t =
                        findPathFromMessage(msg);
                assert t != null : "Reading routing path from message header fail!";
                return sendMsg(t);
            }
        }
        else if (msg.getMessageType() == "request") {

            if(hasMessage(msg.getId())){
                removeFromMessages(msg.getId());
            }
            this.totalrequests++;
            this.lamta_average=totalrequests/SimClock.getTime();


            //在初始节点进行bestto节点的设置，其他节点不设置，在T=10时开始启用该策略
            if(msg.getFrom()==this.getHost()&&SimClock.getTime()>=50&&!totalpoplist.isEmpty()){
                DTNHost bestto=getthebesttonode(msg);
                msg.setTo(bestto);
//                System.out.println(" to change the msg tonode: "+" msg: "+msg.getId()+" message from: "+msg.getFrom()+" oringinal tonode: "+(DTNHost)msg.getOriginalDestination()
//                        +" best tonode: "+msg.getTo());
            }

           List<Integer> buffercont=getBuffercontent();
            //在转发途中遇到内容的处理
            if((msg.getMessageContentName()!=null&&buffercont.size()!=0)&&msg.getTo()!=this.getHost()&&!(boolean)msg.getProperty(isMessagegettobesttonode)){
                //如果内容名不为空&本地缓存空间不为空
               //System.out.println("763 coming in: ");
                updatepopularitylist(msg);
                if(buffercont.contains((Integer) msg.getMessageContentName())){
                        msg.setReceiveTime(SimClock.getTime());

                        for (MessageListener ml : this.mListeners) {
                            ml.messageTransferred(msg,msg.getFrom(),this.getHost(),true);
                        }
                        //1.更新缓存、流行度信息
                        //2.request 数据包不再转发，从incoming队列转到delivered队列
                      //messageAborted(msg.getId(), msg.getFrom(), 0);
                        //3.创建Data数据包，放入incoming队列

                        List<Message> dataMessages = createDataMessages(msg);
                        for (Message dataMsg : dataMessages){
                            if(!hasMessage(dataMsg.getId()))
                            createNewMessage(dataMsg);
                         }
                        T1++;
                        System.out.println(msg.getId()+" message from: " +msg.getFrom()+" message original tonode: "+msg.getOriginalDestination()
                                +" message found in the node: "+this.getHost()+" before to the bestto node: "+ msg.getTo()+" "+T1+" !!! ");
                        return true;

                }

                    Tuple<Message, Connection> t =
                            findPathFromRouterTabel(msg);
                    return sendMsg(t);
            }

            //转发至bestto节点时的策略,实际上转发至目的节点时(不管该目的节点是bestto node还是original node) 其执行的时MessageRouter中的逻辑345行
            else if((msg.getMessageContentName()!=null&&buffercont.size()!=0)&&(msg.getTo()==this.getHost())){
                for(Integer content : buffercont ){
                    if(msg.getMessageContentName()==content){
                        for (MessageListener ml : this.mListeners) {
                            ml.messageTransferred(msg,msg.getFrom(),this.getHost(),true);
                        }
                        //1.更新缓存、流行度信息
                        //2.request 数据包不再转发，从incoming队列转到delivered队列
                        //messageAborted(msg.getId(), msg.getFrom(), 0);
                        //3.创建Data数据包，放入incoming队列
                        List<Message> dataMessages = createDataMessages(msg);
                        for (Message dataMsg : dataMessages){
                            if(!hasMessage(dataMsg.getId()))
                                createNewMessage(dataMsg);
                        }
                        System.out.println(msg.getId()+" request= "+msg.getMessageContentName()+" message from: " +msg.getFrom()+" message original tonode: "+msg.getOriginalDestination()
                                +" message found in the bestto node: "+this.getHost()+" the bestto node: "+ msg.getTo());
                        return true;
                    }

                }
                System.out.println("message"+msg.getId()+" "+" not hit int this besttonode"+" "+this.getHost());
                DTNHost orginalto=(DTNHost) msg.getOriginalDestination();
                msg.setTo(orginalto);
                Tuple<Message, Connection> t =
                        findPathFromRouterTabel(msg);
                return sendMsg(t);
            }
            else {
                Tuple<Message, Connection> t =
                        findPathFromRouterTabel(msg);

                return sendMsg(t);
            }

        }
        else if (msg.getMessageType() == "data") {

            if (hasMessage(msg.getId())) {
                removeFromMessages(msg.getId());
            }
            Tuple<Message, Connection> t =
                    findPathFromRouterTabel(msg);
            return sendMsg(t);
        }

        else {
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
//            throw new SimError("Message: " + msg +
//                    " already arrive the destination! " + this.getHost());  
        	System.out.println("Message: " + msg +
                  " already arrive the destination! " + this.getHost());
        }
        if (routerPath == null)
        	return null;
        
        //try to find the next hop from routing path in the message header
        int nextHopAddress = -1;
        boolean waitLable = false;
        for (int i = 0; i < routerPath.size(); i++) {
            if (routerPath.get(i).getKey() == thisAddress) {
                nextHopAddress = routerPath.get(i + 1).getKey();//找到下一跳节点地址
                waitLable = routerPath.get(i + 1).getValue();//找到下一跳是否需要等待的标志位
                break;
            }
        }

        if (nextHopAddress > -1) {
            Connection nextCon = findConnection(nextHopAddress);
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
        List<Tuple<Integer, Boolean>> routerPath = this.routerTable.get(message.getTo());
        //System.out.println(this.getHost()+" send path "+routerPath+"  "+SimClock.getTime());
        //write the routing path into the header or not according to the 'msgPathLabel'
        if (msgPathLabel == true) {
            message.updateProperty(MSG_ROUTERPATH, routerPath);
        }
        if(message.getTo()==this.getHost()){
            Connection myfirstHop=findConnection(this.getHost().getAddress());

            Tuple<Message,Connection> t=new Tuple<>(message,myfirstHop);
            return  t;
        }
        Connection firstHop = findConnection(routerPath.get(0).getKey());

        if (firstHop != null) {
            Tuple<Message, Connection> t = new Tuple<Message, Connection>(message, firstHop);
            return t;
        } else {
            if (routerPath.get(0).getValue()) {
            	
                return null;
            } else {
            	this.routerTable.remove(message.getTo());
            	return null;
//                throw new SimError("No such connection: " + routerPath.get(0) +
//                       " at routerTable " + this);  
                
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
        shortestPathSearch(msg);
        //System.out.println(this.getHost()+"  "+routerTable+"  "+SimClock.getTime());
        if (this.routerTable.containsKey(msg.getTo())||this.getHost()==msg.getTo()) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Return current network topology in forms of temporal graph
     */
    public HashMap<DTNHost, List<DTNHost>> temporalGraphCaluculation() {
        HashMap<DTNHost, Coord> locationRecord = new HashMap<DTNHost, Coord>();
        HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();

        double radius = transmitRange;//Represent communication Radius
        //Get satellite movement model which store orbit-info of all satellites in the network
        if (! (this.getHost().getMovementModel() instanceof SatelliteMovement))
            return topologyInfo;

        SatelliteMovement movementModel = ((SatelliteMovement) this.getHost().getMovementModel());

        //Calculate the current coordinate of all satellite nodes in the network
        List<DTNHost> allHosts = this.getHosts();
        if (allHosts.size() <= this.getHost().getHostsList().size())
            allHosts = this.getHost().getHostsList();
        for (DTNHost h : allHosts) {
            //locationRecord.put(h, movementModel.getCoordinate(h, SimClock.getTime()));
            locationRecord.put(h, h.getLocation());          
        }
        //System.out.println(this.getHost()+" list: "+locationRecord+"  "+SimClock.getTime());
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
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination
     *
     * @param msg
     */
    public void shortestPathSearch(Message msg) {

        //double t0 = System.nanoTime();
        //double t0=SimClock.getTime();
        //System.out.println("start: "+t0);//用于统计路由算法的运行时间
        HashMap<DTNHost, List<DTNHost>> topologyInfo = temporalGraphCaluculation();//update the current topology information

        //TODO special situation
        if (topologyInfo.isEmpty()) {
            List<Tuple<Message, Connection>> tuples = new ArrayList<Tuple<Message, Connection>>();
            for (Connection con :this.getHost().getConnections()){
                tuples.add(new Tuple<Message, Connection>(msg, con));
                if (tryMessagesForConnected(tuples) != null)// try to send msg on each connection, once success, then return
                    return;
                tuples.clear();
            }
            return;
        }

        if (routerTableUpdateLabel == true)		//routerTableUpdateLabel == true则代表此次更新路由表已经更新过了，所以不要重复计算
            return;
        this.routerTable.clear();
        this.arrivalTime.clear();

        /**全网的传输速率假定为一样的**/
        double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        /**表示路由开始的时间**/

        /**添加链路可探测到的一跳邻居网格，并更新路由表**/
        List<DTNHost> searchedSet = new ArrayList<DTNHost>();
        List<DTNHost> sourceSet = new ArrayList<DTNHost>();
        sourceSet.add(this.getHost());//初始时只有源节点所
        searchedSet.add(this.getHost());//初始时只有源节点

        for (Connection con : this.getHost().getConnections()) {//添加链路可探测到的一跳邻居，并更新路由表
            DTNHost neiHost = con.getOtherNode(this.getHost());
            sourceSet.add(neiHost);//初始时只有本节点和链路邻居
           //只计算了到邻居节点的传输延迟，所有邻居节点的延迟都是同样的
            double time;
            double time_trans=msg.getSize()/transmitSpeed;//消息在下一节点eachNeighborNetgrid上的传输延迟
            double time_prop=proptime(this.getHost(),neiHost);//消息在thishost和neiHost上的传播延迟
            double time_quque=averagequeue(neiHost);//消息在neiHost上的平均排队延迟
            time_quque=0;
            time=time_prop+time_quque+time_trans;
            //System.out.println("111"+time_prop+" "+time_trans+" "+time_quque);

            List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
            Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
            path.add(hop);//注意顺序
            arrivalTime.put(neiHost, time);
            routerTable.put(neiHost, path);
        }
        /**添加链路可探测到的一跳邻居网格，并更新路由表**/

        int iteratorTimes = 0;
        int size = getHosts().size();
        boolean updateLabel = true;
        boolean predictLable = false;

        //arrivalTime.put(this.getHost(), SimClock.getTime());//初始化到达时间

        /**优先级队列，做排序用**/
        List<Tuple<DTNHost, Double>> PriorityQueue = new ArrayList<Tuple<DTNHost, Double>>();
        /**优先级队列，做排序用**/
        
        while (true) {//Dijsktra算法思想，每次历遍全局，找时延最小的加入路由表，保证路由表中永远是时延最小的路径
            if (iteratorTimes >= size)//|| updateLabel == false)
                break;
            updateLabel = false;
            
            for (DTNHost c : sourceSet) {
                List<DTNHost> neiList = topologyInfo.get(c);//get neighbor nodes from topology info

                /**判断是否已经是搜索过的源网格集合中的网格**/
                if (searchedSet.contains(c) || neiList == null)
                    continue;
                
                searchedSet.add(c);
                for (DTNHost eachNeighborNetgrid : neiList) {//startTime.keySet()包含了所有的邻居节点，包含未来的邻居节点
                    if (sourceSet.contains(eachNeighborNetgrid))//确保不回头
                        continue;
                    
                    double time;//这里没加传播延迟、排队延迟
//                    if(SimClock.getTime()>100){
//                        System.out.println("121KKK");
//                    }
                    double time_trans=msg.getSize()/transmitSpeed;//消息在下一节点eachNeighborNetgrid上的传输延迟
                    double time_prop=proptime(c,eachNeighborNetgrid);//消息在c和eachNeighborNetgrid上的传播延迟
                    double time_quque=averagequeue(eachNeighborNetgrid);//消息在eachNeighborNetgrid上的平均排队延迟
                    time_quque=0;
                    double time_this_c=arrivalTime.get(c);
                    time=time_this_c+time_prop+time_quque+time_trans;
//                    System.out.println(this.getHost().getAddress()+"  "+c.getAddress()+"  "+time_this_c+"   "+eachNeighborNetgrid.getAddress()+"   "
//                            +time_prop+"   "+time_trans+"   "+time_quque);
                    /**添加路径信息**/
                    List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
                    if (this.routerTable.containsKey(c))
                        path.addAll(this.routerTable.get(c));
                    Tuple<Integer, Boolean> thisHop = new Tuple<Integer, Boolean>(eachNeighborNetgrid.getAddress(), predictLable);
                    path.add(thisHop);//注意顺序
                    /**添加路径信息**/
                    /**维护最小传输时间的队列**/
                    if (arrivalTime.containsKey(eachNeighborNetgrid)) {
                        /**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
                        if (time <= arrivalTime.get(eachNeighborNetgrid)) {
//                            if (random.nextBoolean() == true && time - arrivalTime.get(eachNeighborNetgrid) < 0.1) {//如果时间相等，做随机化选择
//
//                                /**注意，在对队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
//                                int index = -1;
//                                for (Tuple<DTNHost, Double> t : PriorityQueue) {
//                                    if (t.getKey() == eachNeighborNetgrid) {
//                                        index = PriorityQueue.indexOf(t);
//                                    }
//                                }
//                                /**注意，在上面对PriorityQueue队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
//                                if (index > -1) {
//                                    PriorityQueue.remove(index);
//                                    PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
//                                    arrivalTime.put(eachNeighborNetgrid, time);
//                                    routerTable.put(eachNeighborNetgrid, path);
//                                }
//                            }

                            /**注意，在对队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
                            int index = -1;
                            for (Tuple<DTNHost, Double> t : PriorityQueue) {
                                if (t.getKey() == eachNeighborNetgrid) {
                                    index = PriorityQueue.indexOf(t);
                                }
                            }
                            /**注意，在上面对PriorityQueue队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
                            if (index > -1) {
                                PriorityQueue.remove(index);
                                PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                                arrivalTime.put(eachNeighborNetgrid, time);
                                routerTable.put(eachNeighborNetgrid, path);
                            }
                        }
                        /**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
                    } else {
                        PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                        arrivalTime.put(eachNeighborNetgrid, time);
                        routerTable.put(eachNeighborNetgrid, path);
                    }
                    /**对队列进行排序**/
                    sort(PriorityQueue);
                    updateLabel = true;
                }
            }
            iteratorTimes++;
            for (int i = 0; i < PriorityQueue.size(); i++) {
                if (!sourceSet.contains(PriorityQueue.get(i).getKey())) {
                    sourceSet.add(PriorityQueue.get(i).getKey());//将新的最短网格加入
                    break;
                }
            }
//			if (routerTable.containsKey(msg.getTo()))//如果中途找到需要的路徑，就直接退出搜索
//				break;
        }
        routerTableUpdateLabel = true;
        //double t1 = System.nanoTime();//用于统计路由算法的运行时间
        //double t1=SimClock.getTime();
        //System.out.println("Total Cost: "+ (t1-t0));
    }

    /**
     * Bubble sort algorithm 
     * @param distanceList
     * @return
     */
    public List<Tuple<DTNHost, Double>> sort(List<Tuple<DTNHost, Double>> distanceList) {
        for (int j = 0; j < distanceList.size(); j++) {
            for (int i = 0; i < distanceList.size() - j - 1; i++) {
                if (distanceList.get(i).getValue() > distanceList.get(i + 1).getValue()) {//从小到大，大的值放在队列右侧
                    Tuple<DTNHost, Double> var1 = distanceList.get(i);
                    Tuple<DTNHost, Double> var2 = distanceList.get(i + 1);
                    distanceList.remove(i);
                    distanceList.remove(i);//注意，一旦执行remove之后，整个List的大小就变了，所以原本i+1的位置现在变成了i
                    //注意顺序
                    distanceList.add(i, var2);
                    distanceList.add(i + 1, var1);
                }
            }
        }
        return distanceList;
    }

    /**
     * Find the corresponding DTNHost from host address in 
     * routing path information (contained in message header)
     * @param path
     * @return
     */
    public List<DTNHost> getHostListFromPath(List<Integer> path) {
        List<DTNHost> hostsOfPath = new ArrayList<DTNHost>();
        for (int i = 0; i < path.size(); i++) {
            hostsOfPath.add(this.getHostFromAddress(path.get(i)));//根据节点地址找到DTNHost
        }
        return hostsOfPath;
    }

    /**
     * Find the DTNHost according to its address
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
     * Find the specific connection according to neighbor node's address
     * @param address
     * @return
     */
    public Connection findConnection(int address) {
        List<Connection> connections = this.getHost().getConnections();
        for (Connection c : connections) {
            if (c.getOtherNode(this.getHost()).getAddress() == address) {
                return c;
            }
        }
        return null;
    }

    /**
     * Try to send the message through a specific connection
     * @param t
     * @return
     */

    public Message tryMessageToConnection(Tuple<Message, Connection> t) {
        if (t == null)
            throw new SimError("No such tuple: " + " at " + this);
        Message m = t.getKey();
        Connection con = t.getValue();
        int retVal = startTransfer(m, con);
        if (retVal == RCV_OK) {  	//accepted a message, don't try others
            return m;
        } else if (retVal > 0) { 	//系统定义，只有TRY_LATER_BUSY大于0，即为1
            return null;          	// should try later -> don't bother trying others
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
        /**检查所经过路径的情况，如果下一跳的链路已经被占用，则需要等待**/
        if (con.isTransferring() || ((ShortestPathFirstRouter) con.getOtherNode(this.getHost()).getRouter()).isTransferring()) {
            return true;//说明目的节点正忙
        }
        return false;
        /**至于检查所有的链路占用情况，看本节点是否在对外发送的情况，在update函数中已经检查过了，在此无需重复检查**/
    }

    /**
     * Try to send the message through a specific connection.
     *
     * @param t
     * @return
     */
    public boolean sendMsg(Tuple<Message, Connection> t) {
        if (t == null) {
            assert false : "error!";
            return false;
        } else {
        	// check the next hop is busy or not
            if (nextHopIsBusyOrNot(t) == true)
                return false;
            if (tryMessageToConnection(t) != null)
                return true;
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
        //判断该节点能否进行传输消息，存在以下情况一种以上的，直接返回，不更新,即现在信道已被占用：
        //情形1：本节点正在向外传输
        if (this.sendingConnections.size() > 0) {//protected ArrayList<Connection> sendingConnections;
            return true; // sending something
        }

        List<Connection> connections = getConnections();
        //情型2：没有邻居节点
        if (connections.size() == 0) {
            return false; // not connected
        }
        //情型3：有邻居节点，但自身与周围节点正在传输
        //模拟了无线广播链路，即邻居节点之间同时只能有一对节点传输数据!
        for (int i = 0, n = connections.size(); i < n; i++) {
            Connection con = connections.get(i);
            //isReadyForTransfer返回false则表示有信道在被占用，因此对于广播信道而言不能传输
            if (!con.isReadyForTransfer()) {
                return true;    // a connection isn't ready for new transfer
            }
        }
        return false;
    }

    /**
     * 此重写函数保证在传输完成之后，源节点的信息从messages缓存中删除
     */
    /**
     * get all satellite nodes info in the movement model
     *
     * @return all satellite nodes in the network
     */
    public List<DTNHost> getHosts() {
        return new ArrayList<DTNHost>(((SatelliteMovement) this.getHost().getMovementModel()).getHosts());
    }


    /**tangjin*/

    //设置缓存
    private void SetBuffercontent(){
        Random random=new Random();
        //List<Integer> buffercontent=new ArrayList<Integer>();
        if(this.iscachenablednode()){
            for(int i=0;i<BufferSize;i++){
                int contentid=random.nextInt(2000);
                //int contentid=i+1;
                contentid+=1;
                if(this.buffercontent.contains(contentid)){
                    i--;
                    continue;
                }
                this.buffercontent.add(contentid);
            }
        }
    }

    //得到缓存内容
    public List<Integer> getBuffercontent(){
        return this.buffercontent;
    }

    //根据缓存内容创建流行度表
    public void creatpopularityinfolist(List<Integer> buffercontentent){
        for(Integer content:buffercontentent){
            Popularityinfo temppoplistforeachcontent=new Popularityinfo(content,this.getHost());
            popularityinfoList.put(content,temppoplistforeachcontent);
        }
        lasttimepopularinfolist.putAll(popularityinfoList);
    }


    //创建data包
    public List<Message> createDataMessages(Message requestMsg){
        List<Message> dataMessages = new ArrayList<Message>();
        dataMessages=super.createDataMessages(requestMsg);
        return dataMessages;
    }

    //得到最optimal node,这里要用到上一周期全网下发的流行度表totalpoplist
    public DTNHost getthebesttonode(Message msg){
//        List<DTNHost> bestto=predictbestto(msg);
        //这里的possbestto中存放的是针对某一个内容的可能的节点，以及其被缓存的概率(1-p(missingcaching))

        Map<DTNHost,Double> possiblebestto=new HashMap<DTNHost,Double>();
        int contentid=(Integer) msg.getMessageContentName();
        for(Map<Integer,Popularityinfo> hostpoplist : totalpoplist.values()){
            if(hostpoplist.containsKey(contentid)){
                double phit;
                DTNHost host=hostpoplist.get(contentid).getHostfrompoplist();
                //phit=cachingprobablity(host,contentid);//基于排名的预测方案
                if(isprediction==1){
                    phit=possionpredictphit(hostpoplist,contentid);//基于泊松过程的预测方案
                }

                else {
                    phit=1;
                }
                possiblebestto.put(hostpoplist.get(contentid).getHostfrompoplist(),phit);
            }
        }

        //这里的besttodelay存放的是去往各个可能的目的节点，及其延时（可以用跳数代替）
        Map<DTNHost,Double> besttodelay=new HashMap<DTNHost,Double>();
        for(DTNHost host:possiblebestto.keySet()){
            double delay;
            delay=tofinddelay(this.getHost(),host);
            besttodelay.put(host,delay);
        }

        //这里的besttodelay存放的是去往各个可能的目的节点，及其失败后，所耗费的总延时（可以用跳数代替）
        Map<DTNHost,Double> besttodelayunderpiss=new HashMap<DTNHost,Double>();
        for(DTNHost host:possiblebestto.keySet()){
            double delay;
            //原先去往可能的目的节点的延迟
            delay=besttodelay.get(host);
            //由于该可能的目的节点没有该content，进而又要向原目的节点转发的总耗时
            delay+=tofinddelay(host,msg.getTo());

            besttodelayunderpiss.put(host,delay);
        }
        double pview_hit=0;
        //这里的besttototalcost存放的是去往的各个可能的目的节点，及其总代价
        Map<DTNHost,Double> besttototalgain=new HashMap<DTNHost,Double>();
        for(DTNHost host:possiblebestto.keySet()){
            double totalcost;
            double phit;
            double pmiss;
            double delayphit;
            double delaypmiss;
            double originaldelay;
            phit=possiblebestto.get(host);
            pmiss=1-phit;
            delayphit=besttodelay.get(host);
            delaypmiss=besttodelayunderpiss.get(host);
            originaldelay=tofinddelay(this.getHost(),msg.getTo());
            totalcost=phit*(originaldelay-delayphit)-pmiss*delaypmiss;
            besttototalgain.put(host,totalcost);
            pview_hit=phit;
        }

        //返回最佳节点
        Map<DTNHost,Double> sorted_besttototalcost=new HashMap<DTNHost,Double>();
        sorted_besttototalcost=sortDescend(besttototalgain);
        Iterator<Map.Entry<DTNHost,Double>> it=sorted_besttototalcost.entrySet().iterator();


        DTNHost bestto;
        double besttotime;
        if (it.hasNext()){
            Map.Entry<DTNHost,Double> entry=it.next();
            bestto=entry.getKey();
            besttotime=entry.getValue();
            System.out.println(besttotime);
            System.out.println("!!!"+T1+" !!"+T2+" !"+T3+" !-_-"+T4+"\n");
            if(sorted_besttototalcost.isEmpty())return msg.getTo();
            else if(besttotime<0){
//                if(bestto.getRouter().buffercontent.contains(contentid)&&besttotime>0){
//                    P_hit++;
//                    System.out.println("it hasbeen cached but you dont choose!!!"+P_hit+"\n");
//                    System.out.println("your p_hit: "+pview_hit+"\n");
//                }
                return msg.getTo();

            }
            else  {
//                if(!bestto.getRouter().buffercontent.contains(contentid)){
//                    P_miss++;
//                    System.out.println("it notbeen cached but you choose???"+P_hit+"\n");
//                    System.out.println("your p_miss: "+(1-pview_hit)+"\n");
//                }
                msg.updateProperty(MessageOriginalDestination,msg.getTo());
                return   bestto;

            }
        }
        else return msg.getTo();
    }



    //根据基于内容排名进行缓存内容概率的预测
    public Double cachingprobablity(DTNHost relayhost,Integer contentid){
        Map<Integer,Popularityinfo> relayhostpoplist=totalpoplist.get(relayhost);
        return feedbackpredictphit(relayhostpoplist,contentid);
    }
    public double feedbackpredictphit(Map<Integer,Popularityinfo> hostpoplist,int contentid){
        double phit;
        double a;
        int rank_contentid=1;
        Map<Integer,Double> contentidcollectioninrelayhost=getpopularestcontentid(hostpoplist);
        Map<Integer,Double> sortedcontentidcollectioninrelayhost=sortDescend(contentidcollectioninrelayhost);
        Iterator<Map.Entry<Integer,Double>> it1=sortedcontentidcollectioninrelayhost.entrySet().iterator();
        while(it1.hasNext()) {
            if (it1.next().getKey() == contentid) {
                break;
            } else {
                rank_contentid++;
            }
        }
        DTNHost hostpoplist_host=hostpoplist.get(contentid).getHostfrompoplist();
        phit=gettherelationbetweenpoplistandrank(hostpoplist_host.getRouter().feedbackinfo,rank_contentid);

        return phit;

    }


    //基于possion过程的缓存内容概率预测
    public double possionpredictphit(Map<Integer,Popularityinfo> hostpoplist,int contentid){
        double p_cached=0;
        Map<Integer,Double> contentidcollectioninrelayhost=getpopularestcontentid(hostpoplist);
        Map<Integer,Double> sortedcontentidcollectioninrelayhost=sortDescend(contentidcollectioninrelayhost);
        int i=1;
        Iterator<Map.Entry<Integer,Double>> it=sortedcontentidcollectioninrelayhost.entrySet().iterator();
        while (it.hasNext()&&i<BufferSize){
            it.next();
            i++;
        }
        Entry<Integer,Double> temp=it.next();
        int id_tre=temp.getKey();//得到缓存门限的内容名
        double p_tre=temp.getValue();//定义缓存的门限
        double p_content=contentidcollectioninrelayhost.get(contentid);//得到内容i在流行度表中上个T时刻的流行度
        double lamta_contentid=getlamta_content(hostpoplist,contentid);//定义内容i的请求到达速率
        double lamta_average=getlamta_average(hostpoplist.get(contentid).getHostfrompoplist());//定义请求平均到达速率
        double w=0.1;//定义修正因子
        double p_tre_now=(1+w)*p_tre;//得到当前时刻的缓存门限
        double K;//K值为该内容在（T,T+t）内需要达到的请求数
        double tau=SimClock.getTime()-(T-1)*collectpoplistinterval;
        double totalreq_T=gettotalrequestuntilllast_T(hostpoplist);//得到在T时刻的请求数
        double totalreq_Ttau=totalreq_T+lamta_average*tau;//得到在T+tau时刻的请求数
        K=(p_tre_now*totalreq_Ttau-(totalreq_Ttau/(1+(lamta_average*tau)/totalreq_T))*p_content);
        double K1=ceil(K);
        System.out.println("p_tre_now: "+p_tre_now+" p_content: "+p_content+" tau: "+tau+" totalreq_T: "+totalreq_T
                +" total_Ttau: "+totalreq_Ttau);
        if(K1<=0){
            p_cached=1;
        }
        else {
            double p_temp=0;
            for(int j=0;j<=K1;j++){
                double temp1=Math.pow(Math.E,-lamta_contentid*tau);

                double temp2=Math.pow(lamta_contentid*tau,j);

                double temp3=recursion(j);

                p_temp+=(temp1*temp2/temp3);
            }
            p_cached=1-p_temp;

        }
        System.out.println("K :"+K+" K1: "+K1+" p_cached: "+p_cached);
        System.out.println("request lamta content: "+lamta_contentid+" request average: "+lamta_average+"\n");

        return p_cached;

    }

    //取整函数
    public double ceil(double num){
        if(Math.ceil(num)-num<0.5)return Math.ceil(num);
        else return Math.ceil(num)-1;
    }

    //阶乘函数
    public double recursion(int num){
        if(num==0)return 1;
        else {
            int n=1;
            for(int i=1;i<=num;i++){
                n=i*n;
            }
            return n;
        }
    }

    //发现两个几点之间的延迟函数
    public Double tofinddelay(DTNHost from,DTNHost to){
        double delay=10;
        if(this.arrivalTime.containsKey(to)){
            if(this.getHost()==from){
                delay=this.arrivalTime.get(to);
            }
            else {
                DTNHost relayhost=from;
                //得到中继节点的arrivetime;
                HashMap<DTNHost, Double> relayhostarrivalTime =this.getHost().getHostsList().get(relayhost.getAddress()).getRouter().arrivalTime ;
                //得到从中继节点去往目的节点的时间；
                if(relayhostarrivalTime==null){
                    throw new SimError("relayhostarrivalTime is null");
                }
//                if(!relayhostarrivalTime.containsKey(to)){
//                    throw new SimError("relayhostarrivalTime do not have the time to the besttonode");
//                }
                if(relayhostarrivalTime.containsKey(to)){
                    delay=relayhostarrivalTime.get(to);
                }

            }
        }
        return delay;
    }

    //传播时延计算
    public double proptime(DTNHost node1,DTNHost node2){
        double t_prop=0;
        double distancebetween=this.getHost().getDistance(node1,node2);
        t_prop=distancebetween/(300000);
        //System.out.println(node1+"  "+node2+"  "+t_prop);
        return t_prop;
    }

    //排队时延计算
    public double averagequeue(DTNHost node){
        double t_que=0;
        t_que=node.getRouter().getaveragequetime();
        return  t_que;
    }
    /**tangjin*/

}
