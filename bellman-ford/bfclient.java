import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class bfclient{
	
	private final static String[] cmd = new String[]{"LINKDOWN" , "LINKUP" , "SHOWRT" , "CLOSE"};
	public static int localport;
	public static void main(String[] args) throws InterruptedException, NumberFormatException, IOException {
	//	args = new String[]{"4115" , "10" , "192.168.1.168" , "4116" , "5" , "192.168.1.168" , "4118" , "30"};
		localport = Integer.parseInt(args[0]);
		int timeout = Integer.parseInt(args[1]);
		
		ArrayList<Link> linkinfo = new ArrayList<Link>();
		ArrayList<Link> neighbor = new ArrayList<Link>();
		ArrayList<NeighborLink> neighborinfo = new ArrayList<NeighborLink>();
		
		if((args.length - 2) % 3 == 0){
			linkinfo.add(new Link(InetAddress.getLocalHost().getHostAddress() , localport , InetAddress.getLocalHost().getHostAddress(), localport , 0 , InetAddress.getLocalHost().getHostAddress() , localport , true));
			for(int i = 0 ; i < (args.length - 2)/3 ; i++){
				linkinfo.add(new Link(InetAddress.getLocalHost().getHostAddress() , localport , args[3 * i + 2], Integer.parseInt(args[3 * i + 3]) , Double.parseDouble(args[3 * i + 4]), InetAddress.getLocalHost().getHostAddress(), localport , true));
				neighborinfo.add(new NeighborLink(args[3 * i + 2], Integer.parseInt(args[3 * i + 3]) , new Link(InetAddress.getLocalHost().getHostAddress() + ": " + localport , args[3 * i + 4] , args[3 * i + 2] + ": " + args[3 * i + 3] , true)));
			}
			for(int i = 0 ; i < (args.length - 2)/3 ; i++){
				neighbor.add(new Link(args[3 * i + 2] + ": " + args[3 * i + 3] , args[3 * i + 4]));
			}
		}
		else
			System.out.println("Invalid input");
		
		DistanceVector dv = new DistanceVector(neighbor);
		dv.createdv(linkinfo);
		
		ListenThread ltrd = new ListenThread(localport);
		Thread listen = new Thread(ltrd);
		listen.start();
		
		Timer timer = new Timer(neighborinfo , timeout);
		Thread time = new Thread(timer);
		time.start();
		
		UserInterface ui = new UserInterface();
		Thread console = new Thread(ui);
		console.start();
				
		broadcast (neighborinfo , dv , localport);
	//	Broadcast broadcast = new Broadcast(neighborinfo , dv , localport);
	//	Thread brdcst = new Thread(broadcast);
	//	brdcst.start();
		
		while(true){
			Thread.sleep(10);
			if(ltrd.ifrecv()){
				String[] recvdv = ltrd.getrecvdv();
				String source = ltrd.getsource();
				boolean ifalive;
				for(NeighborLink iter: neighborinfo){
					if(iter.getneighbor().equals(source)){
						for(int i = 0 ; i < recvdv.length / 3 ; i++){
							if(recvdv[3 * i + 1].equals("-1"))
								ifalive = false;
							else
								ifalive = true;
							iter.updatelink(new Link(recvdv[3 * i] , recvdv[3 * i + 1] , recvdv[3 * i + 2] , ifalive));
						}
				/*		if(ltrd.ifpoison()){
							System.out.println(ltrd.getsource() + " poison you to " + recvdv[1]);
							iter.setpoison(recvdv[1]);
							iter.updatelink(new Link(recvdv[1] , "-1" , "NULL" , true));
						}
				*/
						timer.settime(iter.getneighbor());
					}
				}
				
		//		System.out.println("Start update distance vector");
				linkinfo = dv.updatedv(linkinfo, neighborinfo);
				if(dv.getifupdate()){
		//			System.out.println("Distance vector updated");
					broadcast (neighborinfo , dv , localport);
			//		Broadcast broadcast = new Broadcast(neighborinfo , dv , localport);
			//		Thread brdcst = new Thread(broadcast);
			//		brdcst.start();
				}
			}
			if(ui.getifinput()){
				String[] tokens = ui.getinput();
				if (tokens.length == 0 || tokens[0].length() == 0)
				{
					continue;
				}
				
				int i;
				for(i = 0 ; i < cmd.length ; i++){
					if(cmd[i].equals(tokens[0].toUpperCase()))
						break;
				}

				switch (i)
				{
					case 0:{
						if(dolinkdown(tokens , linkinfo , neighborinfo) == -1)
							break;
						else{
				//		linkinfo = dv.updatedv(linkinfo, neighborinfo);
						broadcast (neighborinfo , dv , localport);
				//		Broadcast broadcast = new Broadcast(neighborinfo , dv , localport);
				//		Thread brdcst = new Thread(broadcast);
				//		brdcst.start();
						}
					}
						break;
					case 1:{
						if(dolinkup(tokens , linkinfo , neighborinfo) == -1)
							break;
						else{
				//		linkinfo = dv.updatedv(linkinfo, neighborinfo);
						broadcast (neighborinfo , dv , localport);
				//		Broadcast broadcast = new Broadcast(neighborinfo , dv , localport);
				//		Thread brdcst = new Thread(broadcast);
				//		brdcst.start();
						}
					}
						break;
					case 2:
						doshowrt(dv);
						break;
					case 3:
						doclose();
						break;
					default:
						System.out.println("Unknown command: " + tokens[0]);
						break;
				}
			}
			Thread.sleep(20);
			for(NeighborLink iter: neighborinfo){
				if(timer.gettimeout(iter.getneighbor())){
					broadcast (neighborinfo , dv , localport);
					timer.settimeout(iter.getneighbor() , false);
					timer.settime(iter.getneighbor());
				}
				if(timer.getdead(iter.getneighbor())){
					neighborinfo.remove(iter);
					for(Link iter2: linkinfo){
						if(iter2.getdest().equals(iter.getneighbor()))
							linkinfo.remove(linkinfo.indexOf(iter2));
					}
					System.out.println(iter.getneighbor() + " is shut down");
					linkinfo = dv.updatedv(linkinfo, neighborinfo);
					broadcast (neighborinfo , dv , localport);
				}
			}
			
		}
				
	}
		
	static int dolinkdown(String[] tokens , ArrayList<Link> linkinfo , ArrayList<NeighborLink> neighborinfo){
		System.out.println("Invoke LINKDOWN");
		if (tokens.length != 3)
		{
			System.out.println("Command format error.");
			return -1;
		}
		
		String down = tokens[1] + ": " + tokens[2];
		for(Link iter: linkinfo){
			if(iter.getdest().equals(down))
				iter.setalive(false);
		}
		for(NeighborLink iter: neighborinfo){
			if(iter.getneighbor().equals(down))
				iter.setalive(false);
		}
		System.out.println("link down success.");
		return 0;
		
	}
	
	static int dolinkup(String[] tokens , ArrayList<Link> linkinfo , ArrayList<NeighborLink> neighborinfo){
		System.out.println("Invoke LINKUP");
		if (tokens.length != 3)
		{
			System.out.println("Command format error.");
			return -1;
		}
		
		String up = tokens[1] + ": " + tokens[2];
		for(Link iter: linkinfo){
			if(iter.getdest().equals(up))
				iter.setalive(true);
		}
		for(NeighborLink iter: neighborinfo){
			if(iter.getneighbor().equals(up))
				iter.setalive(true);
		}
		System.out.println("link up success.");
		return 0;
	}
	
	static void doshowrt(DistanceVector dv){
		dv.showrt();
	}
	
	static void doclose(){
		System.exit(0);
	}
	
	static void broadcast(ArrayList<NeighborLink> neighborinfo , DistanceVector dv , int port) throws NumberFormatException, IOException{
		String sendstr = new String();
		byte[] data = new byte[500];
		ArrayList<String> dest = new ArrayList<String>();
		for(NeighborLink iter: neighborinfo){
			if(iter.getisalive())
				dest.add(iter.getneighbor());
		}
		sendstr = dv.getsenddv();
		DatagramSocket broadcast = new DatagramSocket(port + 10000);
		DatagramPacket sendpkt;
		UDPPacket send = new UDPPacket(sendstr.getBytes());
		for(String iter: dest){
			String[] split = iter.split(": ");
			byte[] senddata = send.addheader(InetAddress.getLocalHost().getHostAddress() , port, split[0], Integer.parseInt(split[1]));
			System.arraycopy(senddata, 0, data, 0, senddata.length);
			sendpkt = new DatagramPacket(data , data.length , InetAddress.getByName(split[0]) , Integer.parseInt(split[1]));
			broadcast.send(sendpkt);
		}
		broadcast.close();
	}
	
	static int getlport(){
		return localport;
	}
	
	

}

