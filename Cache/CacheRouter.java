/*
 * copyright 2017 ustc, Infonet
 */
package Cache;

import java.util.*;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;
import routing.MessageRouter;
import routing.Popularityinfo;

public class CacheRouter extends MessageRouter {

	/** bitMap���ڶ�chunkID����ӳ��    */
	private ArrayList<Integer> bitMap = new ArrayList<Integer>();
	/**����һ����ʱ�Ķ��У����ڶ��м̽ڵ�õ�chunk������Ϣ�洢 */
	protected Queue<Message> tempQueue = new LinkedList<Message>();
	/** ��Ҫ�����ά��������ʽ���������ݽ��д洢 */
	protected HashMap<String,HashMap<String,Message>> MessageHashMap = new HashMap<String,HashMap<String,Message>>();
	/** �����ж��ļ��Ƿ�õ�ȷ�ϣ��Ӷ������Ƿ���Ҫ�ش�  */
	private HashMap<String, ArrayList<Object>> judgeForRetransfer = new HashMap<String, ArrayList<Object>>();	
	/** �����ж��ش�ʱ�䣬�����趨Ϊ100s */
	protected double time_out = 10;
	/** �����ж��ش���������ʼΪ0���趨����ش�3��*/
	protected int reTransTimes = 3;
	/**������ack����time_waitʱ�� */
	protected double time_wait = 20;
	/** ����Ӧ����ĵȴ�ʱ�� time_free */
	protected double time_free = 3.5*time_out;
	/** �����жϰ������� */
	public final static String SelectLabel = "PacketType";
	/** �½�һ���ļ�buffer */
	public static final String F_SIZE_S = "filebuffersize";
	/** �ļ������С*/
	private int filebuffersize;
	/** Host where this router belongs to */
	private DTNHost host;
	/** ��һ����Ϣ·�� */
	private MessageRouter router;
	
	/** ���ְ��ı�ʶID �����*/
	public static final String Request_Msg = "Request_";
	/** ���ְ��ı�ʶID Ӧ���*/
	public static final String Response_Msg = "Response_";
	/** ���ְ��ı�ʶID ���ư�*/
	public static final String Control_Msg = "Control_";
	/** ���ְ��ı�ʶID ���ư���ȷ�ϰ�*/
	public static final String Ack2Ctrl_Msg = "Ack2Ctrl_";
	/** ���ְ��ı�ʶID �������ȷ�ϰ�*/
	public static final String Ack2Request_Msg = "Ack2Request_";
	/** ��Ӧ��Ϣǰ׺ */
	public static final String RESPONSE_PREFIX = "R_";
	/** �ж��Ƿ���Ҫ���������*/
	public String DropMessage;
	private Map<Integer, Popularityinfo> popularityinfoList=new HashMap<>();
	
	/**
	 * Initializes the router; i.e. sets the host this router is in and
	 * message listeners that need to be informed about message related
	 * events etc.
	 * @param host The host this router is in
	 * @param mListeners The message listeners
	 */
	public void init(DTNHost host, List<MessageListener> mListeners) {
		Settings s = new Settings("userSetting"); 
		DropMessage = s.getSetting("RandomDropMessage"); //�������ļ���ȡһ���Ƿ���Ҫ�������
		
		this.host = host;
		this.router = host.getRouter();
	}
	
	public CacheRouter(CacheRouter r) {
		super(r);
		this.filebuffersize = r.filebuffersize;  // ��д�ļ������С
	}
	
	public CacheRouter(Settings s) {
		super(s);
		this.filebuffersize = Integer.MAX_VALUE; 	// ���ļ������С ��д
		if (s.contains(F_SIZE_S)) {
			this.filebuffersize = s.getInt(F_SIZE_S);
		}
	}
	
	@Override
	public CacheRouter replicate() {
		return new CacheRouter(this);
	}
	
	/**
	 * Called when a connection's state changes. If energy modeling is enabled,
	 * and a new connection is created to this node, reduces the energy for the
	 * device discovery (scan response) amount
	 * @param @con The connection whose state changed
	 */
	@Override
	public void changedConnection(Connection con) {	
		
	}
	
	/** -------------------------- �Դ�����޸�  --------------------------- */
	public DTNHost getHost(){
		return this.host;
	}
	
	/** ��ӵ���Ӧ���ļ���������  addToFileBuffer() */       
	protected void addToFileBuffer(Message m, boolean newMessage) {
		if ( m.getResponseSize() ==0){	//��message��ȡ��file��
			File ee = m.getFile();				
			this.getHost().getFileBuffer().put(m.getFilename(), ee); // �ŵ���Ϣ������FileBuffer��
		}
	}	
	
	/** ���chunk����Ӧ��chunkBuffer��  	*/
	protected void addToChunkBuffer(Message m){
		if(m.getProperty(SelectLabel)== (Object)1){
			// �ж��Ƿ���ڶ�Ӧ��Ƭ��Hash����������ֱ�ӷ��룬�����½�
			if(this.getHost().getChunkBuffer().containsKey(m.getFilename())){
				this.getHost().getChunkBuffer().get(m.getFilename()).put(m.getChunkID(),m.getFile());
			}	else{
				HashMap<String,File> NewHashMap = new HashMap<String,File>();
				NewHashMap.put(m.getChunkID(),m.getFile());
				this.getHost().getChunkBuffer().put(m.getFilename(), NewHashMap);
			}
		}
	}
	
	/** ��bitMap������Ԫ���������   */
	public void setZeroForBitMap(){
		this.bitMap.clear();
		for(int i=0;i<10;i++)
			bitMap.add(0);
	}
    /** �õ�����ļ��Ļ����Сfilebuffersize */
	public int getFileBufferSize(){
		return this.filebuffersize;
	}
	
	/** �õ���ǰ·�ɵ��ش�buffer��*/
	public HashMap<String,ArrayList<Object>> getJudgeForRetransfer(){
		return this.judgeForRetransfer;
	}
	
