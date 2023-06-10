/*
 * The design of the interface  of simulator by	YongqGui
 */
package Develop;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import core.Settings;


public class RouterInfo extends JFrame implements ActionListener, ChangeListener{
	/** Default width for the GUI window */
	public static final int WIN_DEFAULT_WIDTH = 700;
	/** Default height for the GUI window */
	public static final int WIN_DEFAULT_HEIGHT = 500;
	public JButton Confirm;
	public JButton Reset;
	public JButton Concel;
	public JComboBox RouterC;
	public JTextField simTime;
	public JTextField interval;
	public JTextField warmUp;
	/**Ĭ����LEO���ǹ������**/
	public JTextField totalNodes;
	public JTextField totalPlane;
	public JTextField phaseFactor;
	/**���ƽ�����**/
	public JTextField orbitPlaneAngle;
	/**����뾶**/
	public JTextField radius;
	/**���������**/
	public JTextField eccentricity;
	/**MEO�����ǹ������**/
	public JTextField MEOtotalNodes;
	public JTextField MEOtotalPlane;
	public JTextField MEOphaseFactor;
	/**���ƽ�����**/
	public JTextField MEOorbitPlaneAngle;
	/**����뾶**/
	public JTextField MEOradius;
	/**���������**/
	public JTextField MEOeccentricity;
	
	public JTextField bufferSize;
	public JTextField transmissionRadius;
	public JTextField transmissionRate;
	public JTextField messageSize;
	public JTextField messageTTL;
	public JTextField worldSizeX;
	public JTextField worldSizeY;
	public JTextField worldSizeZ;
	
	public JComboBox satelliteType;
	public JComboBox gridLayer;
	public JRadioButton queueMode1;//���Ͷ���ģʽѡ��Random or FIFO
	public JRadioButton queueMode2;
	public JRadioButton routerMode1;//·��ģʽѡ�� ����ȷ�� or ָ��·��
	public JRadioButton routerMode2;
	public JRadioButton gridCalculationMode1;// �������ģʽѡ�� Ԥ�ȼ���洢 or ʵʱ����
	public JRadioButton gridCalculationMode2;
	
//	public JRadioButton HopByHop_Confirm;					// ����ȷ��
//	public JRadioButton fast_Forward;						// ����ת��
	public JTextField nrofFile;

	private JCheckBox HopByHop_Confirm;
	private JCheckBox RandomDropMessage;
	
	
	public RouterInfo(){
		super("��������");										//   copyright by USTC");
		setSize(WIN_DEFAULT_WIDTH,WIN_DEFAULT_HEIGHT);
		
		//---------------------------����tabҳ��----------------------------//		
		final JTabbedPane tabs = new JTabbedPane();
		this.setLayout(null);
		JPanel jp1 = RouteInfoSetting();	
		JPanel jp2 = CacheSetting();
		JPanel jp3 = MovementSetting();		
		JPanel jp4 = LinkSetting();
		JPanel jp5 = MainSetting();

		tabs.add("·��ģ��",jp1);		
		tabs.add("����ģ��",jp2);
		tabs.add("�˶�ģ��",jp3);		
		tabs.add("��·ģ��",jp4);
		tabs.add("�������",jp5);
		
		//---------------------------���ü�����ť----------------------------//	
		JPanel ButtonMenu = new JPanel();
		ButtonMenu.setLayout(new BoxLayout(ButtonMenu, BoxLayout.X_AXIS));
	    Reset = new JButton("Ĭ��");									// Ϊ��ť���������
	    Reset.addActionListener(this);
	    Concel = new JButton("ȡ��");
	    Concel.addActionListener(this);
	    Confirm = new JButton("ȷ��");
	    Confirm.addActionListener(this);
	    
		ButtonMenu.add(Box.createHorizontalStrut(250));
		ButtonMenu.add(Reset);
		ButtonMenu.add(Box.createHorizontalStrut(30));
		ButtonMenu.add(Concel);
		ButtonMenu.add(Box.createHorizontalStrut(30));
		ButtonMenu.add(Confirm);
		
		tabs.setBounds(0, 0, 666, 380);
		ButtonMenu.setBounds(150, 405, 500, 30);
		add(tabs);
		add(ButtonMenu);
		this.setLocationRelativeTo(null);
		this.setResizable(false);							//	���ý��治�����
		setVisible(true);
	}
	
