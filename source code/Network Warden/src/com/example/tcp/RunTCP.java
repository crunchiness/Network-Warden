package com.example.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;

import android.util.Log;
import android.widget.Button;

public class RunTCP extends Thread{
	
	Process process;         //process running tcpdump
	boolean running;         //a flag. when it turns to "false", terminate tcpdump
	HashTable myhash;        //table 
	InputStreamReader ir;    //output stream of tcpdump  process
	BufferedReader input;    //read output stream of tcpdump  process
	DataOutputStream os;     //input stream of tcpdump  process
	boolean ready;           //if tcpdump and lsof get permission, the value is true
	FileWriter fw;           //write record
	BufferedWriter bw;       //record writer
	static String localIP;   
	
	public RunTCP(){
		try {
			myhash = new HashTable();    
			
			//get the superuser permission
			String commands = "su";
			process = Runtime.getRuntime().exec(commands);
			
			ir=new InputStreamReader(process.getInputStream());
			input = new BufferedReader (ir);		
			os = new DataOutputStream(process.getOutputStream());
			
			//check the user
			os.writeBytes("id\n");
			String line;
		    boolean permission = false;
			while((line = input.readLine()) != null){
				if (line.contains("root")) permission = true;
				if(!input.ready()) break;
			}
			if(permission){
				System.out.println("tcpdump got permission");
				MainActivity.ShowMsg("tcpdump got permission");
				if(myhash.ready){
					ready = true;
				}
				else ready = false;
			}
			else {
				System.out.println("tcpdump cannot get permission");
				MainActivity.ShowMsg("tcpdump cannot get permission");
				ready = false;
			}
			localIP = myhash.localIP;
			
		} catch (IOException e1) {
			System.out.println("permission deny");
			MainActivity.ShowMsg("permission deny");
			e1.printStackTrace();
		}
	}
	
	public void run(){
		running = true;     //a flag. when it turns to "false", terminate tcpdump
		
		try {
				Packet initpackets[] = new Packet[200];   //existing connections
				int numpac = 0;                           //number of existing connections
				
				//run lsof to get existing connections
				os.writeBytes("/data/local/lsof +c 0 -i TCP 2>/dev/null\n");
				os.flush();
				
				//System.out.println("adsfdgawg");
				long t2 = System.currentTimeMillis();
				boolean firstline = true;
				while(true){
					if(!input.ready()){
						if (System.currentTimeMillis() - t2 > 2000)   //if lsof return nothing
						break;	
					}	
					else{
						String line = input.readLine();
						System.out.println(line);
						if (firstline){
							firstline = false;
							continue;
						}
						line = line.replace(':', ' ');
						line = line.replace('-', ' ');
						line = line.replace('>', ' ');
						
						String inf[] = line.split(" +");
						Packet newpacket = new Packet();      //make SYN packet for the connection
						
						newpacket.Protocol = inf[6];
						newpacket.SrcIP = inf[7];
						newpacket.SrcPort = inf[8];
						if(inf.length > 10){
							newpacket.DestIP = inf[9];
							newpacket.DestPort = inf[10];
						}else{
							newpacket.DestIP = "*";
							newpacket.DestPort = "*";
						}
						newpacket.length = "0";
						newpacket.Time = " ";
						newpacket.TCPflags = "S "+inf[0]+" "+inf[1];
						
						initpackets[numpac++] = newpacket;
						
						System.out.println(newpacket.ToString());
						
						if(!input.ready()) break;
					}
				}		
				
			//System.out.println(numpac);
			System.out.println("Starting tcpdump");
			//MainActivity.ShowMsg("Starting tcpdump...");
			
			//runtcpdump
			os.writeBytes("/data/local/tcpdump -v -n -s 0 tcp\n");
			String line;

			//discard first several packets
			/*
			while(true){
				if(input.ready()){
					line = input.readLine ();
					//System.out.println(line);
					if (line == null) continue;
					String packetTime = line.split(" ")[0];
					if(packetTime.charAt(0) > '9' || packetTime.charAt(0) < '0') continue;
					long t1 = ((long) System.currentTimeMillis()) % (1000*60);
					packetTime = packetTime.replace('.', ':');
					int sec = Integer.parseInt(packetTime.split(":")[2]);
					int ms  = Integer.parseInt(packetTime.split(":")[3].substring(0, 3));
					t2 = sec*1000 + ms;
					
					if(t1 < t2) t1 += 60 * 1000;
					
					if(t1 - t2 > 100) continue;
					else break;
				}
				
			}
			*/
			System.out.println("tcpdump started");
			//MainActivity.ShowMsg("tcpdump started");
			
			boolean isfirstpacket = true;
			int lastpacket = 0; 
			String lastserver = "";
			while (running){	
				if(input.ready()){
					line = input.readLine ();
					
					if (line == null) continue;
					
						final Packet newpacket = new Packet();  
						newpacket.ReadPacket(line);
						
						if(isfirstpacket){
							isfirstpacket = false;
							
							//write the SYN packets of existing connections
							for(int i = 0; i < numpac;i++){
								bw.write(newpacket.Time + initpackets[i].ToString() + ";");
								bw.flush();
							}
						}

						if (newpacket.SrcIP.equals(" ")) continue;
						
						//process non-SYN packet
						if (newpacket.Protocol.equals("TCP") && !newpacket.TCPflags.contains("S")){
							bw.write(newpacket.ToString() + ";");
							bw.flush();
							continue;
						}
						
						
						
						//String temp = newpacket.ToString();
						String temp = myhash.GetAPP(newpacket);
						if(temp.equals("IP WRONG")) continue;  
						else temp = newpacket.ToString() + " " + temp;
						
		                bw.write(temp+";");
		                bw.flush();			
						
					//}
					
				}
				else
				{
					//System.out.println("nonoonono");
				}
					       
			}	
					
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("tcp wrong");
			//MainActivity.ShowMsg("tcp wrong");
			e.printStackTrace();
		}
		
	}
	
