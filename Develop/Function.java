package Develop;

import javax.swing.*;
import java.applet.Applet;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.ArrayList;
import core.DTNHost;
import core.Settings;

public class Function extends Applet implements Runnable{
	
	private static ImageIcon image = new ImageIcon("images\\earth4.jpg");
	private static final double WIDTH = image.getIconWidth();//Toolkit.getDefaultToolkit().getScreenSize().getWidth();
	private static final double HEIGHT = image.getIconHeight();//Toolkit.getDefaultToolkit().getScreenSize().getHeight();
	private static final int INCREMENT = 20;
//	Plot2DPanel plot = new Plot2DPanel();
	
	int step = 0;
	int SIZE1 = 180;
	int SIZE2 = 90;
	List<DTNHost> hosts;
	int steps = 200;
	double[][] BL = new double[steps][2];
	
//	public static void main(String[] args) {
//		//new Function();
//	}
	
	public Function(List<DTNHost> hosts) {
		
		this.hosts = hosts;
		
		//this.setTitle("���ǹ켣�Ķ�ά����ͼ");
		this.setLocation(50, 50);
	//	this.setSize(1000, 600);
		this.setVisible(true);
		//this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//this.getContentPane().setBackground(null);
		
		
		JLabel label = new JLabel(image);
		label.setBounds(0, 0, (int)WIDTH, (int)HEIGHT);
		this.add(label,BorderLayout.CENTER);
		
		
		//this.setExtendedState(JFrame.MAXIMIZED_BOTH);
	//	this.setSize(image.getIconWidth(), image.getIconHeight());
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g;
		Color source = g2d.getColor();
	//	System.out.println("color "+ source);
		
		g2d.setColor(Color.BLACK);
	//	g2d.drawString("sinx/x ��ͼ��", 50, 50);
		

		// �� X ��
		g2d.drawLine(INCREMENT, (int)HEIGHT/2, (int)WIDTH-INCREMENT, (int)HEIGHT/2);
		g2d.drawLine((int)WIDTH-INCREMENT, (int)HEIGHT/2, (int)WIDTH-10, (int)HEIGHT/2-5);
		g2d.drawLine((int)WIDTH-INCREMENT, (int)HEIGHT/2, (int)WIDTH-10, (int)HEIGHT/2+5);
		

		// �� Y ��
		g2d.drawLine((int)WIDTH/2, 40, (int)WIDTH/2, (int)HEIGHT-50);
		g2d.drawLine((int)WIDTH/2, 40, (int)WIDTH/2-10, 50);
		g2d.drawLine((int)WIDTH/2, 40, (int)WIDTH/2+10, 50);
		
		// ����ǰ�����ƶ�������
		g2d.translate((int) WIDTH / 2, (int) HEIGHT / 2);

		// ����GeneralPath����������
		GeneralPath gp = new GeneralPath();

		// ��GeneralPath��ʵ��gp�Ļ����ƶ�����ǰ��������ģ�����������������g2d���ʵ����ĵ�
		gp.moveTo(0, 0);

		// ��sin(x)/x ��ͼ��
//		drawSinxDX(gp, g2d);

		// sin(x)��ͼ��
//		g2d.setColor(Color.YELLOW);
		drawOrbit(gp, g2d,hosts);
		drawPoints(gp,g2d);

//		drawCosx(gp, g2d);

		// tan(x)��ͼ��
//		drawTanx(gp, g2d);
//		g2d.setColor(source);
	}

	private void drawTanx(GeneralPath gp, Graphics2D g2d) {
		for (double i = 0.000001; i <= 8*Math.PI; i+=0.0001*Math.PI) {
			gp.lineTo(20*i, 100*-Math.tan(i));
		}
		g2d.draw(gp);

		// ����ǰ������ԭ��Ϊ���ģ���ת180�ȣ����溯��������ԭ��Գƣ�
		g2d.rotate(Math.PI);
		g2d.draw(gp);
	}