class Link{
	private String source;
	private String destination;
	private double weight;
	private String hop;
	private boolean isalive = true;

	public Link(String source , int sport , String dest , int port , double w , String hopip , int hopport , boolean isalive){
		this.source = source + ": " + sport;
		destination = dest + ": " + port;
		weight = w;
		hop = hopip + ": " + hopport;
		this.isalive = isalive;
	}
	
	public Link(String source , String dest , String weight , String hop , boolean isalive){
		this.source = source;
		this.destination = dest;
		this.weight = Double.parseDouble(weight);
		this.hop = hop;
		this.isalive = isalive;
	}
	
	public Link(String dest , String weight , String hop , boolean isalive){
		this.destination = dest;
		this.weight = Double.parseDouble(weight);
		this.hop = hop;
		this.isalive = isalive;
	}
	
	public Link(String dest , String weight){
		this.destination = dest;
		this.weight = Double.parseDouble(weight);
	}
	
	public String getdest(){
		return destination;
	}
	
	public String getweight(){
		return Double.toString(weight);
	}
	
	public String gethop(){
		return hop;
	}
	
	public String getsource(){
		return source;
	}
	
	public void setweight(Double w){
		this.weight = w;
	}
	
	public void setalive(boolean set){
		isalive = set;
	}
	