	/** ���մ�������Ϣ���뵽�ж��Ƿ���Ҫ�ش�buffer�� */
	public void putJudgeForRetransfer(Message m){		
		switch((int) m.getProperty(SelectLabel)){
		case 0:{
			ArrayList<Object> arraylist = new ArrayList<Object>();
			arraylist.add(0, m);
			arraylist.add(1, this.time_out);
			arraylist.add(2, this.reTransTimes);
			this.judgeForRetransfer.put(m.getId(), arraylist);
			return;
		}
//		case 1:{  // Ӧ������Բ��������
//			ArrayList<Object> arraylist = new ArrayList<Object>();
//			arraylist.add(0, m);
//			arraylist.add(1, this.time_free);
//			arraylist.add(2, -1);
//			this.judgeForRetransfer.put("Chunk"+m.getInitMsgId(), arraylist);
//			return;
//		}
		case 2:{
			ArrayList<Object> arraylist = new ArrayList<Object>();
			arraylist.add(0, m);
			arraylist.add(1, this.time_out);
			arraylist.add(2, this.reTransTimes);
			this.judgeForRetransfer.put(m.getId(), arraylist);
			return;
		}
		case 3:{
			ArrayList<Object> arraylist = new ArrayList<Object>();
			arraylist.add(0, m);
			arraylist.add(1, this.time_wait);
			arraylist.add(2, this.reTransTimes);
			this.judgeForRetransfer.put(m.getId(), arraylist);
			return;
		}
		case 4:{	//	�����ش��Ļ���
			ArrayList<Object> arraylist = new ArrayList<Object>();
			arraylist.add(0, m);
			arraylist.add(1, this.time_wait);
			arraylist.add(2, this.reTransTimes);
			this.judgeForRetransfer.put(m.getId(), arraylist);
			return;
		}
		}
	}
	
	/** ���´�ȷ����Ϣbuffer�е���Ϣ */
	public void updateReTransfer(){
		
		/**	������Ҫ�Դ�ȷ����Ϣ�����е���Ϣ��ȷ��ʱ����£����ж��Ƿ��ڣ������ش���*/
//		System.out.println("ˢ����������С��"+this.judgeForRetransfer.values().size());
		
		for( ArrayList<Object> reTrans : this.judgeForRetransfer.values()){	
			System.out.println("��ˢ�±����С��"+this.judgeForRetransfer.size());
			reTrans.set(1, (double)reTrans.get(1)-10*(0.01));
			Message n = (Message)reTrans.get(0);
			String s = n.getId();
			
			if((double)reTrans.get(1)<=0){	//�ж�����ʱ���Ƿ��ڣ�
				Message m = (Message)reTrans.get(0);
				switch((int) m.getProperty(SelectLabel)){
					case 0:{
						if(this.getHost().getFileBuffer().containsKey(m.getFilename())==false){	// ����������У��Ͳ����ط�������Ϣ��û�вŷ�
							if((int)reTrans.get(2)>0){	//�ж��ش������Ƿ�����
								Message reqMessage = new Message(m.getFrom(),m.getTo(),m.getId(), m.getResponseSize());
								reqMessage.setInitMsgId(m.getInitMsgId());
								reqMessage.updateProperty(SelectLabel, 0);						//	��ʶΪ���ư�
								reqMessage.setFilename(m.getFilename());
								reqMessage.setZeroForBitMap();
								reqMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//	������Ϣ����ʱ��
								
								this.judgeForRetransfer.get(m.getInitMsgId()).set(1, this.time_out);
								int i = (int) this.judgeForRetransfer.get(m.getInitMsgId()).get(2);
				                this.judgeForRetransfer.get(m.getInitMsgId()).set(2, i-1); 				//	�ش���������һ��
				                this.createNewMessage(reqMessage);
							}
							else{
								this.judgeForRetransfer.remove(m.getInitMsgId());
							}
						}
						else{
							this.judgeForRetransfer.remove(m.getInitMsgId());
						}
						return;
					}
					
					/** ����Ӧ�����time_free���ڣ������ж��ڴ����Ƿ��ж�Ӧ��Ӧ����� �еĻ�ɾ�ˣ�Ȼ��ɾ����ȷ����Ϣ�����еĴ���Ϣ��*/
					case 1:{ 		
						if(this.MessageHashMap.containsKey(n.getFilename())){
							this.MessageHashMap.remove(n.getFilename());
						}
						if(this.getHost().getChunkBuffer().containsKey(n.getFilename())){
							this.getHost().getChunkBuffer().remove(n.getFilename());
						}
						this.judgeForRetransfer.remove("Chunk"+n.getInitMsgId());
					}
					
					case 2:{	 
						
						if((int)reTrans.get(2)>0){	//�ж��ش������Ƿ����꣬�ش����ư�
							Message ctrMessage = new Message(m.getFrom(),m.getTo(),
									 Control_Msg + m.getInitMsgId(), m.getResponseSize());
							
							ctrMessage.setInitMsgId(m.getInitMsgId());
							ctrMessage.updateProperty(SelectLabel, 2);	//	��ʶΪ���ư�
							ctrMessage.setFilename(m.getFilename());
							ctrMessage.setZeroForBitMap();
							ctrMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//	������Ϣ����ʱ��
							
							this.judgeForRetransfer.get(Control_Msg+m.getInitMsgId()).set(1, this.time_out);//	ˢ���ش�ʱ��
			                int j = (int) this.judgeForRetransfer.get(Control_Msg + m.getInitMsgId()).get(2);
			                this.judgeForRetransfer.get( Control_Msg + m.getInitMsgId()).set(2, j-1); 		//	�ش���������һ��
			                this.createNewMessage(ctrMessage);
			                System.out.println("�ط����ư���Ϣ��������");
			        		System.out.println("IB�ɹ������ļ���"+"  "+this.getHost()+"   "+ctrMessage.getProperty(SelectLabel)+ "  "
			        				+ctrMessage.getFilename()+" "+ctrMessage.getChunkID()+"  "
			        				+ctrMessage.getId()+" "+ctrMessage.getFrom()+"  "+ctrMessage.getTo()
			        				+"  "+"��ʼ��Ϣ���ƣ�"+"  "+ctrMessage.getInitMsgId()
			        				+" "+"��Ϣ����ʱ�䣺"+"  "+ ctrMessage.getCreationTime()
			        				+"  "+"��Ϣ����ʱ�䣺"+"  "+ ctrMessage.getReceiveTime());
						}
						else{
							this.judgeForRetransfer.remove(Control_Msg + m.getInitMsgId());
						}
						return;
					}
					
					case 3:{		// �Կ��ư���ȷ����Ϣ
						this.judgeForRetransfer.remove(s);
						return;
					}
					case 4:{		// ���������ȷ����Ϣ��
						this.judgeForRetransfer.remove(s);
						return;
					}
				
				}
			}
		}	
	}

	
	/**
	 * Creates a new message to the router.
	 * @param m The message to create
	 * @return True if the creation succeeded, false if not (e.g.
	 * the message was too big for the buffer)
	 */
	@Override
	public boolean createNewMessage(Message m) {
		if( DropMessage.indexOf("true") >= 0){	// �������ģʽ���ٷ�֮һ�ĸ��ʶ���
			// ��Բ�ͬ���͵İ�����������������
			Random random = new Random();
			int r = random.nextInt(1000);
			if(r!=0){
				m.setTtl(this.msgTtl);
				this.router.addToMessages(m, true);		
				return true;
			} else{
				System.out.println("++++++++++++++++++++����ʧ�ܵ���Ϣ�ǣ�" + "  "
						+ this.getHost() + "   " + "��Ϣ�����ǣ�" + " "
						+ m.getProperty(SelectLabel) + "  " + m.getFilename() + " "
						+ m.getChunkID() + "  " + m.getId() + " " + m.getFrom()
						+ "  " + m.getTo());
				return false;
			}
		} else{
			m.setTtl(this.msgTtl);
			this.router.addToMessages(m, true);		
			return true;
		}
	}

