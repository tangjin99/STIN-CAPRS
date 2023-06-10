package routing;

import com.sun.org.apache.xml.internal.resolver.helpers.PublicId;
import core.DTNHost;
import jdk.nashorn.internal.ir.RuntimeNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import routing.*;

public class Popularityinfo implements Serializable {
    private static final int Requestsinfosize=30;
    private int size;
    public int contentid;
    private double popularity;
    public DTNHost host;
    public List<Double> Requestsinfo;
    private double lamta=0;
    public int getPopularitylistsize(){
        return this.size;
    }

    public int getContentidfrompoplist(){
        return this.contentid;
    }

    public double getContentpopularity(){
        return popularity;
    }

    public List<Double> getRequestsinfo(){
        return this.Requestsinfo;
    }

    public void updateContentpopularityinfo(double totalrequests){
        popularity=this.getRequestsinfo().size()/totalrequests;
    }

    public int getContentrequeststimes(){

        return Requestsinfo.size();

    }

    public DTNHost getHostfrompoplist(){

        return host;
    }

    public void updateRequestsinfo(double lastrequest){
        if(Requestsinfo.size()<Requestsinfosize){
            Requestsinfo.add(lastrequest);
        }
        else {
            Requestsinfo.remove(0);
            Requestsinfo.add(lastrequest);
        }
        return ;
//        Requestsinfo.add(lastrequest);

    }

    public double getlamtaofcontent(int t1){
        int length=Requestsinfo.size();
        if(length==0){
            return 0;
        }
        else if(length==1){
//            DTNHost host=this.host;
            System.out.println("request interavl: "+Requestsinfo);
            //System.out.println(t1);
            System.out.println("lamta_content: "+(1/(Requestsinfo.get(0))));

            return 1/Requestsinfo.get(0);
     //       return this.host.getRouter().lamta_average;
        }
        else {
            double lamta=0;

            lamta=2/(Requestsinfo.get(length-1)-Requestsinfo.get(length-2));
            System.out.println("request interavl: "+Requestsinfo);
            System.out.println("lamta_content: "+lamta);
            return lamta;
        }
    }

    public void setRequestsinfo(double lastrequest){
        this.Requestsinfo.add(lastrequest);
    }

    public void setContentpopularity(double totalrequests){
        popularity=this.Requestsinfo.size()/totalrequests;
    }

    public Popularityinfo replicate(Popularityinfo popinfo){
        Popularityinfo copypop=new Popularityinfo(popinfo.contentid,popinfo.host);
        List<Double> requestinfo=new ArrayList<Double>();
        for(double time :popinfo.Requestsinfo){
            requestinfo.add(time);
        }
        copypop.Requestsinfo=requestinfo;
        copypop.popularity=popinfo.popularity;

        return copypop;
    }

    public Popularityinfo(int contentid,DTNHost host){
        this.contentid=contentid;
        this.popularity=0;
        this.Requestsinfo=new ArrayList<>();
        this.host=host;
    }

}