	private void drawCosx(GeneralPath gp, Graphics2D g2d) {
		for (double i = 0.000001; i <= 8*Math.PI; i+=0.0001*Math.PI) {
			gp.lineTo(20*i, 100*-Math.cos(i));
		}
		g2d.draw(gp);

		// ����ǰ������Y��Ϊ�Գ��ᣬ��ż����(����Y��Գ�)
		g2d.scale(-1, 1);
		g2d.draw(gp);
	}
	private void drawSinx(GeneralPath gp, Graphics2D g2d) {
		for (double i = 0.000001; i <= 8*Math.PI; i+=0.0001*Math.PI) {
        	  gp.lineTo(20*i, 100*-Math.sin(i));
        	}
		g2d.draw(gp);
		g2d.rotate(Math.PI);
		g2d.draw(gp);
	}

	private void drawSinxDX(GeneralPath gp, Graphics2D g) {
		for (double i = 0.000001; i <= 8*Math.PI; i+=0.0001*Math.PI) {
        	 gp.lineTo(20*i, 100*-Math.sin(i)/i);
         	}
		g.draw(gp);
		g.scale(-1, 1);
		g.draw(gp);
	}
	
	private void drawOrbit(GeneralPath gp, Graphics2D g2d, List<DTNHost> hosts) {
		for(int j=0;j<hosts.size();++j) {
			if(j==0||j==8||j==16) {
			for(int i=0;i<steps;++i) {
				gp.lineTo(3.8*hosts.get(j).getBL()[i][0], 3.8*hosts.get(j).getBL()[i][1]);
				}
			g2d.setColor(Color.WHITE);
			g2d.draw(gp);
			}
		}
		for(int j=0;j<hosts.size();++j) {
				g2d.setColor(Color.RED);
				g2d.fillOval((int)(3.8*hosts.get(j).getBL()[step][0]),
						(int)(3.8*hosts.get(j).getBL()[step][1]),10,10);
		}
/*		double time_increment = 0.1;
		for(int i=0;i<hosts.size();++i) {
			plot.addLinePlot("orbit", Color.BLACK, hosts.get(i).getBL());
			plot.addScatterPlot("satellite", Color.RED, hosts.get(i).get2Dpoints());
		}
		this.add(plot);*/
        
    /*    for(int i=0;i<steps;++i) {
			gp.lineTo(3.8*hosts.get(0).getBL()[i][0], 3.8*hosts.get(0).getBL()[i][1]);
			}
		g2d.setColor(Color.WHITE);
		g2d.draw(gp);
		
		for(int i=0;i<steps;++i) {
			gp.lineTo(3.6*hosts.get(8).getBL()[i][0], 3.8*hosts.get(8).getBL()[i][1]);
			}
//		g2d.setColor(Color.GREEN);
		g2d.draw(gp);
		
		for(int i=0;i<steps;++i) {
			gp.lineTo(3.6*hosts.get(16).getBL()[i][0], 3.8*hosts.get(16).getBL()[i][1]);
			}
//		g2d.setColor(Color.YELLOW);
		g2d.draw(gp);*/
        
   //     System.out.println(".-.");
		
	}
	
	private void drawPoints(GeneralPath gp, Graphics2D g2d) {
	/*	System.out.println(".-.");
		Plot2DPanel p2 = new Plot2DPanel();
        for (int i = 0; i < 3; i++) {
            double[][] XYZ = new double[10][2];
            for (int j = 0; j < XYZ.length; j++) {*/
           //     XYZ[j][0] = /*1 + */ 100*Math.random();
           //     XYZ[j][1] = /*100 * */ 100*Math.random();
    /*        }
            p2.addScatterPlot("toto" + i, XYZ);
        }
        new FrameView(p2).setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        System.out.println(".-.");*/
		
		
	}
	
	public void run() {
		while(true) {
			try {
				Thread.sleep(100);
				}
			catch(InterruptedException e) {
			    //e.printStackTrace();
		    }
			step+=1;
		    this.repaint();
		    if(step>=199) {
			    step = 0;
		    }
		}
	}
	
	public void move() {
	/*	while(true) {
			try {
				Thread.sleep(100);
			}
			catch(InterruptedException e) {
				//e.printStackTrace();
			}
			step+=1;
			this.repaint();
			if(step>=199) {
				step = 0;
			}
		}*/
	}
}