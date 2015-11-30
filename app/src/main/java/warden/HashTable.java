package warden;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HashTable {
    Map<String, String> Port_PID;
    Map<String, Double> Port_Time;
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

        Port_PID = new HashMap<>();
        Port_Time = new HashMap<>();
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
            System.out.println("lsof got permission");
            MainActivity.ShowMsg("lsof got permission");
            ready = true;
        } else {
            System.out.println("lsof cannot get permission");
            MainActivity.ShowMsg("lsof cannot get permission");
            ready = false;
        }

        //get local ip address of wlan
        os.writeBytes("ifconfig wlan0\n");
        line = input.readLine();
        localIP = line.split(" ")[2];
        System.out.println("local IP is: " + localIP);
        MainActivity.ShowMsg("local IP is: " + localIP);
    }


    //return associated process name of a packet
    public String getApp(Packet newpacket) {


        double Time = newpacket.getTimeSec();

        //check table every 30s
        if (Time - lastCheck > 30) {
            CheckTable(Time);
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

        if (Port_PID.containsKey(key)) {
            return Port_PID.get(key);
        } else {
            thisQueryTime = newpacket.getTimeSec();

            // if the timeStamp difference between this query and last query is less than 50ms, and
            // their server ips are the same, directly return the result of last query
            if (thisQueryTime < lastQueryTime) thisQueryTime += 60000; // TODO: wtf
            if (thisQueryTime - lastQueryTime < 0.050 && thisServer.equals(lastServer)) {
                Port_PID.put(key, lastResult);
                Port_Time.put(key, Time);
                lastQueryTime = thisQueryTime;
                return lastResult;
            }

            try {
                // run lsof
                String commands = "/data/local/lsof +c 0 -i:" + localPort + " 2>/dev/null \n";
                os.writeBytes(commands);
                os.flush();

                int i = 0;
                String Name_PID = "";
                long tt1 = System.currentTimeMillis();
                while (true) {
                    if (!input.ready()) {
                        if (System.currentTimeMillis() - tt1 > 1000) {  //which means lsof dont get any result
                            String er = "unknown";
                            Port_PID.put(key, er);
                            Port_Time.put(key, Time);
                            return er;

                        } else {
                            continue;
                        }
                    }

                    if (i == 0) {
                        i++;
                        continue;
                    } else if (i == 1) {
                        i++;
                        String[] temp = input.readLine().split(" ");
                        Name_PID = temp[0] + " " + temp[1];
                        Port_PID.put(key, Name_PID);
                        Port_Time.put(key, Time);
                    }

                    if (!input.ready()) {
                        lastQueryTime = thisQueryTime;
                        lastServer = thisServer;
                        lastResult = Name_PID;
                        return Name_PID;
                    }
                }
            } catch (IOException e) {
                System.out.println(" lsof wrong");
                e.printStackTrace();
                return null;
            }
        }

    }

    //check table, remove entries which haven't used in last 30s.
    public void CheckTable(double Time) {
        Set<String> key = Port_PID.keySet();
        Set<String> r = new HashSet<>();
        for (String k : key) {
            if (Time - Port_Time.get(k) > 30) {
                r.add(k);
            }
        }

        for (String k : r) {
            Port_PID.remove(k);
            Port_Time.remove(k);
        }
        lastCheck = Time;
    }
}
