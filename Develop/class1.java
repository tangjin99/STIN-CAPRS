package Develop;


import java.awt.BorderLayout;  
import java.awt.Color;  
import java.awt.GradientPaint;  
import java.awt.Graphics;  
import java.awt.Graphics2D;  
import javax.swing.*;  
public class class1 extends JFrame{  
    private myPanel p;  
    public class1(String name){  
        super();            //�̳и���Ĺ��췽��  
        setTitle(name);                 //����  
        setBounds(0,0,300,300);     //��С  
        BorderLayout bl = new BorderLayout();  
        bl.setHgap(20);  
        bl.setVgap(20);  
        getContentPane().setLayout(bl);//���ֹ���  
            p = new myPanel("jarvischu");   
            p.setBounds(0, 0, 150, 150);  
            getContentPane().add(p,bl.CENTER);  
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);//����Ĭ�Ϲرղ���  
    }                                                                                                             
    public static void main(String args[]){  
    	class1 frame = new class1("JarvisChu");  
        frame.setVisible(true);       
    }   
}  
class myPanel extends JPanel{  
    private String m_Name;  
    public myPanel(String name){  
        m_Name = name;  
    }     
    public void paint(Graphics g){  
        Graphics2D g2d = (Graphics2D)g;  
        GradientPaint grdp = new GradientPaint(0,0,Color.blue,100,50,Color.RED);  
                                                           //����һ���������Ķ���  
        g2d.setPaint(grdp);                        //ѡ�и�Paint����  
        g2d.fillRect(0, 0, 150, 150);  
    }  
}  