	/**
	 * ��Ϣ����Ŀ�Ľڵ㣬�����ǵ�һ�ε���
	 * @param aMessage Ϊ��Ӧ�������Ϣ
	 */
	public void DestinationCache(Message aMessage){
		if (aMessage.getProperty(SelectLabel) == (Object) 0) { // ����һ�������
			RequestMessage_Destination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 1) { // ����һ��Ӧ�����Я��chunk�ļ�
			ResponseMessage_Destination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 2) { // ����һ�����ư�
			ControlMessage_Destination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 3) { // ���ǶԿ��ư���ȷ�ϰ�
			Ack2CtrlMessage_Destination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 4) { // ���Ƕ��������ȷ�ϰ�
			Ack2RequestMessage_Destination(aMessage);
		}
		
	}

	
	/**
	 * ������Ŀ�Ľڵ�������0
	 * @param aMessage
	 */
	private void RequestMessage_Destination(Message aMessage) {
		/** �����������������Ack2Requestȷ�ϰ���ʧ��ɵ��ط�����Ҫtime_wait���� */
		if(this.judgeForRetransfer.containsKey(Ack2Request_Msg + aMessage.getInitMsgId())){
			
			/** Ҳ��������Ack2Requestȷ�ϰ���ʧ��ɵ��ط����ư�������ֱ�ӻظ�ȷ�ϰ�����*/
			Message m = (Message)this.judgeForRetransfer
						.get(Ack2Request_Msg + aMessage.getInitMsgId()).get(0);

			Message ackMessage = new Message(m.getFrom(), m.getTo(),
						Ack2Request_Msg + m.getId(), m.getResponseSize());
			ackMessage.setInitMsgId(m.getInitMsgId());
			ackMessage.updateProperty(SelectLabel, 4);	//��ʶΪ���ư�
			ackMessage.setFilename(m.getFilename());
			ackMessage.setZeroForBitMap();
			ackMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//������Ϣ����ʱ��
			
			this.judgeForRetransfer.get(Ack2Request_Msg + m.getInitMsgId()).set(1, this.time_wait);	//ˢ���ش�ʱ��
            createNewMessage(ackMessage);

		} else{
			/** ���յ�������Ϣ֮�����Ƚ���ȷ��*/
			createAcknowledgeMessage(aMessage);
			if (this.getHost().getFileBufferForFile(aMessage)!=null) {
				/** ���ļ����з�Ƭ������Ӧ���*/
				createResponseMessage(aMessage);
				/** Ӧ����Ϣ����֮��Ӧ�÷���һ�����ư�*/
				createControlMessage(aMessage);	
	        } else {
				System.out.println("��ΪĿ�Ľڵ�ʱ�����ִ���Ŀ�Ľڵ���û�ж�Ӧ���ļ���");
			}
		}
	}
	
	/**
	 * ������Ŀ�Ľڵ��Ӧ���1
	 * @param aMessage
	 */
	private void ResponseMessage_Destination(Message aMessage) {
		/** ΪӦ������ϼ�ʱ�� Time_free,�����ж�Ӧ����ļ�ʱ���ڴ�ȷ����Ϣ���Ƿ���ڣ� ���ڵĻ����£��������ڣ�������һ��*/
		if (this.judgeForRetransfer.containsKey("Chunk" + aMessage.getInitMsgId())) {
			this.judgeForRetransfer.get("Chunk" + aMessage.getInitMsgId()).set(
					1, this.time_free);
		} else {
			this.putJudgeForRetransfer(aMessage);
		}
		
		/** ��chunkBuffer�з����ļ�*/
		if (this.getHost().getFileBufferForFile(aMessage) == null) {
			addToChunkBuffer(aMessage);
		}
		
		/** ����Ϣ�����ڶ����ط�,���м̽ڵ㵱��Ŀ�Ľڵ���д���*/
		if (this.MessageHashMap.containsKey(aMessage.getFilename())) {
			this.MessageHashMap.get(aMessage.getFilename()).put(aMessage.getChunkID(), aMessage);
		}
	}
	
