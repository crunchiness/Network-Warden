package com.example.tcp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HashTable {
    Map<String, String> Port_PID;
    Map<String, Integer> Port_Time;
    String localIP;
    Process process;              //lsof process
    DataOutputStream os;          //input stream of lsof process
    InputStreamReader ir;         //output stream of lsof process
    BufferedReader input;         //read output stream of lsof process
    int lastcheck;                //time of the last check of table
    boolean ready;                //if lsof get permission, the value is true

    String thisserver;            //server ip of this packet
    String lastserver;            //server ip of last packet
    int thisquerytime;            //the time of calling GetApp()
    int lastquerytime;            //last time of calling GetApp()
    String lastresult;            //result of last query

    public HashTable() throws IOException {  //initialization

        thisserver = "";
        lastserver = "";
        lastresult = "";
        thisquerytime = 1000;
        lastquerytime = 0;
        lastcheck = 0;

        Port_PID = new HashMap<String, String>();
        Port_Time = new HashMap<String, Integer>();
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
            System.out.println("lsof get permission");
            MainActivity.ShowMsg("lsof get permission");
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
    public String GetAPP(Packet newpacket) {
        int Time = newpacket.GetTimeSec();

        //check table every 30s
        if (Time - lastcheck > 30) {
            CheckTable(Time);
        }

        String inf;         //key of table
        String localPort;


        //key = localip + localport + serverip + serverport + protocol
        if (newpacket.SrcIP.equals(localIP)) {
            localPort = newpacket.SrcPort;
            thisserver = newpacket.DestIP + newpacket.DestPort;
            inf = newpacket.SrcIP + newpacket.SrcPort + newpacket.DestIP + newpacket.DestPort + newpacket.Protocol;
        } else if (newpacket.DestIP.equals(localIP)) {
            localPort = newpacket.DestPort;
            thisserver = newpacket.SrcIP + newpacket.DestPort;
            inf = newpacket.DestIP + newpacket.DestPort + newpacket.SrcIP + newpacket.SrcPort + newpacket.Protocol;
        } else {
            System.out.println("IP wrong!");
            return "IP WRONG";
        }
        //inf = localPort;
        //inf = newpacket.SrcIP+newpacket.SrcPort+newpacket.DestIP + newpacket.DestPort + new.protocol;

        if (Port_PID.containsKey(inf)) {
            return Port_PID.get(inf);
        } else {
            String packetTime = newpacket.Time.replace('.', ':');
            String ts[] = packetTime.split(":");
            int sec = Integer.parseInt(ts[2]);
            int ms = Integer.parseInt(ts[3].substring(0, 3));
            thisquerytime = sec * 1000 + ms;

            //if the time difference between this query and last query is less than 30ms, and
            //their server ips are the same, directly return the result of last query
            if (thisquerytime < lastquerytime) thisquerytime += 60000;
            if (thisquerytime - lastquerytime < 50 && thisserver.equals(lastserver)) {
                Port_PID.put(inf, lastresult);
                Port_Time.put(inf, Time);
                lastquerytime = thisquerytime;
                return lastresult;
            }

            try {
                //run lsof
                String commands = "/data/local/lsof +c 0 -i:" + localPort + " 2>/dev/null \n";
                os.writeBytes(commands);
                os.flush();

                String line;

                int i = 0;
                String Name_PID = "";
                long tt1 = System.currentTimeMillis();
                while (true) {
                    if (!input.ready()) {
                        if (System.currentTimeMillis() - tt1 > 1000) {  //which means lsof dont get any result
                            String er = "unknow";
                            Port_PID.put(inf, er);
                            Port_Time.put(inf, Time);
                            //System.out.println(er);
                            return er;

                        } else continue;
                    }
                    line = input.readLine();
                    if (i == 0) {
                        //System.out.println(line);
                        i++;
                        continue;
                    } else if (i == 1) {
                        //System.out.println(line);
                        i++;
                        String[] temp = line.split(" ");
                        Name_PID = temp[0] + " " + temp[1];
                        Port_PID.put(inf, Name_PID);
                        Port_Time.put(inf, Time);
                        //System.out.println("got name and pid: "+Name_PID);
                    }
                    if (!input.ready()) {
                        lastquerytime = thisquerytime;
                        lastserver = thisserver;
                        lastresult = Name_PID;
                        return Name_PID;
                    }

                    //return null;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                System.out.println(" lsof wrong");
                //MainActivity.ShowMsg("lsof wrong");
                e.printStackTrace();
                return null;
            }
        }

    }

    //check table, remove entries which haven't used in last 30s.
    public void CheckTable(int Time) {
        Set<String> key = Port_PID.keySet();
        Set<String> r = new HashSet<String>();
        for (String k : key) {
            if (Time - Port_Time.get(k) > 30) {
                r.add(k);
            }
        }

        for (String k : r) {
            Port_PID.remove(k);
            Port_Time.remove(k);
        }
        lastcheck = Time;
    }
}