	public boolean getisalive(){
		return isalive;
	}
	
	public void sethop(String hop){
		this.hop = hop;
	}
}

class NeighborLink{
	private String neighborip;
	private int neighborport;
	private boolean isalive = true;
	ArrayList<Link> link = new ArrayList<Link>();
	ArrayList<String> poison = new ArrayList<String>();
	
	public NeighborLink(String ip , int port , Link tolocal){
		neighborip = ip;
		neighborport = port;
		link.add(tolocal);
	}
	
	public boolean ifexistdest(ArrayList<Link> link , Link l){
		boolean exist = false;
		for(Link iter: link){
			if(iter.getdest().equals(l.getdest()))
				exist = true;
		}
		return exist;
	}
	
	public int getindex(ArrayList<Link> link , Link l){
		int index = -1;
		for(Link iter: link){
			if(iter.getdest().equals(l.getdest()))
				index = link.indexOf(iter);
		}
		return index;
	}
	
	public void updatelink(Link l){
		
		if(!ifexistdest(link , l)){
			link.add(l);
		}
		else{
			link.set(getindex(link , l), l);
		}
	}
	
	public ArrayList<String> getalldest(){
		ArrayList<String> destset = new ArrayList<String>();
		for(Link iter: link){
			destset.add(iter.getdest());
		}
		return destset;
	}
	
	public double getweight(String dest){
		int i = 0;
		for(Link iter: link){
			if(iter.getisalive()){
				if(iter.getdest().equals(dest))
					break;
			}
			i++;
		}
		if(i == link.size())
			return -1;
		double weight = Double.parseDouble(link.get(i).getweight());
		return weight;
	}
	
	public String getneighbor(){
		return neighborip + ": " + neighborport;
	}
	
