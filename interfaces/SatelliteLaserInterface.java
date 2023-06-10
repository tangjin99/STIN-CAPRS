package interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import movement.SatelliteMovement;
import core.CBRConnection;
import core.Connection;
import core.DTNHost;
import core.Neighbors;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;

/**
 * A simple Network Interface that provides a constant bit-rate service, where
 * one transmission can be on at a time.
 */
public class SatelliteLaserInterface  extends NetworkInterface {

	/** router mode in the sim -setting id ({@value})*/
	public static final String USERSETTINGNAME_S = "userSetting";
	/** router mode in the sim -setting id ({@value})*/
	public static final String ROUTERMODENAME_S = "routerMode";
	public static final String DIJSKTRA_S = "dijsktra";
	public static final String SIMPLECONNECTIVITY_S = "simpleConnectivity";
	
	private Collection<NetworkInterface> interfaces;
	
	/** indicates the interface type, i.e., radio or laser*/
	public static final String interfaceType = "LaserInterface";
	/** dynamic clustering by MEO or static clustering by MEO */
	private static boolean dynamicClustering;
	/** allConnected or clustering */
	private static String mode;
	
	/**
	 * Reads the interface settings from the Settings file
	 */
	public SatelliteLaserInterface(Settings s)	{
		super(s);
		Settings s1 = new Settings("Interface");
		dynamicClustering = s1.getBoolean("DynamicClustering");
		Settings s2 = new Settings(USERSETTINGNAME_S);
		mode = s2.getSetting(ROUTERMODENAME_S);
	}
		
	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
	public SatelliteLaserInterface(SatelliteLaserInterface ni) {
		super(ni);
	}

	public NetworkInterface replicate()	{
		return new SatelliteLaserInterface(this);
	}

	/**
	 * Tries to connect this host to another host. The other host must be
	 * active and within range of this host for the connection to succeed. 
	 * @param anotherInterface The interface to connect to
	 */
	public void connect(NetworkInterface anotherInterface) {
		
		if (isScanning()  
				&& anotherInterface.getHost().isRadioActive() 
				&& isWithinRange(anotherInterface) 
				&& !isConnected(anotherInterface)
				&& (this != anotherInterface)
				&& (this.interfaceType == anotherInterface.getInterfaceType())) {
			// new contact within range
			// connection speed is the lower one of the two speeds 
			int conSpeed = anotherInterface.getTransmitSpeed();//�������˵����������ɽ�С��һ������		
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}			
			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
			connect(con,anotherInterface);//���������˫����host�ڵ㣬����������ɵ�����con���������б���
		}
	}

	/**
	 * Updates the state of current connections (i.e. tears down connections
	 * that are out of range and creates new ones).
	 */
	public void update() {
		//update the satellite link info
		List<DTNHost> allowConnectedList = 
				((SatelliteMovement)this.getHost().getMovementModel()).updateSatelliteLinkInfo();
		
		if (!this.getHost().multiThread){
			if (optimizer == null) {
				return; /* nothing to do */
			}
			
			// First break the old ones
			optimizer.updateLocation(this);
		}
		
		for (int i=0; i<this.connections.size(); ) {
			Connection con = this.connections.get(i);
			NetworkInterface anotherInterface = con.getOtherInterface(this);

			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";

			if (!isWithinRange(anotherInterface)) {//���½ڵ�λ�ú󣬼��֮ǰά���������Ƿ����Ϊ̫Զ���ϵ�
				disconnect(con,anotherInterface);
				connections.remove(i);
				
				//neighbors.removeNeighbor(con.getOtherNode(this.getHost()));//�ڶϵ����ӵ�ͬʱ�Ƴ����ھ��б�����ھӽڵ㣬����������
			}
			else {
				i++;
			}
		}

		// Then find new possible connections
		switch (mode) { 
		case "AllConnected":{
			if (!this.getHost().multiThread) {
				// Then find new possible connections
				interfaces = optimizer.getNearInterfaces(this);
			}
			for (NetworkInterface i : interfaces) {
				connect(i);
			}
			break;
		}
		case "Cluster":{
			if (!this.getHost().multiThread) {
				// Then find new possible connections
				interfaces = optimizer.getNearInterfaces(this);
			}				

			for (NetworkInterface i : interfaces) {	
				/*����Ƿ������������б��У�������������·*/
				boolean allowConnection = false;
				switch(this.getHost().getSatelliteType()){
				/*����ڷ�Χ�ڵ�����ڵ�Ȳ���ͬһƽ���ڵģ��ֲ���ͨѶ�ڵ㣬�Ͳ��������ӣ���ʡ����**/
					case "LEO":{						
						//ֻ��LEOͨ�Žڵ�������MEO�㽨����·
						if (allowConnectedList.contains(i.getHost()))
							allowConnection = true;//����������
						break;
					}
					case "MEO":{
						if (allowConnectedList.contains(i.getHost()))
							allowConnection = true;//����������
						break;
					}
					case "GEO":{
						if (i.getHost().getSatelliteType().contains("MEO")){
							allowConnection = true;
							break;
						}
						if (allowConnectedList.contains(i.getHost()))
							allowConnection = true;//����������
						break;
					}
				}
				
				if (allowConnection){//������λ���Ž�������
					connect(i);
				}
			}
			break;
		}
		}

	}

	
	/** 
	 * Creates a connection to another host. This method does not do any checks
	 * on whether the other node is in range or active 
	 * @param anotherInterface The interface to create the connection to
	 */
	public void createConnection(NetworkInterface anotherInterface) {
		if (!isConnected(anotherInterface) && (this != anotherInterface)) {		   			
			// connection speed is the lower one of the two speeds 
			int conSpeed = anotherInterface.getTransmitSpeed();
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}

			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
			connect(con,anotherInterface);
		}
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "SatelliteLaserInterface " + super.toString();
	}
	
	/** return the type of this interface */
	@Override
	public String getInterfaceType(){
		return this.interfaceType;
	}

}