	public void DestroyTCP() throws IOException{
		running = false;
		CloseFile();
		postprocess();
		process.destroy();
	}
	
	//open log1.txt
	public void OpenFile() throws IOException{
		File folder = new File("/data/local/Warden");
		if (!folder.exists() || !folder.isDirectory()){
			folder.mkdir();
		}
		File writefile = new File("/data/local/Warden/log1.txt");	
		
		try{
			if(writefile.exists()){
				writefile.delete();
			}
			writefile.createNewFile();
			System.out.println("log file created");
			//MainActivity.ShowMsg("log file created");
		}catch(Exception e){
			System.out.println("failed to create file");
			//MainActivity.ShowMsg("failed to create file");
		}
		Button buttonStart = (Button) findViewById(R.id.button1);
		
		fw = new FileWriter("/data/local/Warden/log1.txt", true);
		bw = new BufferedWriter(fw);
	}
	private Button findViewById(int button1) {
		// TODO Auto-generated method stub
		return null;
	}

	//close log1.txt and get it readable
	public void CloseFile() throws IOException{
		bw.close();
		fw.close();
		
		Runtime.getRuntime().exec("chmod 777 /data/local/Warden/log1.txt \n");
	}
	

	public String readFile(String filename)
	{
		System.out.println(filename);
	   String content = null;
	   File file = new File(filename); //for ex foo.txt
	   if (file.exists()) System.out.println("exist");
	   try {
	       FileReader reader = new FileReader(file);
	       char[] chars = new char[(int) file.length()];
	       reader.read(chars);
	       content = new String(chars);
	       //System.out.println(content);
	       String h[] = content.split(";");
	       for(int i = 0;i < h.length-1;i++){
	    	   System.out.println(h[i]);
	       }
	       System.out.println(h.length);
	       reader.close();
	   } catch (IOException e) {
		   System.out.print("agawfwfa");
	       e.printStackTrace();
	   }
	   return content;
	}
	

	public String GetProtocol(String msg){
		String [] s = msg.split(" ");
		int p = -1;
		for(int i = 0;i < s.length;i++){
			if (s[i].equals("proto")){
				p = i;
				break;
			}
		}
		if(p == -1)	return "null";
		else return s[p+1];
	}
	