	public String getneighborip(){
		return neighborip;
	}
	
	public int getneighborport(){
		return neighborport;
	}
	
	public void setalive(boolean set){
		isalive = set;
	}
	
	public boolean getisalive(){
		return isalive;
	}
	
	public void setpoison(String dest){
		poison.add(dest);
	}
	
	public void cancelpoison(String dest){
		poison.remove(dest);
	}
	
	public ArrayList<String> getpoisonlist(){
		return poison;
	}
}

class WeightInfo{
	private String source;
	private double weight;
	
	public WeightInfo(String s , double w){
		source = s;
		weight = w;
	}
	
	public String getsource(){
		return source;
	}
	
	public double getweight(){
		return weight;
	}
}
class DistanceVector{
	
	ArrayList<Link> l = new ArrayList<Link>();
	private ArrayList<String> dv = new ArrayList<String>();
	private String senddv = new String();
	private boolean ifupdate;
	private ArrayList<Link> neighbor = new ArrayList<Link>();
	
	public DistanceVector(ArrayList<Link> neighbor){
		this.neighbor = neighbor;
	}
	
	
	public void createdv(ArrayList<Link> linkinfo){
		l = linkinfo;
		for(Link iter: linkinfo){
			if(iter.getisalive())
				dv.add("Destination = " + iter.getdest() + ", Cost = " + iter.getweight() + ", Link = (" +  iter.gethop() + ")");				
		}
	}
	
	public String getsenddv(){
		senddv = new String();
		for(Link iter: l){
			if(iter.getisalive())
				senddv = senddv + iter.getdest() + "  " + iter.getweight() + "  " + iter.gethop() + "  ";
			else
				senddv = senddv + iter.getdest() + "  " + "-1" + "  " + "NULL" + "  ";
		}
		return senddv;
	}
	
	public void showrt(){
		Date time = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String ctime = dateFormat.format(time); 
		System.out.println("<" + ctime + "> Distance vector list is:");
		for(String iter: dv){
			System.out.println(iter);
		}
	}
	