	/**
	 * ������Ŀ�Ľڵ�Ŀ��ư�2 
	 * @param aMessage
	 */
	private void ControlMessage_Destination(Message aMessage) {
		if(this.getHost().getFileBuffer().containsKey(aMessage.getFilename())==false){// Ŀ�Ľڵ��в������ļ�
			/** Ҳ��������ackȷ�ϰ���ʧ��ɵ��ط����ư�������ֱ�ӻظ�ȷ�ϰ�����.�������ڴ�ʱ��chunkBuffer���Ѿ�û����chunk�ļ���*/
			boolean a = false;
			for(int i=0;i<10;i++){					
				if(this.getHost().getChunkBuffer().containsKey(aMessage.getFilename())){
					if (this.getHost()
							.getChunkBuffer()
							.get(aMessage.getFilename())
							.containsKey(aMessage.getFilename() + "ChunkID" + i))
						a = true;
				} 
				if(a==true) break;
			}
			if(	a==false ){		
				this.NoChunkInChunkBuffer(aMessage);
			} else{
				// ����chunkBuffer��chunk�Ƿ����룬�ظ�ȷ�ϰ�
				boolean b = true;						//b=1 Ĭ��Ϊ����
				this.setZeroForBitMap();				//�� bitmap�������
				for(int i=0;i<10;i++){
					if (this.getHost()
							.getChunkBuffer()
							.get(aMessage.getFilename())
							.containsKey(aMessage.getFilename() + "ChunkID" + i))
						this.bitMap.set(i, 1);
					else 
						b = false;  
				}
				if(b == true){	//  �ж�����������
					if (this.getHost().getFreeFileBufferSize() < 0) {	//�ж��ڴ��Ƿ����������Ļ�ɾ���ڴ�,�����ļ�����ʱ��
						this.getHost().makeRoomForNewFile(0);   		//��Ҫʱ��ɾ����Щ������յ��Ҳ����ڴ������Ϣ
						System.out.print("+++++++++++++++++++++		ɾ���ɹ�	 ++++++++++++++++++++"+"\n");
					}						
					//�ж��ļ���Ƭ���������£������ļ�������
					PutIntoFileBuffer(aMessage);
					System.out.println("-------------------------" 
										+ " Ŀ�Ľڵ� " + this.getHost() 
										+ " �����ļ���" + aMessage.getFilename()
										+ "--------------------------");
				}
				/** �ظ�ȷ�ϰ� */
				createAck2CtrlMessage(aMessage);
			}
		}else{
			System.out.println("Ŀ�Ľڵ����Ѵ����ļ���"+aMessage.getFilename());
		}
	}
	
	
	/**
	 * ����Կ��ư���ȷ�ϰ�3������bitmap���д���
	 * @param aMessage
	 */
	private void Ack2CtrlMessage_Destination(Message aMessage) {
		boolean b = true;		//�����ж��Ƿ���Ҫ�ٻظ�һ�����ư���Ĭ��Ϊ����Ҫ��
		File f = this.getHost().getFileBufferForFile(aMessage);				
				
		for(int i=0; i<10; i++){
			File chunk = f.copyFrom(f);
			if(aMessage.getBitMap().get(i)!=1){		
				for(int j=i*10;j<i*10+10;j++){					
					chunk.getData().add(j-i*10,f.getData().get(j));
				}
				Message res = new Message(this.getHost(), aMessage.getFrom(),
										  Response_Msg + aMessage.getInitMsgId()+i,
										  aMessage.getResponseSize(), chunk);
				res.setInitMsgId(aMessage.getInitMsgId());
				res.setResponseSize(0);				
				res.setFilename(aMessage.getFilename());
				res.setChunkID(aMessage.getFilename()+"ChunkID"+i);	
				res.updateProperty(SelectLabel,1);	
				this.createNewMessage(res);
				b = false;
			}	
		}	
		
		if(b==false){	//��b=false��֤���а���ʧ����ʱ��Ҫ�ٷ���һ�����ư�
			Message ctrMessage =new Message(this.getHost(),aMessage.getFrom(),
					Control_Msg + aMessage.getId(), aMessage.getResponseSize());
			
			ctrMessage.setInitMsgId(aMessage.getInitMsgId());
			ctrMessage.updateProperty(SelectLabel, 2);			//��ʶΪ���ư�
			ctrMessage.setFilename(aMessage.getFilename());
			ctrMessage.setZeroForBitMap();
			ctrMessage.setTime(SimClock.getTime()+11*0.01, SimClock.getTime()+11*0.01);	//������Ϣ����ʱ��
            this.createNewMessage(ctrMessage);
            
			if(this.judgeForRetransfer.containsKey(Control_Msg+aMessage.getInitMsgId())== true){
				this.judgeForRetransfer.get(Control_Msg+aMessage.getInitMsgId()).set(1, this.time_out);	//ˢ���ش�ʱ��
				int m = (int) this.judgeForRetransfer.get(Control_Msg+aMessage.getInitMsgId()).get(2);
	            this.judgeForRetransfer.get(Control_Msg + aMessage.getInitMsgId()).set(2, m-1); 	//�ش���������һ��
			} //���򲻹ܣ��൱�������ش���ʧ�ܣ��������ڳ�ʱ���ã�����ɾ��������
			
        } else{
			this.judgeForRetransfer.remove(Control_Msg+aMessage.getInitMsgId());
		}
	}
	
	
	
	/**
	 * ������������ȷ�ϰ�4
	 * @param aMessage
	 */
	private void Ack2RequestMessage_Destination(Message aMessage) {
		this.judgeForRetransfer.remove(aMessage.getInitMsgId());	// ɾ�������������ش���������Ϣ
	}
	
	/**
	 * �յ����ư�2֮�󣬼��ChunkBuffer����û��Chunk���ڣ���Կ���������д���
	 * @param aMessage
	 */
	public void NoChunkInChunkBuffer(Message aMessage){
		/**	֤��һ���ļ���û�У������ڶԿ��ư���ȷ�ϰ���ʧ��ɵ�*/
		if(this.judgeForRetransfer.containsKey(Ack2Ctrl_Msg + aMessage.getInitMsgId())){
			Message m = (Message) this.judgeForRetransfer.get(
					Ack2Ctrl_Msg + aMessage.getInitMsgId()).get(0);
			
			Message ackMessage = new Message(m.getFrom(), m.getTo(),
					Ack2Ctrl_Msg + m.getId(), m.getResponseSize());	
			ackMessage.setInitMsgId(m.getInitMsgId());
			ackMessage.updateProperty(SelectLabel, 3);	//��ʶΪ���ư�
			ackMessage.setFilename(m.getFilename());
			ackMessage.setZeroForBitMap();
			ackMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//������Ϣ����ʱ��					
			this.judgeForRetransfer.get(Ack2Ctrl_Msg + m.getInitMsgId()).set(1, this.time_wait);	//ˢ���ش�ʱ��
            this.createNewMessage(ackMessage);
		}else{
		/** ֤����������ѡ·��ɵ�*/
			Message ack2CtrlMessage = new Message(this.getHost(),
					aMessage.getHops().get(aMessage.getHopCount() - 1),
					Ack2Ctrl_Msg + aMessage.getInitMsgId(),
					aMessage.getResponseSize());
			ack2CtrlMessage.setInitMsgId(aMessage.getInitMsgId());
			ack2CtrlMessage.updateProperty(SelectLabel, 3);															
			ack2CtrlMessage.setFilename(aMessage.getFilename());
			ack2CtrlMessage.setZeroForBitMap();
			ack2CtrlMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//������Ϣ����ʱ��
			this.putJudgeForRetransfer(ack2CtrlMessage);	//�����ش�����
            this.createNewMessage(ack2CtrlMessage);
            this.router.removeFromMessages(aMessage.getId());
		}
	}
	