	public JPanel RouteInfoSetting(){
		JPanel jp1 = new JPanel();
		//JPanel jp = new JPanel();
		//---------------------------����·�ɲ���ҳ��----------------------------//	
		JPanel RouteFirst = new JPanel();
		RouteFirst.setBorder(new TitledBorder("ͨ������"));
		//RouteFirst.setLayout(new GridLayout(8,1,10,10));
		RouteFirst.setLayout(null);
		//��һ��
	    JLabel label1=new JLabel("·��Э��ѡ��",JLabel.LEFT);
		RouterC = new JComboBox();
		String[] description = {
//				"ShortestPathFirstRouter","DirectDeliveryRouter", "GridRouter", "ClusteringRouter",
//				"EASRRouter", "FirstContactRouter", "EpidemicRouter",
				"ShortestPathFirstRouter", "ClusteringRouter", "NetGridRouter",  "EpidemicRouter",
		};
		
	    for(int i = 0; i < description.length; i++)
	    	RouterC.addItem(description[i]);
		
		label1.setBounds(10, 25, 100, 30);
		RouterC.setBounds(130, 25, 160, 30);
		RouteFirst.add(label1);
		RouteFirst.add(RouterC);

		//		�ڶ��У�·��ģʽѡ��ָ��·����������·�ɣ�
		JLabel rlabel1 = new JLabel("·��ģʽѡ��",JLabel.LEFT);
		JLabel rlabel2 = new JLabel("��������",JLabel.LEFT);
		JLabel rlabel3 = new JLabel("ָ��·��",JLabel.LEFT);
		ButtonGroup g = new ButtonGroup();
		routerMode1 = new JRadioButton("", false);
		routerMode2 = new JRadioButton("", false);
		routerMode1.setSelected(true);
	    g.add(routerMode1);
	    g.add(routerMode2);
	    
		rlabel1.setBounds(10, 65, 100, 30);
		rlabel2.setBounds(130,65, 55, 30);
		routerMode1.setBounds(189,65, 20, 30);
		rlabel3.setBounds(215,65, 55, 30);
		routerMode2.setBounds(272,65,20, 30);
		RouteFirst.add(rlabel1);
		RouteFirst.add(rlabel2);
		RouteFirst.add(rlabel3);
		RouteFirst.add(routerMode1);
		RouteFirst.add(routerMode2);
		//������ 		
		JLabel qlabel1 = new JLabel("���Ͷ���ģʽ��",JLabel.LEFT);
		JLabel qlabel2 = new JLabel("Random",JLabel.LEFT);
		JLabel qlabel3 = new JLabel("FIFO",JLabel.LEFT);
		ButtonGroup queueModel = new ButtonGroup();
		queueMode1 = new JRadioButton("Random", false);
		queueMode2 = new JRadioButton("FIFO", false);
		queueMode2.setSelected(true);
	    
	    queueModel.add(queueMode1);
	    queueModel.add(queueMode2);
	    qlabel1.setBounds(10, 105, 100, 30);
	    qlabel2.setBounds(215,105, 55, 30);
	    queueMode1.setBounds(272,105,20, 30);
	    qlabel3.setBounds(131,105, 55, 30);
	    queueMode2.setBounds(189,105, 20, 30);
	    
		RouteFirst.add(qlabel1);
		RouteFirst.add(qlabel2);
		RouteFirst.add(qlabel3);
		RouteFirst.add(queueMode1);
		RouteFirst.add(queueMode2);
		
		//������
	    JLabel label2=new JLabel("��Ϣ��С��",JLabel.LEFT);
		final JTextField txt1 = new JTextField("1");
		final JTextField txt2 = new JTextField("10");
		JLabel label21 = new JLabel("~",JLabel.CENTER);
		JLabel label22 = new JLabel("M",JLabel.CENTER);
		
		label2.setBounds(10, 145, 100, 30);
		txt1.setBounds(130, 145, 60, 30);
		label21.setBounds(190, 145, 10, 30);
		txt2.setBounds(200, 145, 60, 30);
		label22.setBounds(265, 145, 10, 30);
		RouteFirst.add(label2);
		RouteFirst.add(txt1);
		RouteFirst.add(label21);
		RouteFirst.add(txt2);
		RouteFirst.add(label22);
		//������
	    JLabel label3=new JLabel("��Ϣ����ʱ�䣺",JLabel.LEFT);
		final JTextField txt3 = new JTextField("1");
		final JTextField txt4 = new JTextField("5");
		JLabel label31 = new JLabel("~",JLabel.CENTER);
		JLabel label32 = new JLabel("Min",JLabel.CENTER);
		
		label3.setBounds(10, 185, 100, 30);
		txt3.setBounds(130, 185, 60, 30);
		label31.setBounds(190, 185, 10, 30);
		txt4.setBounds(200, 185, 60, 30);
		label32.setBounds(265, 185, 20, 30);
		RouteFirst.add(label3);
		RouteFirst.add(txt3);
		RouteFirst.add(label31);
		RouteFirst.add(txt4);
		RouteFirst.add(label32);
		
		//������
	    JLabel label4=new JLabel("�ڵ㻺���С��",JLabel.LEFT);
	    JLabel label5=new JLabel("M",JLabel.LEFT);
		bufferSize = new JTextField("1000");
		label4.setBounds(10, 225, 100, 30);
		bufferSize.setBounds(130,225, 130, 30);
		label5.setBounds(265, 225, 20, 30);
		RouteFirst.add(label4);
		RouteFirst.add(bufferSize);
		RouteFirst.add(label5);

	    //������
	    JLabel label61=new JLabel("�ڵ㴫�����ʣ�",JLabel.LEFT);
	    JLabel label62=new JLabel("kbps",JLabel.LEFT);
		transmissionRate = new JTextField("500");
		label61.setBounds(10, 265, 100, 30);
		transmissionRate .setBounds(130,265, 130, 30);
		label62.setBounds(265, 265, 40, 30);
		RouteFirst.add(label61);
		RouteFirst.add(transmissionRate);
		RouteFirst.add(label62);
		
		//�ڰ���
	    JLabel label71=new JLabel("�ڵ㴫��뾶��",JLabel.LEFT);
	    JLabel label72=new JLabel("km",JLabel.LEFT);
		transmissionRadius = new JTextField("4000");
		label71.setBounds(10, 305, 100, 30);
		transmissionRadius.setBounds(130,305, 130, 30);
		label72.setBounds(265, 305, 40, 30);
		RouteFirst.add(label71);
		RouteFirst.add(transmissionRadius);
		RouteFirst.add(label72);
		
		RouteFirst.setSize(320, 200);
		//---------------------------���ö���·�ɲ���ҳ��----------------------------//			
		final JPanel RouteSecond = new JPanel();
		RouteSecond.setBorder(new TitledBorder("�ض���������"));
		RouteSecond.setLayout(null);
		//	���ü�����
		RouterC.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				// ��������Ĭ�ϴ��ڵĲ�������
		
				// ���������Ҫ��ӵĲ���
				if(RouterC.getSelectedIndex()==1 || RouterC.getSelectedIndex()==2){
					RouteSecond.removeAll();
					
					JLabel rlabel1 = new JLabel("����ģʽѡ��",JLabel.LEFT);
					JLabel rlabel2 = new JLabel("ʵʱ�������ģʽ",JLabel.LEFT);
					JLabel rlabel3 = new JLabel("�����Ϣ�洢ģʽ",JLabel.LEFT);
					ButtonGroup g = new ButtonGroup();
					gridCalculationMode1 = new JRadioButton("", false);
					gridCalculationMode2 = new JRadioButton("", false);
					gridCalculationMode1.setSelected(true);
				    g.add(gridCalculationMode1);
				    g.add(gridCalculationMode2);
				    rlabel1.setBounds(10, 20, 200, 30);
				    rlabel2.setBounds(90, 55, 120, 30);
				    gridCalculationMode1.setBounds(215,55, 20, 30);
				    rlabel3.setBounds(90, 90, 120, 30);
				    gridCalculationMode2.setBounds(215,90, 20, 30);
				    
				    RouteSecond.add(rlabel1);
				    RouteSecond.add(rlabel2);
				    RouteSecond.add(rlabel3);
				    RouteSecond.add(gridCalculationMode1);
				    RouteSecond.add(gridCalculationMode2);
				    
				    
					JLabel rlabel21 = new JLabel("���ֲ�����",JLabel.LEFT);
					gridLayer = new JComboBox();
					String[] description = {
							    "1","2","3"
					};
					for(int i = 0; i < 3; i++)
						gridLayer.addItem(description[i]);
					rlabel21.setBounds(10, 130, 70, 30);
					gridLayer.setBounds(90, 130, 80, 30);
					RouteSecond.add(rlabel21);
					RouteSecond.add(gridLayer);
				    
				    RouteSecond.repaint();
				}
				else{
					RouteSecond.removeAll();
				    RouteSecond.repaint();
				}
			}
		});
		
		jp1.setLayout(null);
		RouteFirst.setBounds(10, 0, 330, 350);
		RouteSecond.setBounds(340, 0, 330, 350);
		jp1.add(RouteFirst);
		jp1.add(RouteSecond);
		
		return jp1;
	}
	
	public JPanel CacheSetting(){
		JPanel jp2 = new JPanel();
		//---------------------------���û������ҳ��----------------------------//	
		JPanel CacheFirst = new JPanel();
		CacheFirst.setBorder(new TitledBorder("ͨ������"));
		CacheFirst.setLayout(null);
		
		//	��һ��
	    JLabel label1=new JLabel("����ȷ��",JLabel.LEFT);
	    JLabel label2=new JLabel("�������",JLabel.LEFT);
	    
	    HopByHop_Confirm = new JCheckBox();
	    HopByHop_Confirm.setSelected(false);

	    RandomDropMessage = new JCheckBox();
	    RandomDropMessage.setSelected(false);
	    
	    label1.setBounds(10, 20, 60, 30);
		HopByHop_Confirm.setBounds(70, 20, 20, 30);
	    label2.setBounds(110, 20, 60, 30);
	    RandomDropMessage.setBounds(170, 20, 20, 30);
	    CacheFirst.add(label1);
	    CacheFirst.add(HopByHop_Confirm);
	    CacheFirst.add(label2);
	    CacheFirst.add(RandomDropMessage);
	    
	    // ������
	    JLabel label3=new JLabel("�ڵ㻺���С��",JLabel.LEFT);
		JTextField txt3 = new JTextField("1000");
		JLabel label31 = new JLabel("k",JLabel.LEFT);		
		label3.setBounds(10, 60, 100, 30);
		txt3.setBounds(130,60, 130, 30);
		label31.setBounds(265, 60, 20, 30);
		CacheFirst.add(label3);
		CacheFirst.add(txt3);
		CacheFirst.add(label31);
	    
	    // ������
	    JLabel label4 = new JLabel("�ļ���������",JLabel.LEFT);
		JTextField txt4 = new JTextField("1");
		JLabel label41 = new JLabel("Min",JLabel.LEFT);

		label4.setBounds(10, 100, 100, 30);
		txt4.setBounds(130,100, 130, 30);
		label41.setBounds(265, 100, 20, 30);
		CacheFirst.add(label4);
		CacheFirst.add(txt4);
		CacheFirst.add(label41);
	    
	    // ������
	    JLabel label5=new JLabel("�ļ������С��",JLabel.LEFT);
		final JTextField txt5 = new JTextField("1");
		final JTextField txt51 = new JTextField("10");
		JLabel label51 = new JLabel("~",JLabel.LEFT);
		JLabel label52 = new JLabel("k",JLabel.LEFT);

		label5.setBounds(10, 140, 100, 30);
		txt5.setBounds(130, 140, 60, 30);
		label51.setBounds(190, 140, 10, 30);
		txt51.setBounds(200, 140, 60, 30);
		label52.setBounds(265, 140, 20, 30);
		CacheFirst.add(label5);
		CacheFirst.add(txt5);
		CacheFirst.add(label51);
		CacheFirst.add(txt51);
		CacheFirst.add(label52);
		
	    // ������
	    JLabel label6 = new JLabel("�ļ���Ŀ��",JLabel.LEFT);
		nrofFile = new JTextField("60");
		JLabel label61 = new JLabel("��",JLabel.LEFT);
		label6.setBounds(10, 180, 100, 30);
		nrofFile.setBounds(130,180, 130, 30);
		label61.setBounds(265, 180, 20, 30);
		CacheFirst.add(label6);
		CacheFirst.add(nrofFile);
		CacheFirst.add(label61);
		
	    JLabel label7 = new JLabel("��Ƭ��Ŀ��",JLabel.LEFT);
		JTextField txt7 = new JTextField("10");
		JLabel label71 = new JLabel("��",JLabel.LEFT);
		label7.setBounds(10, 220, 100, 30);
		txt7.setBounds(130,220, 130, 30);
		label71.setBounds(265, 220, 20, 30);
		CacheFirst.add(label7);
		CacheFirst.add(txt7);
		CacheFirst.add(label71);

	    
		//---------------------------���ö����������ҳ��----------------------------//			
		JPanel CacheSecond = new JPanel();
		CacheSecond.setBorder(new TitledBorder("�ض���������"));
		
		jp2.setLayout(null);
		CacheFirst.setBounds(10, 0, 330, 350);
		CacheSecond.setBounds(340, 0, 330, 350);
		jp2.add(CacheFirst);
		jp2.add(CacheSecond);
		
		return jp2;
	}
	
	public JPanel MovementSetting(){
		JPanel jp3 = new JPanel();
		//JPanel jp = new JPanel();
		//---------------------------���û������ҳ��----------------------------//	
		JPanel MovementFirst = new JPanel();
		MovementFirst.setBorder(new TitledBorder("���ǹ����������"));
		//MovementFirst.setLayout(new GridLayout(8,1,10,10));
		MovementFirst.setLayout(null);
		
		//��һ��
	    JLabel label1=new JLabel("�������ã�",JLabel.LEFT);
		JComboBox WalkerC = new JComboBox();
		String[] description = {
				    "Walker star", "Walker delta",
		};
		
	    for(int i = 0; i < 2; i++)
	    	WalkerC.addItem(description[i]);
	    
	    label1.setBounds(10, 25, 100, 30);
	    WalkerC.setBounds(130, 25, 120, 30);
	    MovementFirst.add(label1);
	    MovementFirst.add(WalkerC);

		
		//  �ڶ���
	    JLabel label2=new JLabel("���ģ�ͣ�",JLabel.LEFT);
		JComboBox OrbitC = new JComboBox();
		String[] description1 = {
				    "TwoBody Model", "Perturbation model",
		};
		
	    for(int i = 0; i < 2; i++)
	    	OrbitC.addItem(description1[i]);
	    
	    label2.setBounds(10, 65, 100, 30);
	    OrbitC.setBounds(130, 65, 120, 30);
	    MovementFirst.add(label2);
	    MovementFirst.add(OrbitC);
  
	    /**������**/
	    JLabel label3 = new JLabel("���ǹ�����ͣ�",JLabel.LEFT);
	    satelliteType = new JComboBox();
		String[] description2 = {
				    "�͹�����", "�й�����",
		};

	 	label3.setBounds(10, 105, 100, 30);
	 	satelliteType.setBounds(130, 105, 120, 30);
	    for(int i = 0; i < 2; i++)
	    	satelliteType.addItem(description2[i]);
	    
	 	MovementFirst.add(label3);
	 	MovementFirst.add(satelliteType);
		//---------------------------���ö����������ҳ��----------------------------//	
		final JPanel MovementSecond = new JPanel();
		MovementSecond.setLayout(null);
		
		MovementSecond.setBorder(new TitledBorder("LEO��������"));
		
		/**�ظ����ã�������**/
		satelliteTypeSecondPanel_LEO(MovementSecond);
		/**�ظ����ã�������**/
		
	 	satelliteType.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(satelliteType.getSelectedIndex()==0){
					/**�������ǹ�������������� -- LEO**/
					satelliteTypeSecondPanel_LEO(MovementSecond);
				}
				if(satelliteType.getSelectedIndex()==1){
					/**�������ǹ�������������� -- MEO**/
					satelliteTypeSecondPanel_MEO(MovementSecond);
				}
			}
		});

		jp3.setLayout(null);
		MovementFirst.setBounds(10, 0, 330, 350);
		MovementSecond.setBounds(340, 0, 330, 350);
		jp3.add(MovementFirst);
		jp3.add(MovementSecond);
		return jp3;
	}
	/**
	 * �������ǹ�������������� -- LEO
	 * @param MovementSecond
	 */
	public void satelliteTypeSecondPanel_LEO(JPanel MovementSecond){
		MovementSecond.removeAll();
		MovementSecond.setBorder(new TitledBorder("LEO��������"));
		
		JLabel mslabel1 = new JLabel("������M/N/P��",JLabel.LEFT);
		JLabel mslabel11 = new JLabel("M-�ܵĽڵ�����",JLabel.LEFT);
		JLabel mslabel12 = new JLabel("N-���ƽ������",JLabel.LEFT);	
		JLabel mslabel13 = new JLabel("P-��λ���ӣ�",JLabel.LEFT);		
		JLabel mslabel2 = new JLabel("����뾶��",JLabel.LEFT);
		JLabel label21 = new JLabel("km",JLabel.CENTER);						
		JLabel mslabel3 = new JLabel("�����ʣ�",JLabel.LEFT);		
		JLabel mslabel4 = new JLabel("�������ǣ�",JLabel.LEFT);
		
		if (totalNodes == null){
			totalNodes = new JTextField("36");
			totalPlane = new JTextField("3");
			phaseFactor = new JTextField("0");
			radius = new JTextField("785");
			eccentricity = new JTextField("0");
			orbitPlaneAngle = new JTextField("45");
		}
			
		
		mslabel1.setBounds(10, 20, 100, 30);
		mslabel11.setBounds(35, 60, 100, 30);
		totalNodes.setBounds(145,60, 100, 30);
		mslabel12.setBounds(35, 100, 100, 30);
		totalPlane.setBounds(145, 100, 100, 30);
		mslabel13.setBounds(35, 140, 100, 30);
		phaseFactor.setBounds(145, 140, 100, 30);
		
		mslabel2.setBounds(10, 185, 100, 30);
		label21.setBounds(205, 185, 100, 30);
		radius.setBounds(145, 185, 100, 30);
		mslabel3.setBounds(10, 225, 100, 30);
		eccentricity.setBounds(145, 225, 100, 30);
		mslabel4.setBounds(10, 265, 100, 30);
		orbitPlaneAngle.setBounds(145, 265, 100, 30);
		
		MovementSecond.add(mslabel1);
		MovementSecond.add(mslabel11);
		MovementSecond.add(totalNodes);
		MovementSecond.add(mslabel12);
		MovementSecond.add(totalPlane);
		MovementSecond.add(mslabel13);
		MovementSecond.add(phaseFactor);
		MovementSecond.add(mslabel2);
		MovementSecond.add(label21);
		MovementSecond.add(radius);
		MovementSecond.add(mslabel3);
		MovementSecond.add(eccentricity);
		MovementSecond.add(mslabel4);
		MovementSecond.add(orbitPlaneAngle);
	}
	/**
	 * �������ǹ�������������� -- LEO
	 * @param MovementSecond
	 */
	public void satelliteTypeSecondPanel_MEO(JPanel MovementSecond){
		MovementSecond.removeAll();
		MovementSecond.setBorder(new TitledBorder("MEO��������"));
		
		//��һ�� ������M/N/P
		JLabel mslabel1 = new JLabel("������M/N/P��",JLabel.LEFT);
		JLabel mslabel11 = new JLabel("M-�ܵĽڵ�����",JLabel.LEFT);
		JLabel mslabel12 = new JLabel("N-���ƽ������",JLabel.LEFT);
		JLabel mslabel13 = new JLabel("P-��λ���ӣ�",JLabel.LEFT);
		JLabel mslabel2 = new JLabel("����뾶��",JLabel.LEFT);
		JLabel label21 = new JLabel("km",JLabel.CENTER);
		JLabel mslabel3 = new JLabel("�����ʣ�",JLabel.LEFT);
		JLabel mslabel4 = new JLabel("�������ǣ�",JLabel.LEFT);
		
		if (MEOtotalNodes == null){
			MEOtotalNodes = new JTextField("0");
			MEOtotalPlane = new JTextField("0");
			MEOphaseFactor = new JTextField("0");
			MEOradius = new JTextField("10000");
			MEOeccentricity = new JTextField("0");
			MEOorbitPlaneAngle = new JTextField("45");
		}
		
		mslabel1.setBounds(10, 20, 100, 30);
		mslabel11.setBounds(35, 60, 100, 30);
		MEOtotalNodes.setBounds(145,60, 100, 30);
		mslabel12.setBounds(35, 100, 100, 30);
		MEOtotalPlane.setBounds(145, 100, 100, 30);
		mslabel13.setBounds(35, 140, 100, 30);
		MEOphaseFactor.setBounds(145, 140, 100, 30);
		
		mslabel2.setBounds(10, 185, 100, 30);
		label21.setBounds(205, 185, 100, 30);
		MEOradius.setBounds(145, 185, 100, 30);
		mslabel3.setBounds(10, 225, 100, 30);
		MEOeccentricity.setBounds(145, 225, 100, 30);
		mslabel4.setBounds(10, 265, 100, 30);
		MEOorbitPlaneAngle.setBounds(145, 265, 100, 30);
		
		MovementSecond.add(mslabel1);
		MovementSecond.add(mslabel11);
		MovementSecond.add(MEOtotalNodes);
		MovementSecond.add(mslabel12);
		MovementSecond.add(MEOtotalPlane);
		MovementSecond.add(mslabel13);
		MovementSecond.add(MEOphaseFactor);
		MovementSecond.add(mslabel2);
		MovementSecond.add(label21);
		MovementSecond.add(MEOradius);
		MovementSecond.add(mslabel3);
		MovementSecond.add(MEOeccentricity);
		MovementSecond.add(mslabel4);
		MovementSecond.add(MEOorbitPlaneAngle);
	}
	
	public JPanel LinkSetting(){
		JPanel jp4 = new JPanel();
		//---------------------------���û������ҳ��----------------------------//	
		JPanel LinkFirst = new JPanel();
		LinkFirst.setBorder(new TitledBorder("ͨ������"));
		LinkFirst.setLayout(null);
		
		
		//��һ��    
	    JLabel label1=new JLabel("��·ģ�ͣ�",JLabel.RIGHT);	
	    
		JComboBox linkModel = new JComboBox();
		String[] description = {
				    "CBR Connection", "VBR Connection",
		};		
	    for(int i = 0; i < 2; i++)
	    	linkModel.addItem(description[i]);
	    
		JLabel label2 = new JLabel("����ͷ");
		JTextField text1 = new JTextField("1");
		JLabel label3 = new JLabel("΢������");
		JTextField text2 = new JTextField("1");
		JLabel label4 = new JLabel("��");
		JLabel label5 = new JLabel("��");
		
		label1.setBounds(10, 25, 100, 30);
		label2.setBounds(100, 65, 60, 30);
		text1.setBounds(170, 65, 60, 30);
		label4.setBounds(240, 65, 20, 30);
		label3.setBounds(100, 105, 60, 30);
		text2.setBounds(170, 105, 60, 30);
		label5.setBounds(240, 105, 20, 30);
		linkModel.setBounds(130, 25, 120, 30);
		
		LinkFirst.add(label1);
		LinkFirst.add(linkModel);
		LinkFirst.add(label2);
		LinkFirst.add(text1);
		LinkFirst.add(label3);
		LinkFirst.add(text2);
		LinkFirst.add(label4);
		LinkFirst.add(label5);
		
		//---------------------------���ö����������ҳ��----------------------------//			
		JPanel LinkSecond = new JPanel();
		LinkSecond.setBorder(new TitledBorder("�ض���������"));
		
		
		jp4.setLayout(null);
		LinkFirst.setBounds(10, 0, 330, 350);
		LinkSecond.setBounds(340, 0, 330, 350);
		jp4.add(LinkFirst);
		jp4.add(LinkSecond);
		
		return jp4;
	}
	
	public JPanel MainSetting(){
		JPanel jp5 = new JPanel();
		//---------------------------�����������ҳ��----------------------------//	
		JPanel LinkFirst = new JPanel();
		LinkFirst.setBorder(new TitledBorder("ͨ������"));
		LinkFirst.setLayout(null);
		
		//��һ��
	    JLabel label1 = new JLabel("��ά�߽磺",JLabel.RIGHT);
	    worldSizeX = new JTextField("100000");
	    worldSizeY = new JTextField("100000");
	    worldSizeZ = new JTextField("100000");	    
		JLabel km = new JLabel("Km");

		label1.setBounds(0, 25, 80, 30);
		worldSizeX.setBounds(95, 25, 50, 30);
		worldSizeY.setBounds(155, 25, 50, 30);
		worldSizeZ.setBounds(215, 25, 50, 30);
		km.setBounds(270, 25, 20, 30);
		LinkFirst.add(label1);
		LinkFirst.add(worldSizeX);
		LinkFirst.add(worldSizeY);
		LinkFirst.add(worldSizeZ);
		LinkFirst.add(km);
		
		//�ڶ��� ����ʱ������
		JLabel label2 = new JLabel("����ʱ�䣺",JLabel.RIGHT);
		JLabel time = new JLabel("s");
		simTime = new JTextField("12000",JTextField.CENTER);
		simTime.addActionListener(this);
		label2.setBounds(0, 65, 80, 30);
		simTime.setBounds(95, 65, 170, 30);
		time.setBounds(270, 65, 20, 30);
		LinkFirst.add(label2);
		LinkFirst.add(simTime);
		LinkFirst.add(time);
		
		
		//������ ���沽��
		JLabel label3 = new JLabel("���沽����",JLabel.RIGHT);
		JLabel time2 = new JLabel("s");
		interval = new JTextField("0.1");
		label3.setBounds(0, 105, 80, 30);
		interval.setBounds(95, 105, 170, 30);
		time2.setBounds(270, 105, 20, 30);
		LinkFirst.add(label3);
		LinkFirst.add(interval);
		LinkFirst.add(time2);
		
		//������ Ԥ��ʱ��
		JLabel label4 = new JLabel("Ԥ��ʱ�䣺",JLabel.RIGHT);
		JLabel time3 = new JLabel("s");
		warmUp = new JTextField("10");
		label4.setBounds(0, 145, 80, 30);
		warmUp.setBounds(95, 145, 170, 30);
		time3.setBounds(270, 145, 20, 30);
		LinkFirst.add(label4);
		LinkFirst.add(warmUp);
		LinkFirst.add(time3);
		
		
		//---------------------------���ö����������ҳ��----------------------------//			
		JPanel LinkSecond = new JPanel();
		LinkSecond.setBorder(new TitledBorder("�ض���������"));
		
		jp5.setLayout(null);
		LinkFirst.setBounds(10, 0, 330, 350);
		LinkSecond.setBounds(340, 0, 330, 350);
		jp5.add(LinkFirst);
		jp5.add(LinkSecond);
		
		
		return jp5;
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() == this.Confirm) {
			this.ParaSet();
			this.setVisible(false);
		}
		if (e.getSource() == this.Concel) {
			this.setVisible(false);
		}
		if (e.getSource() == this.Reset) {
			
		}
		
	}
	

	//--------------------------- �������� --------------------------//
	public void ParaSet(){
		this.RouteParaSet();
		this.CacheParaSet();
		this.MoveParaSet();
		this.LinkParaSet();
	}

	private void LinkParaSet() {
		// TODO Auto-generated method stub
		
	}

	private void MoveParaSet() {
		// TODO Auto-generated method stub
		
	}

	private void CacheParaSet() {
		// TODO Auto-generated method stub
		Settings settings = new Settings();
		/** �Ƿ����û��湦�� */
		if (HopByHop_Confirm.isSelected()){			//���Ͷ���ģʽ
			settings.setSetting("userSetting.EnableCache","true");			//����ȷ��
		}else{
			settings.setSetting("userSetting.EnableCache","false");			//����ת��
		}

		if(RandomDropMessage.isSelected()){
			settings.setSetting("userSetting.RandomDropMessage","true");	//�������
		}else{
			settings.setSetting("userSetting.RandomDropMessage","false");	//��ѡ���������
		}
		
		/**	���÷������ļ���Ŀ*/
		settings.setSetting("Group.nrofFile", String.valueOf(nrofFile.getText()));

	}

	public void RouteParaSet(){
		Settings settings = new Settings();
		
//		HashMap<String, String> map = new HashMap<String, String>();
//		map.put("Group.router",String.valueOf(RouterC.getSelectedItem()));
//		settings.setSetting(map);
		
		settings.setSetting("Group.router",String.valueOf(RouterC.getSelectedItem()));
		if (gridLayer != null)
			settings.setSetting("Group.layer",String.valueOf(gridLayer.getSelectedItem()));
		/**���Ͷ���ģʽѡ��**/
		if (queueMode1.isSelected())//���Ͷ���ģʽ
			settings.setSetting("Group.sendQueue","1");//Random
		else
			settings.setSetting("Group.sendQueue","2");//FIFO
		/**·��ģʽѡ��**/
		if (routerMode1.isSelected())//·��ģʽ
			settings.setSetting("userSetting.routerMode","AllConnected");//����ȷ��
		else
			settings.setSetting("userSetting.routerMode","Cluster");//ָ��·��
		/**����·�ɼ���ģʽ**/
		if (gridCalculationMode1 != null){
			if (gridCalculationMode1.isSelected())
				settings.setSetting("Group.Pre_or_onlineOrbitCalculation","onlineOrbitCalculation");//��ǰ����������Ϣ���洢
			else
				settings.setSetting("Group.Pre_or_onlineOrbitCalculation","preOrbitCalculation");//ʵʱ��������
		}
		
		settings.setSetting("Group.bufferSize",String.valueOf(bufferSize.getText()) + "M");
		settings.setSetting("Interface.transmitSpeed",String.valueOf(transmissionRate.getText()) + "k");	//kbps
		settings.setSetting("Interface.transmitRange",String.valueOf(transmissionRadius.getText()));		//km
		
		settings.setSetting("Scenario.endTime",String.valueOf(simTime.getText()));
		settings.setSetting("Scenario.updateInterval",String.valueOf(interval.getText()));
		settings.setSetting("MovementModel.warmup",String.valueOf(warmUp.getText()));


		
		settings.setSetting("MovementModel.worldSize", String.valueOf(worldSizeX.getText()) + ", " + 
					String.valueOf(worldSizeY.getText()) + ", " + String.valueOf(worldSizeZ.getText()));
		
		orbitSetting_LEO();
		orbitSetting_MEO();
	}
	/**
	 * ����LEO���ǹ������
	 */
	public void orbitSetting_LEO(){
		Settings settings = new Settings();
		settings.setSetting("Group.nrofHosts",String.valueOf(totalNodes.getText()));
		String range = "[0,"+String.valueOf(totalNodes.getText())+"]";
		settings.setSetting("Events1.hosts", range);
		settings.setSetting("Group.nrofLEOPlanes",String.valueOf(totalPlane.getText()));
		settings.setSetting("userSetting.phaseFactor",String.valueOf(phaseFactor.getText()));
		settings.setSetting("userSetting.radius",String.valueOf(radius.getText()));
		settings.setSetting("userSetting.eccentricity",String.valueOf(eccentricity.getText()));
		settings.setSetting("userSetting.orbitPlaneAngle",String.valueOf(orbitPlaneAngle.getText()));
	}
	/**
	 * ����MEO���ǹ������
	 */
	public void orbitSetting_MEO(){
		Settings settings = new Settings();
		
		if (MEOtotalNodes != null){
			settings.setSetting("userSetting.nrofMEO",String.valueOf(MEOtotalNodes.getText()));
		}
		if (MEOtotalPlane != null){
			settings.setSetting("userSetting.MEOnrofPlane",String.valueOf(MEOtotalPlane.getText()));
		}
		if (MEOphaseFactor != null){
			settings.setSetting("userSetting.MEOphaseFactor",String.valueOf(MEOphaseFactor.getText()));
		}
		if (MEOradius != null){
			settings.setSetting("userSetting.MEOradius",String.valueOf(MEOradius.getText()));
		}
		if (MEOeccentricity != null){
			settings.setSetting("userSetting.MEOeccentricity",String.valueOf(MEOeccentricity.getText()));
		}
		if (MEOorbitPlaneAngle != null){
			settings.setSetting("userSetting.MEOorbitPlaneAngle",String.valueOf(MEOorbitPlaneAngle.getText()));
		}
	}

}
