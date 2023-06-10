/*
 * copyright 2017 ustc, Infonet
 */
package Cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import core.SimClock;

import javax.swing.*;

public class File {
    private String id;
    private int size;
    private int fromAddressID;
    private double timeRequest;
    private boolean initFile;             							//	�����ж��Ƿ�Ϊ��ʼ��֮����뻺���е��ļ�
    private ArrayList<Integer> data = new ArrayList<Integer>();    	//	�ö�̬��������ʾ�ļ�������
    private int dataSize=100;										//	���ݴ�СΪ100
    /**tangjin*/
//    private String  contentname;
//    private Double contentpopularity;
//    private List<Double> contentrequestinfo;
//    private popularitylist popularitylist;
    /**tangjin*/


    /**tangjin*/
//    public String getContentname(){
//        return  contentname;
//    }
//    public Double getContentpopularity(){
//        return contentpopularity;
//    }
//    public List<Double> getContentrequestinfo(){
//        return contentrequestinfo;
//    }
//
//    public popularitylist getPopularitylist(){
//        popularitylist poplist=new popularitylist();
//        poplist.popularity=this.contentpopularity;
//        poplist.requestinfo=this.contentrequestinfo;
//        return poplist;
//    }
//
//    public popularitylist updatepopularity(){
//
//    }

    /**tangjin*/


    public String getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public int getFromAddressID() {
        return fromAddressID;
    }
    
    public double getTimeRequest(){
    	return this.timeRequest;
    }
    
    public void setTimeRequest(double time){
		this.timeRequest = time;
    }
    
    public void setInitFile(File file){
    	file.initFile=false;
    }
    
    public boolean getInitFile(){
    	return this.initFile;
    }
    
    /**
     * ���ļ��ĸ��ƺ�����ԭ���ǲ�ϣ�������ô��ݲ�����
     * @param File
     * @return
     */
	public File copyFrom(File File) {	
		//System.out.println(File.getId());
		File f = new File();		
		f.id = File.getId();
		f.size = File.getSize();
		f.timeRequest = File.getTimeRequest();
		f.fromAddressID = File.getFromAddressID();
		f.initFile = File.getInitFile();
		//f.data= File.data;
		return f;
	}
    
    public void setSize(int size) {						//�ļ��Ĵ�С��������õ�
        //Random random = new Random();
//        int max=5000;
//        int min=size/10;
//        this.size=random.nextInt(max)%(max-min+1) + min;
    	this.size = size;
    }

    public void setFromAddressID(int  nrofHosts) {
        Random random = new Random();
        this.fromAddressID = random.nextInt(nrofHosts);
        
//        System.out.println("�ļ���fromIDΪ��"+this.fromAddressID);
    }
    
    public ArrayList<Integer> getData(){
    	return this.data;
    }
    
    public void copyData(File File){
    	this.data = File.getData();
    }
    
    
    /**
     * ���ļ����г�ʼ��
     * @param
     * @param
     */
    public File(int ID, int nrofHosts){
        this.id="filename"+ID;
        this.timeRequest= SimClock.getTime();			// �ļ��ڴ�����ʱ������ʱ���Ϊ��ǰ�ļ�����ʱ�䡣
        this.initFile= true;
        setSize(20000);
        setFromAddressID(nrofHosts);					// ���������һ���ڵ���Ҳ�������

        
        for(int i=0;i<dataSize;i++){
        	int m;
        	m=getRandomInt(100);						// data�����д洢��Ҳ�������
        	data.add(i, m);
        }
    }
    
    public File(){    }
    
    /**
     * ��������data������Ҫ�洢�������
     * @param max Ϊ���ֵ
     * @return ���������
     */
    public int getRandomInt(int max){					
    	int m;
    	Random random = new Random();
        m=random.nextInt(max)%(max+1);
        return m;
    }
}