	/**
	 * ���ڵ��ļ������з����ļ�
	 */
	public void PutIntoFileBuffer(Message aMessage){
		File NewFile = 	this.getHost().getChunkBuffer()
							.get(aMessage.getFilename())
							.get(aMessage.getFilename() + "ChunkID" + 0);
		for (int i = 1; i < 10; i++) {
			File temp = this.getHost().getChunkBuffer()
							.get(aMessage.getFilename())
							.get(aMessage.getFilename() + "ChunkID" + i);
			ArrayList<Integer> c = temp.getData();
			NewFile.getData().addAll(i * 10, c);
		}
		this.getHost().getChunkBuffer().remove(aMessage.getFilename());				
		NewFile.setInitFile(NewFile);  		
		NewFile.setTimeRequest(SimClock.getTime());

		this.getHost().getFileBuffer().put(aMessage.getFilename(), NewFile);
	}
	
	/**
	 * create response message identified as 1
	 * @param aMessage
	 */
	public void createResponseMessage(Message aMessage){
    	/** ��Ҫ������Ӷ��ļ���Ƭ�Ĵ���Ȼ���ٽ���Ӧ��������Ϣ���������������*/
		this.getHost().getFileBufferForFile(aMessage).setTimeRequest(SimClock.getTime());  	//�������ļ���������ʱ��					
		File f = this.getHost().getFileBufferForFile(aMessage);
		for(int i=0;i<10;i++){						// each file is divided into 10 chunks
			File chunk = f.copyFrom(f);
			for(int j=i*10;j<i*10+10;j++){
				chunk.getData().add(j-i*10,f.getData().get(j));
			}
	
			Message res = new Message(this.getHost(), aMessage.getFrom(),
					Response_Msg + aMessage.getInitMsgId()+i, aMessage.getResponseSize(),chunk);	
			
			res.setInitMsgId(aMessage.getInitMsgId());
			res.setResponseSize(0);					//��һ��Ӧ��û��
			res.setFilename(aMessage.getFilename());
			res.setChunkID(aMessage.getFilename()+"ChunkID"+i);	//����chunkID����message�����á�
			res.updateProperty(SelectLabel,1);	    			//˵������һ��Ӧ���						
			res.setTime(SimClock.getTime()+0.01*(i+1), SimClock.getTime()+0.01*(i+1));
			this.createNewMessage(res);
		}
	}
	
	/**
	 * create Control message identified as 2
	 * @param aMessage
	 */
	public void createControlMessage(Message aMessage){
		Message ctrMessage =new Message(this.getHost(),aMessage.getFrom(),
				Control_Msg + aMessage.getInitMsgId(), aMessage.getResponseSize());
		ctrMessage.setInitMsgId(aMessage.getInitMsgId());
		ctrMessage.updateProperty(SelectLabel, 2);		//��ʶΪ���ư�
		ctrMessage.setFilename(aMessage.getFilename());
		ctrMessage.setZeroForBitMap();
		ctrMessage.setTime(SimClock.getTime()+11*(0.01), SimClock.getTime()+11*(0.01));	              
		this.createNewMessage(ctrMessage);  
		this.putJudgeForRetransfer(ctrMessage);
	}
	
	/**
	 * �յ����ư�2֮�����ɶ�Ӧ�Ŀ��ư���ȷ�ϰ�3
	 * @param aMessage
	 */
	public void createAck2CtrlMessage(Message aMessage){
		Message ackMessage =new Message(this.getHost(),aMessage.getFrom(),
				Ack2Ctrl_Msg + aMessage.getInitMsgId(), aMessage.getResponseSize());
		ackMessage.setInitMsgId(aMessage.getInitMsgId());
		ackMessage.updateProperty(SelectLabel,3);		//˵������һ��ȷ�ϰ�
		ackMessage.getBitMap().clear();             	//�����bitmap
		ackMessage.getBitMap().addAll(this.bitMap);		//�ظ�bitMap
		ackMessage.setFilename(aMessage.getFilename());	
		this.putJudgeForRetransfer(ackMessage);
		System.out.println("Ŀ�Ľڵ�bitmapȷ�ϣ�"+"  "+ackMessage.getBitMap()+" "+"��Ϣ�ĳ�ʼIDΪ��"
							+ackMessage.getInitMsgId()+"  "+"��ǰ�ڵ�Ϊ��"+this.getHost());
		this.createNewMessage(ackMessage);
	}
	
	/**
	 * create acknowledge message identified as 4
	 * @param aMessage
	 */
	public void createAcknowledgeMessage(Message aMessage){
		// ȷ�ϰ�4������һ������ȷ��
		Message ackMessage = new Message(this.getHost(), aMessage.getHops()
				.get(aMessage.getHopCount() - 1), Ack2Request_Msg
				+ aMessage.getInitMsgId(), aMessage.getResponseSize());

		ackMessage.setInitMsgId(aMessage.getInitMsgId());
		ackMessage.updateProperty(SelectLabel, 4);			//��ʶΪ�������ȷ�ϰ�
		ackMessage.setFilename(aMessage.getFilename());
		ackMessage.setTime(SimClock.getTime()+0, SimClock.getTime()+0);		
		this.putJudgeForRetransfer(ackMessage);
		this.createNewMessage(ackMessage); 
		
	}
	
	/**
	 * ��Ϣ��δ����Ŀ�Ľڵ㣬����Ϣ���������жϣ�������Ӧ����
	 * @param aMessage Ϊ��Ӧ�������Ϣ
	 */	
	public void NotDestinationCache(Message aMessage) {
		if (aMessage.getProperty(SelectLabel) == (Object) 0) { // ����һ�������
			RequestMessage_NotDestination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 1) { // ����һ��Ӧ�����Я��chunk�ļ�
			ResponseMessage_NotDestination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 2) { // ����һ�����ư�
			ControlMessage_NotDestination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 3) { // ���ǶԿ��ư���ȷ�ϰ�
			Ack2CtrlMessage_NotDestination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 4) { // ���Ƕ��������ȷ�ϰ�
			Ack2RequestMessage_NotDestination(aMessage);
		}
	}
	