	public ArrayList<Link> updatedv(ArrayList<Link> linkinfo , ArrayList<NeighborLink> neighborinfo) throws NumberFormatException, IOException{
		boolean existdest;
		ifupdate = false;
		ArrayList<String> predv = new ArrayList<String>();
		for(String iter: dv){
			predv.add(iter);
		}
		ArrayList<WeightInfo> weightinfo = new ArrayList<WeightInfo>();
		ifupdate = false;
		
		//add new destination to this client
		
		for(NeighborLink iter: neighborinfo){
			ArrayList<String> destset = iter.getalldest();
			for(String iter2: destset){
				existdest = false;
				for(Link iter3: linkinfo){
					if(iter3.getdest().equals(iter2)){
						existdest = true;
						break;
					}
				}
				if(!existdest){
					linkinfo.add(new Link(linkinfo.get(0).getsource() , iter2 , "-1" , "NULL" , true));
				}
			}
		}
		
		//delete unavailable destination
		
	/*	for(Link iter: linkinfo){
			boolean exist = false;
			for(NeighborLink iter2: neighborinfo){
				ArrayList<String> destset = iter2.getalldest();
				for(String iter3: destset){
					if(iter.getdest().equals(iter3)){
						exist = true;
						break;
					}
				}
			}
			if(!exist)
				linkinfo.remove(iter);
		}
		*/
		
		//update distancevector
		
		for(Link iter: linkinfo){
			
		//for each destination in this client, create a arraylist to store the weight to that destination from each neighbors 
			
			weightinfo.clear();
			for(NeighborLink iter2: neighborinfo){
				if(iter.getdest().equals(iter2.getneighbor()))
					
		//if this destination is this neighbor, set the weighinfo 0
					
					weightinfo.add(new WeightInfo(iter2.getneighbor(), 0));
				else if(iter2.getpoisonlist().contains(iter.getdest()))
					
		//if this destination is poisoned by this neighbor, set the weighinfo -1
					
					weightinfo.add(new WeightInfo(iter2.getneighbor(), -1));
				else
					
		//the weightinfo is the weight from this neighbor to this destination
					
					weightinfo.add(new WeightInfo(iter2.getneighbor(), iter2.getweight(iter.getdest())));
			}
			
		//for each neighbor, compare the direct weight from client to destination with the weight using that neighbor as a hop
			
			for(WeightInfo iter2: weightinfo){
				
				boolean broken = false;
				
		//if link to this neighbor is broken, continue
				
				if(findweight(linkinfo , iter2.getsource()) == 0)
					broken = true;
				if(broken)
				//	iter.setweight(findweight(linkinfo , iter2.getsource()) + iter2.getweight());
					continue;
				
				//if weightinfo.getweight() = -1, it means this neighbor has no route to this destination at this moment, then we should continue
				
				if(iter2.getweight() == -1)
					continue;
				if(ifneedupdate(iter , iter2 , neighbor) || ifhopbroken(weightinfo , iter)){
					iter.setweight(findweight(neighbor , iter2.getsource()) + iter2.getweight());
					if(iter.getdest().equals(iter2.getsource()))
						iter.sethop(linkinfo.get(0).getdest());
					else{
						iter.sethop(iter2.getsource());
						unicast(iter.gethop() , iter.getdest() , bfclient.getlport());
					}
					System.out.println("Link Update: " + "Destination = " + iter.getdest() + ", Cost = " + iter.getweight() + ", Link = (" +  iter.gethop() + ")");
				}
			}
		}
		dv.clear();
		for(Link iter: linkinfo){
		//	if(iter.getisalive())
				dv.add("Destination = " + iter.getdest() + ", Cost = " + iter.getweight() + ", Link = (" +  iter.gethop() + ")");
		}
		
		if(predv.size() == dv.size()){
			for(String iter: predv){
				if(!dv.get(predv.indexOf(iter)).equals(iter))
					ifupdate = true;
			}
		}
		else
			ifupdate = true;
		
		
		return linkinfo;
		
	}
	
	public boolean ifneedupdate(Link l , WeightInfo w , ArrayList<Link> neighbor){
		if(Double.parseDouble(l.getweight()) == -1)
			return true;
		if(Double.parseDouble(l.getweight()) > findweight(neighbor , w.getsource()) + w.getweight())
			return true;
		if(l.getsource().equals(l.gethop()) && !l.getisalive())
			return true;
		return false;
	}
	
	public boolean ifhopbroken(ArrayList<WeightInfo> w, Link l){
		for(WeightInfo iter: w){
			if(iter.getsource().equals(l.gethop()) && iter.getweight() == -1)
				return true;
		}
		return false;
	}
	
	public double findweight(ArrayList<Link> linkinfo , String dest){
		for(Link iter: linkinfo){
			if(iter.getdest().equals(dest) && iter.getisalive())
				return Double.parseDouble(iter.getweight());
		}
		return 0;
	}
	
	public boolean getifupdate(){
		return ifupdate;
	}
	
	public ArrayList<String> getdv(){
		return dv;
	}
	
	public void unicast(String neighbor , String destination , int port) throws NumberFormatException, IOException{
		String sendstr = "poision" + "  " + destination;
		byte[] data = new byte[500];
		DatagramSocket unicast = new DatagramSocket(port + 5000);
		DatagramPacket sendpkt;
		UDPPacket send = new UDPPacket(sendstr.getBytes());
		String[] split = neighbor.split(": ");
		byte[] senddata = send.addheader(InetAddress.getLocalHost().getHostAddress() , port, split[0], Integer.parseInt(split[1]));
		System.arraycopy(senddata, 0, data, 0, senddata.length);
		sendpkt = new DatagramPacket(data , data.length , InetAddress.getByName(split[0]) , Integer.parseInt(split[1]));
		unicast.send(sendpkt);
		unicast.close();

	}
}

