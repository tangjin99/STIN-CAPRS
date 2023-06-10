package Develop;

import javax.swing.JWindow;

import java.awt.Color;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

@SuppressWarnings ("serial" )
public class WaitingWindow extends JWindow implements Runnable{
	//������ش��ڴ�С
	public static final int LOAD_WIDTH = 400;
	public static final int LOAD_HEIGHT = 225;
	// ��ȡ��Ļ���ڴ�С
	public static final int WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
	public static final int HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
	public JLabel label;
	public JLabel waitTime;

	// ���캯��
	public WaitingWindow() {
		// ������ǩ , ���ڱ�ǩ�Ϸ���һ��ͼƬ
		label = new JLabel(new ImageIcon("images/loading.gif"));
		label.setBounds(0, 0, LOAD_WIDTH, LOAD_HEIGHT - 15);
		waitTime = new JLabel("�����������룬�ȴ�ʱ��Ϊ��0 s");
		waitTime.setBounds(0, LOAD_HEIGHT - 15, LOAD_WIDTH, 15);
		this.add(label);
		this.add(waitTime);

		// ���ò���Ϊ��
		this.setLayout(null);
		// ���ô��ڳ�ʼλ��
		this.setLocation((WIDTH - LOAD_WIDTH) / 2, (HEIGHT - LOAD_HEIGHT) / 2);
		// ���ô��ڴ�С
		this.setSize(LOAD_WIDTH, LOAD_HEIGHT);
		// ���ô�����ʾ
		this.setVisible(true);
		this.setAlwaysOnTop(true);
	}

	@Override
	public void run() {
		for (int i = 0; i < 100000; i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.setTime(i);
		}

	}
	public void setTime(int i){
		this.waitTime.setText("�����������룬�ȴ�ʱ��Ϊ��"+ i +" "+"s");
	}
    /**
     * ������Դ
     */
    private WaitingWindow Loading(){
		WaitingWindow t = new WaitingWindow();
		new Thread(t).start();
		return t;
    }
    
}