	/**
	 * �������Ŀ�Ľڵ�������0
	 * @param aMessage
	 */
	private void RequestMessage_NotDestination(Message aMessage) { 
		if(this.judgeForRetransfer.containsKey(Ack2Request_Msg + aMessage.getInitMsgId())){
			Message m = (Message) this.judgeForRetransfer.get(
					Ack2Request_Msg + aMessage.getInitMsgId()).get(0);
			Message ackMessage = new Message(m.getFrom(), m.getTo(),
					Ack2Request_Msg + m.getId(), m.getResponseSize());
			ackMessage.setInitMsgId(m.getInitMsgId());
			ackMessage.updateProperty(SelectLabel, 4);		//��ʶΪ���ư�
			ackMessage.setFilename(m.getFilename());
			ackMessage.setZeroForBitMap();
			ackMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//������Ϣ����ʱ��				
			this.judgeForRetransfer.get(Ack2Request_Msg + m.getInitMsgId()).set(1, this.time_wait);	//ˢ���ش�ʱ��
            this.createNewMessage(ackMessage);

		} else{
			createAcknowledgeMessage(aMessage);				//���������ȷ��
			this.router.getMessage(aMessage.getId()).setTime(
					SimClock.getTime() + 0.01, SimClock.getTime() + 0.01); // �Դ���ʱ�������ʱ����������趨
			
			if (this.getHost().getFileBufferForFile(aMessage)!=null) {
				//�������ֱ�ӽ���Ӧ��
				this.createResponseMessage(aMessage);
				//Ӧ��֮�󷢿��ư�
	            this.createControlMessage(aMessage);
	            this.router.removeFromMessages(aMessage.getId());	
			} else{
//				System.out.println("������������������������������������������������������������������������������������ֱ������һ��ת��������");
			}
		}
	}
	
	/**
	 * �������Ŀ�Ľڵ��Ӧ���1
	 * @param aMessage
	 */
	private void ResponseMessage_NotDestination(Message aMessage) {
		/**
		 * ΪӦ������ϼ�ʱ�� Time_free
		 * �����ж�Ӧ����ļ�ʱ���ڴ�ȷ����Ϣ���Ƿ���ڣ� ���ڵĻ����£��������ڣ�������һ��
		 */
		if(this.judgeForRetransfer.containsKey("Chunk"+aMessage.getInitMsgId())){		
			this.judgeForRetransfer.get("Chunk"+aMessage.getInitMsgId()).set(1, this.time_free);
		} else{
			this.putJudgeForRetransfer(aMessage);
		}
		
		if (this.getHost().getFileBufferForFile(aMessage)==null){		
			/**
			 * ����ӵ�������֮ǰ����Ҫ�ȶԻ������жϣ��Ƿ����� ��δ����ֱ�Ӽ��뻺�棻���������ȶԻ��������ݽ���ɾ�����ټ��뻺��
			 * ���ö��ж���Ϣ���д洢��ͳһ��һ����ά��HashMap���д洢��
			 */
			addToChunkBuffer(aMessage);				
			if(MessageHashMap.containsKey(aMessage.getFilename())){
				this.MessageHashMap.get(aMessage.getFilename()).put(aMessage.getChunkID(), aMessage);
			}	else{
				HashMap<String,Message> NewHashMap = new HashMap<String,Message>();
				NewHashMap.put(aMessage.getChunkID(), aMessage);
				this.MessageHashMap.put(aMessage.getFilename(), NewHashMap);
			}
			this.router.removeFromMessages(aMessage.getId());	
		}else{	
			/** 
			 * �м����Ѿ��������ļ���ԭ������Ӧ��ʱ·��������ʱ��·����һ�� 
			 * Ϊ�˱�֤����ȷ�ϣ���Ҫ�ڴ˽ڵ㱣����Щ�ļ���Ȼ���ɴ��м̽ڵ�����һ��Ӧ��
			 */
			addToChunkBuffer(aMessage);
			if(MessageHashMap.containsKey(aMessage.getFilename())){
				this.MessageHashMap.get(aMessage.getFilename()).put(aMessage.getChunkID(), aMessage);
			}	else{
				HashMap<String,Message> NewHashMap = new HashMap<String,Message>();
				NewHashMap.put(aMessage.getChunkID(), aMessage);
				this.MessageHashMap.put(aMessage.getFilename(), NewHashMap);
			}
			this.router.removeFromMessages(aMessage.getId());
		}
	}
	