    //post-process. find associated process for non-SYN packet. Result is log.txt
	public static void postprocess() throws IOException{
		String filename = "/data/local/Warden/log1.txt";
		String outputfile = "/data/local/Warden/log.txt";
		
		String content = null;
		   File file = new File(filename); //for ex foo.txt
		   //if (file.exists()) System.out.println("exist");
		   try {
		       FileReader reader = new FileReader(file);
		       char[] chars = new char[(int) file.length()];
		       reader.read(chars);
		       content = new String(chars);
		       //System.out.println(content);
		       reader.close();
		   }catch (IOException e) {
			   System.out.print("Fail to read file");
		       e.printStackTrace();
		   }
		   
		   String temp[] = content.split(";");
		   System.out.println(temp.length);
	    InputStream in = null;
	    BufferedReader reader = null;
	    
	    File outputf = new File(outputfile);
	    if(outputf.exists()){
	    	outputf.delete();
	    }
	    outputf.createNewFile();
	    
	    FileWriter fw;
		BufferedWriter bw;
		fw = new FileWriter(outputfile, true);
		bw = new BufferedWriter(fw);

	    Map <String, String> portproc = new HashMap<String, String>(); 
	    
	    try {
	        reader = new BufferedReader(new FileReader(file));
	        String tempString = null;
	        int line = 1;
	        int cc = 0;

	        for(int z = 0; z < temp.length-1;z++){
	        	tempString = temp[z];
	            line++;
	            
	            String inf[] = tempString.split(" +");
	            String port;
	            String proc;
	            if(inf[1].equals(localIP)) port = inf[2];
	            else port = inf[4];
	            
	            if(inf[6].equals("UDP")){
	            	bw.write(tempString+"\n");
	            	bw.flush();
	            	continue;
	            }
	            else if(inf.length > 8){
	            	proc = inf[8];
	            	portproc.put(port, proc);
	            	bw.write(tempString+"\n");
	            	bw.flush();
	            }
	            else{
	            	if(portproc.containsKey(port)){
	            		proc = portproc.get(port);
	            	}else{
	            		proc = "cannot find syn packet";
	            	}
	            	bw.write(tempString + " " + proc+"\n");
	            	bw.flush();
	            }
	        }
	        reader.close();
	        
	        bw.close();
	        fw.close();
	        //System.out.println("Amount:"+line+"  "+"failed:"+cc+"  "+"fail rate:" + (double)cc/line);
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        if (reader != null) {
	            try {
	                reader.close();
	            } catch (IOException e1) {
	            }
	        }
	    }
	}
	
	
}




/*
String Procs[] = {"facebook", "mediaserver"};
String ProcID[] = new String [10];
int numproc = 0;

Packet initpackets[] = new Packet[100];
int numpac = 0;

for(int i = 0; i < Procs.length;i++){
	String proc = Procs[i];
	os.writeBytes("ps | grep " + proc +"\n");
	System.out.println();
	os.flush();
	
	long t1 = System.currentTimeMillis();
	while(true){
		if(!input.ready()){
			if (System.currentTimeMillis() - t1 > 300)
			break;	
		}					
		else{
			String line = input.readLine();
			ProcID[numproc] = line.split(" +")[1];
			numproc++;		
			if(!input.ready()) break;
		}
	}
}
for(int i = 0; i < numproc;i++){
	os.writeBytes("/data/local/lsof -a -p "+ProcID[i]+" 2>/dev/null | grep TCP\n");
	System.out.println("/data/local/lsof -a -p "+ProcID[i]+" 2>/dev/null | grep TCP");
	os.flush();
	
	long t2 = System.currentTimeMillis();
	while(true){
		if(!input.ready()){
			if (System.currentTimeMillis() - t2 > 300)
			break;	
		}	
		else{
			String line = input.readLine();
			line = line.replace(':', ' ');
			line = line.replace('-', ' ');
			line = line.replace('>', ' ');
			
			String inf[] = line.split(" +");
			Packet newpacket = new Packet();
			
			newpacket.Protocol = inf[6];
			newpacket.SrcIP = inf[7];
			newpacket.SrcPort = inf[8];
			newpacket.DestIP = inf[9];
			newpacket.DestPort = inf[10];
			newpacket.length = "0";
			newpacket.Time = " ";
			newpacket.TCPflags = "S "+inf[0]+" "+inf[1];
			
			initpackets[numpac++] = newpacket;
			
			System.out.println(newpacket.ToString());
			
			if(!input.ready()) break;
		}
	}
	if(!input.ready()) break;
}
*/
