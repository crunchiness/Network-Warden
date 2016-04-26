package network.warden;

import android.util.Pair;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HashTable {
    Map<String, Pair<String,Double>> portMap;
    String localIP;
    Process process;              //lsof process
    DataOutputStream os;          //input stream of lsof process
    InputStreamReader ir;         //output stream of lsof process
    BufferedReader input;         //read output stream of lsof process
    double lastCheck;                //timeStamp of the last check of table
    boolean ready;                //if lsof get permission, the value is true

    String thisServer;            //server ip of this packet
    String lastServer;            //server ip of last packet
    double thisQueryTime;            //the timeStamp of calling GetApp()
    double lastQueryTime;            //last timeStamp of calling GetApp()
    String lastResult;            //result of last query

    public HashTable() throws IOException {  //initialization

        thisServer = "";
        lastServer = "";
        lastResult = "";
        thisQueryTime = 1000;
        lastQueryTime = 0;
        lastCheck = 0;

        portMap = new HashMap<>();
        process = Runtime.getRuntime().exec("su");        //get root permission

        os = new DataOutputStream(process.getOutputStream());
        ir = new InputStreamReader(process.getInputStream());
        input = new BufferedReader(ir);

        //check root permission
        os.writeBytes("id\n");
        String line;
        boolean permission = false;
        while ((line = input.readLine()) != null) {
            if (line.contains("root")) permission = true;
            if (!input.ready()) break;
        }
        if (permission) {
            System.out.println("got permission");
            ready = true;
        } else {
            System.out.println("cannot get permission");
            ready = false;
        }

        //get local ip address of wlan
        os.writeBytes("ifconfig wlan0\n");
        line = input.readLine();
        localIP = line.split(" ")[2];
        System.out.println("local IP is: " + localIP);
    }


    //return associated process name of a packet
    public String getApp(Packet newpacket) {


        double time = newpacket.getTimeSec();

        //check table every 30s
        if (time - lastCheck > 30) {
            CheckTable(time);
        }

        String key = null;         // key of table
        String localPort = null;


        // key = localip + localport + serverip + serverport + protocol

        if (newpacket.getSrcIP().equals(localIP)) {
            localPort = newpacket.getSrcPort();
            thisServer = newpacket.getDstIP() + newpacket.getDstPort();
            key = newpacket.getSrcIP() + newpacket.getSrcPort() + newpacket.getDstIP() + newpacket.getDstPort() + newpacket.getProtocol();
        } else if (newpacket.getDstIP().equals(localIP)) {
            localPort = newpacket.getDstPort();
            thisServer = newpacket.getSrcIP() + newpacket.getDstPort();
            key = newpacket.getDstIP() + newpacket.getDstPort() + newpacket.getSrcIP() + newpacket.getSrcPort() + newpacket.getProtocol();
        }

        if (portMap.containsKey(key)) {
            return portMap.get(key).first;
        } else {
            thisQueryTime = newpacket.getTimeSec();

            // if the timeStamp difference between this query and last query is less than 50ms, and
            // their server ips are the same, directly return the result of last query
            if (thisQueryTime - lastQueryTime < 0.050 && thisServer.equals(lastServer)) {
                portMap.put(key, new Pair<>(lastResult, time));
                lastQueryTime = thisQueryTime;
                return lastResult;
            }

            try {
                // run lsof
                String commands = "/data/local/lsof +c 0 -i:" + localPort + " 2>/dev/null \n";
                System.out.println(commands);
                os.writeBytes(commands);
                os.flush();

                int i = 0;
                String Name_PID = "";
                long tt1 = System.currentTimeMillis();
                while (true) {
                    if (input.ready()) {
                        String line = input.readLine();
                        if (i == 1) {
                            String[] temp = line.split(" ");
                            Name_PID = temp[0] + " " + temp[1];
                            portMap.put(key, new Pair<>(Name_PID, time));
                        }
                        if (!input.ready()) {
                            lastQueryTime = thisQueryTime;
                            lastServer = thisServer;
                            lastResult = Name_PID;
                            return Name_PID;
                        }
                        i++;
                    } else {
                        if (System.currentTimeMillis() - tt1 > 1000) {  // which means lsof didn't get any result
                            String er = "unknown";
                            portMap.put(key, new Pair<>(er, time));
                            return er;
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("lsof wrong");
                e.printStackTrace();
                return null;
            }
        }

    }

    // check table, remove entries which haven't used in last 30s
    public void CheckTable(double time) {
        Set<String> keys = new HashSet<>();
        keys.addAll(portMap.keySet());
        for (String key : keys) {
            if (time - portMap.get(key).second > 30) {
                portMap.remove(key);
            }
        }
        lastCheck = time;
    }
}