	/**
	 * �������Ŀ�Ľڵ�Ŀ��ư�2 
	 * @param aMessage
	 */
	private void ControlMessage_NotDestination(Message aMessage) {
		if(this.getHost().getFileBuffer().containsKey(aMessage.getFilename())==false){
			/** ��Կ��ư��ش��������1�����ڿ��ư���ʧ��2�����ڿ��ư�����ѡ·   */
			boolean a = false;
			for(int i=0;i<10;i++){					
				if(this.getHost().getChunkBuffer().containsKey(aMessage.getFilename())){
					if (this.getHost()
							.getChunkBuffer()
							.get(aMessage.getFilename())
							.containsKey(aMessage.getFilename()+"ChunkID"+i))
						a = true;
				}
				if (a == true) break;
			}
			
			if(	a==false ){						
				/**֤��һ���ļ���û��,����һ�������ļ���������ϢaMessage����*/
				RetransCtrlMsg_notDestination(aMessage);
			} else {
				if(MessageHashMap.containsKey(aMessage.getFilename())){	// ��һ��Ϊ����������
					this.MessageHashMap.get(aMessage.getFilename()).put(aMessage.getId(), aMessage);
				} else{
					HashMap<String,Message> NewHashMap = new HashMap<String,Message>();
					NewHashMap.put(aMessage.getChunkID(), aMessage);
					this.MessageHashMap.put(aMessage.getId(), NewHashMap);
				}
	        	this.router.removeFromMessages(aMessage.getId());
				
	        	/** ����chunkBuffer��chunk�Ƿ����룬b=1 Ĭ��Ϊ����*/
	        	boolean b = true;						
	        	this.setZeroForBitMap();
	        	for(int i=0;i<10;i++){
					if(this.getHost().getChunkBuffer().get(aMessage.getFilename()).containsKey(aMessage.getFilename()+"ChunkID"+i))
	        			this.bitMap.set(i, 1);
	        		else 
	        			b = false;
	        	}
	        	if(b == true){
	            	/** �������֮ǰ����Ҫ���ж��ڴ��Ƿ����������Ļ���Ҫɾ���ڴ�*/
	        		if (this.getHost().getFreeFileBufferSize() < 0) {
	        			this.getHost().makeRoomForNewFile(0);    	//��Ҫʱ��ɾ����Щ������յ��Ҳ����ڴ������Ϣ
	        			System.out.print("+++++++++++++++++++++		ɾ���ɹ�	++++++++++++++++++++"+"\n");
	        		}	
	        		this.PutIntoFileBuffer(aMessage);
	        		System.out.println("-------------------------"+" �м̽ڵ� "+this.getHost()+
	        						   " �����ļ���"+aMessage.getFilename()+"--------------------");
		        	
					/**
					 * �յ����ư�֮����Ҫ�������£�һ���ظ���һ����һ������Ŀ�Ľڵ㷢
					 * 1���ж����������£���MessageHashMap  ����Ϣ˳��ȡ��������һ����
					 */	 
			        this.createResponseMsg_notDestination(aMessage);
		        	this.createControlMsg_notDestination(aMessage);
		    		this.MessageHashMap.remove(aMessage.getFilename());	
	        	} 
	        	/**
	        	 * 2���ظ�ȷ�ϰ�(�������û�����붼������һ���ظ�ȷ�ϰ�)			
	        	 */
	        	this.createAck2CtrlMsg_notDestination(aMessage);
			}
		}else{
			/** 
			 * ������յ�Ӧ������ڴ��д��ڴ��ļ�����Ҫ����ȷ�ϣ� ��Ҫ�������£�
			 * 1������һ�����ͶԿ��ư���ȷ�ϰ���
			 * 2������һ������������ȡ���ļ�����Ӧ�𣬶����ع��Ƿ����룻
			 * 3�� ɾ�����յ����ļ���Ƭ�Լ����ư�
			 */	
//			System.out.println("******************************�м̽ڵ����Ѿ��������ļ���"+aMessage.getFilename());

			/**  ����һ������������ȡ���ļ�����Ӧ�𣬶����ع��Ƿ�����*/
			CreateResponseMsg(aMessage);
			/**  ����һ�����Ϳ��ư� */
			CreateControlMsg(aMessage);
			/**  ����һ�����ͶԿ��ư���ȷ�ϰ� */
			CreateAck2CtrlMsg(aMessage);
			/**  ɾ�����յ����ļ���Ƭ�Լ����ư� */
			DeleteRcvMsg(aMessage);
		}
	}
	/**
	 * ��Կ��ư��ش��������1�����ڿ��ư���ʧ��2�����ڿ��ư�����ѡ·
	 * @param aMessage
	 */
	public void RetransCtrlMsg_notDestination(Message aMessage){
		/**	֤��һ���ļ���û��,����һ�������ļ���������ϢaMessage����*/
		if(this.judgeForRetransfer.containsKey(Ack2Ctrl_Msg + aMessage.getInitMsgId())){			
			Message m = (Message) this.judgeForRetransfer.get(
					Ack2Ctrl_Msg + aMessage.getInitMsgId()).get(0);
			Message ackMessage = new Message(m.getFrom(), m.getTo(),
					Ack2Ctrl_Msg + m.getId(), m.getResponseSize());
			
			ackMessage.setInitMsgId(m.getInitMsgId());
			ackMessage.updateProperty(SelectLabel, 3);															
			ackMessage.setFilename(m.getFilename());
			ackMessage.setZeroForBitMap();
			ackMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//������Ϣ����ʱ��
			this.judgeForRetransfer.get(Ack2Ctrl_Msg + m.getInitMsgId()).set(1, this.time_wait);
            this.createNewMessage(ackMessage);
		}else{
			/** ֤����������ѡ·��ɵ�*/
			Message ack2CtrlMessage = new Message(this.getHost(),aMessage.getHops().get(aMessage.getHopCount()-1),
					Ack2Ctrl_Msg + aMessage.getInitMsgId(),aMessage.getResponseSize());
			ack2CtrlMessage.setInitMsgId(aMessage.getInitMsgId());
			ack2CtrlMessage.updateProperty(SelectLabel, 3);															
			ack2CtrlMessage.setFilename(aMessage.getFilename());
			ack2CtrlMessage.setZeroForBitMap();
			ack2CtrlMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//������Ϣ����ʱ��
			this.putJudgeForRetransfer(ack2CtrlMessage);	//�����ش�����
            this.createNewMessage(ack2CtrlMessage);
            this.router.removeFromMessages(aMessage.getId());
		}
	}
	