class ListenThread implements Runnable{
	DatagramSocket listen;
	DatagramPacket recv;
	boolean newdv = false;
	boolean poison = false;
	String from;
	String[] recvdv;
	byte[] recvpkt = new byte[1000];
	int lport;
			
	public ListenThread(int port){
		lport = port;
	}
	
	public void run() {
		try {
			listen = new DatagramSocket(lport);
			recv = new DatagramPacket(recvpkt , 0 , recvpkt.length);
			while(true){
				newdv = false;
				poison = false;
				listen.receive(recv);
				UDPPacket recvdata = new UDPPacket(recvpkt);
				if(!recvdata.checkcorrupt())
					continue;
				else{
					newdv = true;
					from = recvdata.getsource();
					recvdv = recvdata.deliver();
					if(recvdv[0].equals("poision"))
						poison = true;
			//		System.out.println("Receive distance vector from: " + from);
				}
				newdv = true;
				from = recvdata.getsource();
				recvdv = recvdata.deliver();
				Thread.sleep(40);
				
			}
			
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean ifrecv(){
		return newdv;
	}
	
	public String getsource(){
		return from;
	}
	
	public String[] getrecvdv(){
		return recvdv;
	}
	
	public boolean ifpoison(){
		return poison;
	}
}

class UDPPacket{
	private byte[] data;
	
	public UDPPacket(byte[] p){
		data = p;
	}
	
	public UDPPacket(){
	}
	
	public int length(){
		return data.length;
	}
	
	public byte[] addheader(String srcip , int srcport , String dstip , int dstport){
		byte[] udpheader = new byte[14];
		byte[] udppacket = new byte[500];
		byte[] sourceip = new byte[4];
		byte[] destip = new byte[4];
		byte[] sourceport = new byte[2];
		byte[] destinationport = new byte[2];
		byte[] checksum = new byte[2];
		
		
		sourceport = inttobyte(srcport , 2);
		destinationport = inttobyte(dstport , 2);
		sourceip = new UDPPacket().iptransform(srcip);
		destip = new UDPPacket().iptransform(dstip);

		System.arraycopy(sourceip, 0, udpheader, 0, 4);
		System.arraycopy(destip, 0, udpheader, 4, 4);
		System.arraycopy(sourceport, 0, udpheader, 8, 2);
		System.arraycopy(destinationport, 0, udpheader, 10, 2);
		System.arraycopy(checksum, 0, udpheader, 12, 2);
		System.arraycopy(udpheader, 0, udppacket, 0, 14);
		System.arraycopy(data, 0, udppacket, 14, data.length);
		
		checksum = checksum(udppacket);
		System.arraycopy(checksum, 0, udppacket, 12, 2);
		
		return udppacket;

	}
	
	public byte[] iptransform(String ip){
		byte[] result = new byte[4];
	//	String[] ipsplit = new String[4];
		String[] ipsplit = ip.split("\\.");
		for(int i = 0 ; i < ipsplit.length ; i++){
			int a = Integer.parseInt(ipsplit[i]);
			result[i] = (byte)(a & 0xff);
		}
		return result;
	}
	
	public byte[] checksum(byte[] d){
		byte[] result = new byte[2];
		String checksum = "0000000000000000";
		for(int i = 0 ; i < d.length ; i = i+2){
			String m = bytetobstr(d[i]) + bytetobstr(d[i+1]);
			checksum  = add(checksum , m);
		}
		checksum = Integer.toBinaryString(Integer.valueOf(checksum, 2) ^ 65535);
		if(checksum.length() < 16){
			for(int i = checksum.length() ; i < 16 ; i++){
				checksum = "0" + checksum;
			}
		}
		result[0] = Integer.valueOf(checksum.substring(0, 8), 2).byteValue();
		result[1] = Integer.valueOf(checksum.substring(8), 2).byteValue();
		
		return result;
	}
	
	public String[] deliver(){
		byte[] recvdata = new byte[data.length - 14];
		System.arraycopy(data, 14, recvdata, 0, data.length - 14);
		String recvstr = new String(recvdata);
		String[] split = recvstr.split("  ");
		String[] recvdv = split;
		return recvdv;
	}
	
	public boolean checkcorrupt(){
		String result = "0000000000000000";
		for(int i = 0 ; i < data.length ; i = i+2){
			String x = bytetobstr(data[i]) + bytetobstr(data[i+1]);
			result = add(result , x);
		}
		if(result.equals("1111111111111111")){
			return true;
		}
		else
			return false;
	}
	
	public String getsrcip(){
		String source = Integer.toString((data[0] & 0xff)) + "." + Integer.toString((data[1] & 0xff)) + "." + Integer.toString((data[2] & 0xff)) + "." + Integer.toString((data[3] & 0xff)) ;
		return source;
	}
	
	public String getdestip(){
		String dest = Integer.toString((data[4] & 0xff)) + "." + Integer.toString((data[5] & 0xff)) + "." + Integer.toString((data[6] & 0xff)) + "." + Integer.toString((data[7] & 0xff)) ;
		return dest;
	}
	
	public String getsource(){
		String sourceip = Integer.toString((data[0] & 0xff)) + "." + Integer.toString((data[1] & 0xff)) + "." + Integer.toString((data[2] & 0xff)) + "." + Integer.toString((data[3] & 0xff));
		String sourceport = bytetobstr(data[8]) + bytetobstr(data[9]);
		sourceport = String.valueOf(Integer.valueOf(sourceport , 2));
		String source = sourceip + ": " + sourceport;
		return source;
	}
	
	public String getdest(){
		String destip = Integer.toString((data[0] & 0xff)) + "." + Integer.toString((data[1] & 0xff)) + "." + Integer.toString((data[2] & 0xff)) + "." + Integer.toString((data[3] & 0xff));
		String destport = bytetobstr(data[8]) + bytetobstr(data[9]);
		destport = String.valueOf(Integer.valueOf(destport , 2));
		String dest = destip + ": " + destport;
		return dest;
	}
	
	public String getsrcport(){
		String source = bytetobstr(data[8]) + bytetobstr(data[9]);
		source = String.valueOf(Integer.valueOf(source , 2));
		return source;
	}
	
	public String getdestport(){
		String dest = bytetobstr(data[10]) + bytetobstr(data[11]);
		dest = String.valueOf(Integer.valueOf(dest , 2));
		return dest;
	}
	
	public byte[] inttobyte(int integer , int bytesize){
		byte[] a = new byte[bytesize];
		String intstr = Integer.toBinaryString(integer);
		if(intstr.length() < bytesize * 8){
			for(int i = intstr.length() ; i < bytesize * 8 ; i++){
				intstr = "0" + intstr;
			}
		}
		for (int i = 0; i < bytesize - 1; i++){  
			a[i] = Integer.valueOf(intstr.substring(8 * i, 8 * i + 8), 2).byteValue();     
		}
		a[bytesize - 1] = Integer.valueOf(intstr.substring(8 * (bytesize - 1)), 2).byteValue();
		return a;
	}
	
	public String bytetobstr(byte b){
		String ZERO = "00000000";
		String s = Integer.toBinaryString(b);
        if (s.length() > 8) {
            s = s.substring(s.length() - 8);
        } else if (s.length() < 8) {
            s = ZERO.substring(s.length()) + s;
        }
        return s;
            
	}
	
	private String add(String x , String y){
		int x1 = Integer.valueOf(x,2);
		int y1 = Integer.valueOf(y,2);
		int z = x1 + y1;
		String result = Integer.toBinaryString(z);
		if(result.length() < 16){
			for(int i = result.length() ; i < 16 ; i++){
				result = "0" + result;
			}
			return result;
		}
		if(result.length() == 16)
			return result;
		if(result.length() > x.length() && result.substring(0 , 1).equals("1")){
			result = Integer.toBinaryString(Integer.valueOf(result.substring(1) , 2) + 1);
		}
		if(result.length() < 16){
			for(int i = result.length() ; i < 16 ; i++){
				result = "0" + result;
			}
		}
		return result;

	}
}


class UserInterface implements Runnable{
	
	private boolean ifinput;
	String[] tokens;
	
	
	public void run(){
		Scanner sc = new Scanner(System.in);
		System.out.println("Welcome to bfclient!");

		while (true)
		{
			ifinput = false;
			String userInput = sc.nextLine().trim();
			ifinput = true;
			tokens = userInput.split(" ");
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}	
	}
	
	
	public boolean getifinput(){
		return ifinput;
	}
	
	public String[] getinput(){
		return tokens;
	}
}

class Broadcast implements Runnable{
	DatagramSocket broadcast;
	DatagramPacket sendpkt;
	int port;
	String sendstr = new String();
	byte[] data = new byte[500];
	ArrayList<String> dest = new ArrayList<String>();
	
	public Broadcast(ArrayList<NeighborLink> neighborinfo , DistanceVector dv , int port){
		for(NeighborLink iter: neighborinfo){
			if(iter.getisalive())
				dest.add(iter.getneighbor());
		}
		sendstr = dv.getsenddv();
		this.port = port;
	}
	
	public void run(){
		try {
			broadcast = new DatagramSocket(port + 10000);
			UDPPacket send = new UDPPacket(sendstr.getBytes());
			for(String iter: dest){
				String[] split = iter.split(": ");
				byte[] senddata = send.addheader(InetAddress.getLocalHost().getHostAddress() , port, split[0], Integer.parseInt(split[1]));
				System.arraycopy(senddata, 0, data, 0, senddata.length);
				sendpkt = new DatagramPacket(data , data.length , InetAddress.getByName(split[0]) , Integer.parseInt(split[1]));
				broadcast.send(sendpkt);
			}
			
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			broadcast.close();
		}
	}
}

class Timer implements Runnable{
	private ArrayList<TimerInfo> timer = new ArrayList<TimerInfo>();
	private int timeout;
	
	public Timer(ArrayList<NeighborLink> neighbor , int timeout){
		for(NeighborLink iter: neighbor){
			timer.add(new TimerInfo(iter.getneighbor() , new Date().getTime()));
		}
		this.timeout = timeout;
	}
	
	public void run(){
		while(true){
			for(TimerInfo iter: timer){
				if(new Date().getTime() - iter.gettime() > timeout * 1000){
					iter.settimeout(true);
				}
				if(new Date().getTime() - iter.gettime() > timeout * 3000)
					iter.setdead(true);
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public boolean gettimeout(String neighbor){
		for(TimerInfo iter: timer){
			if(iter.getneighbor().equals(neighbor))
				return iter.gettimeout();
		}
		return false;
	}
	
	public void settimeout(String neighbor , boolean timeout){
		for(TimerInfo iter: timer){
			if(iter.getneighbor().equals(neighbor))
				iter.settimeout(false);
		}
	}
	
	public boolean getdead(String neighbor){
		for(TimerInfo iter: timer){
			if(iter.getneighbor().equals(neighbor))
				return iter.getdead();
		}
		return false;
	}
	
	public void settime(String neighbor){
		for(TimerInfo iter: timer){
			if(iter.getneighbor().equals(neighbor))
				iter.settime(new Date().getTime());
		}
	}
	
}

class TimerInfo{
	private String neighbor;
	private long time;
	private boolean senddv = false;
	private boolean dead = false;
	
	public TimerInfo(String neighbor , long time){
		this.neighbor = neighbor;
		this.time = time;
	}
	
	public long gettime(){
		return time;
	}
	
	public void settime(long time){
		this.time = time;
	}
	
	public String getneighbor(){
		return neighbor;
	}
	
	public void settimeout(boolean timeout){
		this.senddv = timeout;
	}
	
	public boolean gettimeout(){
		return senddv;
	}
	
	public void setdead(boolean dead){
		this.dead = dead;
	}
	
	public boolean getdead(){
		return dead;
	}
}