	/**  
	 * �м̽ڵ�����ļ������
	 * ɾ�����յ����ļ���Ƭ�Լ����ư� 
	 */
	public void DeleteRcvMsg(Message aMessage){
		this.getHost().getChunkBuffer().remove(aMessage.getFilename());	//���ChunkBuffer	
		this.MessageHashMap.remove(aMessage.getFilename());		//���MessageHashMap����Ϣ
		this.router.removeFromMessages(aMessage.getId());		//��Ҫɾ��������ư�
	}
	/**
	 * ����м̽ڵ��д����ļ����������Ӧ���
	 */
	public void CreateResponseMsg(Message aMessage){
		this.getHost().getFileBufferForFile(aMessage).setTimeRequest(SimClock.getTime());  	//�������ļ���������ʱ��
		File f = this.getHost().getFileBufferForFile(aMessage);
		for(int i=0;i<10;i++){							// each file is divided into 10 chunks
			File chunk = f.copyFrom(f);
			for(int j=i*10;j<i*10+10;j++){
				chunk.getData().add(j-i*10,f.getData().get(j));
			}
			Message response = new Message(this.getHost(), aMessage.getTo(),Response_Msg
									  +aMessage.getInitMsgId()+i, aMessage.getResponseSize(),chunk);	
			response.setInitMsgId(aMessage.getInitMsgId());
			response.setResponseSize(0);						//��һ��Ӧ��û��
			response.setFilename(aMessage.getFilename());
			response.setChunkID(aMessage.getFilename()+"ChunkID"+i);	//����chunkID����message�����á�
			response.updateProperty(SelectLabel,1);		//˵������һ��Ӧ���						
			response.setTime(SimClock.getTime()+0.01*(i+1), SimClock.getTime()+0.01*(i+1));
			this.createNewMessage(response);
		}
		System.out.println("�м̽ڵ�����ļ����յ����ư�ʱ�����ϢIDΪ��"+aMessage.getId()+"  "+"test!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	}
	
	/**
	 * ����м̽ڵ��д����ļ�ʱ�������ư�
	 */
	public void CreateControlMsg(Message aMessage){
		
		Message ctrMessage =new Message(this.getHost(),aMessage.getTo(),
				aMessage.getId(), aMessage.getResponseSize());
		ctrMessage.setInitMsgId(aMessage.getInitMsgId());
		ctrMessage.updateProperty(SelectLabel, 2);												//��ʶΪ���ư�
		ctrMessage.setFilename(aMessage.getFilename());
		ctrMessage.setZeroForBitMap();
		ctrMessage.setTime(SimClock.getTime()+11*(0.01), SimClock.getTime()+11*(0.01));	              
		this.createNewMessage(ctrMessage);  
		this.putJudgeForRetransfer(ctrMessage);
	}
	
	/**
	 * ����м̽ڵ��д����ļ�ʱ�������ư���ȷ�ϰ�
	 */
	public void CreateAck2CtrlMsg(Message aMessage){
    	Message ackMessage = new Message(this.getHost(),aMessage.getHops().get(aMessage.getHopCount()-1),
    			Ack2Ctrl_Msg + aMessage.getInitMsgId(), aMessage.getResponseSize());
		ackMessage.setInitMsgId(aMessage.getInitMsgId());
    	ackMessage.updateProperty(SelectLabel,3);	//˵������һ��ȷ�ϰ�
		ackMessage.getBitMap().clear();             //�����bitmap
		/** Ϊ�����֣������bitmap��һ���ֲ�����*/
		ArrayList<Integer> BitMap = new ArrayList<Integer>();
		for (int i = 0; i<10; i++) {
			BitMap.add(i, 1);
		}
		ackMessage.getBitMap().addAll(BitMap);				//�ظ�bitMap    	
    	ackMessage.setFilename(aMessage.getFilename());
		this.putJudgeForRetransfer(ackMessage);
    	this.createNewMessage(ackMessage);
		System.out.println("�м���bitmap���в��ԣ�" + "  " + ackMessage.getBitMap()
						    + " " + "��Ϣ�ĳ�ʼIDΪ��" + ackMessage.getInitMsgId() 
						    + " " + "��ǰ�ڵ�Ϊ��" + this.getHost());
	}
	
	
	/**
	 * ��Ŀ�Ľڵ�����Ӧ���1
	 * �յ����ư�֮����Ҫ�������£�һ���ظ���һ����һ������Ŀ�Ľڵ㷢
	 * �ж����������£��� MessageHashMap ����Ϣ˳��ȡ��������һ������Ӧ���
	 * @param aMessage
	 */
	public void createResponseMsg_notDestination (Message aMessage){
		try{
			HashMap<String,Message> NewHashMap = MessageHashMap.get(aMessage.getFilename());
	    	for(int i=0;i<10;i++){
	    		Message m = NewHashMap.get(aMessage.getFilename()+"ChunkID"+i);    		
	    		// ��Ҫ�Ǹı�Դ��ַ 
	    		DTNHost thisHost = this.getHost();	//	Դ��ַ 
	    		DTNHost thisto = m.getTo();			//	��ǰ��Ϣ��Ŀ�Ľڵ�
	    		Message newMessage = new Message(thisHost,thisto,m.getId(),m.getSize());
	    		newMessage.copyFrom(m);				//  copy��ǰ��Ϣ������
				newMessage.setFilename(m.getFilename());	        			
				newMessage.setBitMap(m.getBitMap());
				newMessage.setInitMsgId(m.getInitMsgId());
		        newMessage.setFile(m.getFile());
		        newMessage.setTime(SimClock.getTime()+0.01*(i+1), SimClock.getTime()+0.01*(i+1));
	    		this.createNewMessage(newMessage);
	    	}
    	} catch(NullPointerException e){
    		System.out.println("MessageHashMap����һ��Ϊ�գ�����");
    	}
	}
	
	/**
	 * ��Ŀ�Ľڵ����ɿ��ư�2
	 * �յ����ư�֮����Ҫ�������£�һ���ظ���һ����һ������Ŀ�Ľڵ㷢
	 * �ж����������£��� MessageHashMap ����Ϣ˳��ȡ��������һ�����Ŀ��ư�
	 * @param aMessage
	 */
	public void createControlMsg_notDestination(Message aMessage){
		try{
			HashMap<String,Message> NewHashMap = MessageHashMap.get(aMessage.getFilename());
	    	Message m = NewHashMap.remove(aMessage.getId());// ����ǿ��ư�		            		
	    	DTNHost thisHost = this.getHost();				// Դ��ַ 
			DTNHost thisto = m.getTo();						// ��ǰ��Ϣ��Ŀ�Ľڵ�
			Message newMessage = new Message(thisHost,thisto,m.getId(),m.getSize());
			newMessage.copyFrom(m);							// copy��ǰ��Ϣ������
			newMessage.setFilename(m.getFilename());	        			
			newMessage.setBitMap(m.getBitMap());
			newMessage.setInitMsgId(m.getInitMsgId());
			newMessage.setTime(SimClock.getTime()+11*(0.01), SimClock.getTime()+11*(0.01));
		    this.createNewMessage(newMessage);
			this.putJudgeForRetransfer(newMessage);			// �ɵ�ǰ�ڵ㷢���Ŀ��ư������뵱ǰ�ڵ�Ĵ�ȷ�ϻ�����
		} catch(NullPointerException e){
    		System.out.println("MessageHashMap����һ��Ϊ�գ�����");
    	}

	}
	
	/**
	 * ��Ŀ�Ľڵ����ɶԿ��ư���ȷ�ϰ�3
	 * @param aMessage
	 */
	public void createAck2CtrlMsg_notDestination(Message aMessage){
    	Message ackMessage = new Message(this.getHost(),aMessage.getHops().get(aMessage.getHopCount()-1),
    			Ack2Ctrl_Msg + aMessage.getInitMsgId(), aMessage.getResponseSize());
		ackMessage.setInitMsgId(aMessage.getInitMsgId());
    	ackMessage.updateProperty(SelectLabel,3);		//˵������һ��ȷ�ϰ�
		ackMessage.getBitMap().clear();             	//�����bitmap
    	ackMessage.getBitMap().addAll(this.bitMap);		//�ظ�bitMap    	
    	ackMessage.setFilename(aMessage.getFilename());
		this.putJudgeForRetransfer(ackMessage);
    	this.createNewMessage(ackMessage);
    	
    	System.out.println("�м���bitmap���в��ԣ�"+"  "+ ackMessage.getBitMap()+" "+
				   "��Ϣ�ĳ�ʼIDΪ��"+ackMessage.getInitMsgId()+"  "+"��ǰ�ڵ�Ϊ��"+this.getHost());
	}
	
	/**
	 * �������Ŀ�Ľڵ�Ŀ��ư���ȷ�ϰ�3 
	 * @param aMessage
	 */
	private void Ack2CtrlMessage_NotDestination(Message aMessage) {
		// TODO Auto-generated method stub
	}
	
	/**
	 * �������Ŀ�Ľڵ���������ȷ�ϰ�4
	 * @param aMessage
	 */
	private void Ack2RequestMessage_NotDestination(Message aMessage) {
		// TODO Auto-generated method stub
	}


}
