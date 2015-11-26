package warden;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.Button;

public class RunTCP extends Thread {

    Process process;         // process running tcpdump
    boolean running;         // a flag. when it turns to "false", terminate tcpdump
    HashTable hashTable;        // table
    InputStreamReader ir;    // output stream of tcpdump  process
    BufferedReader input;    // read output stream of tcpdump  process
    DataOutputStream os;     // input stream of tcpdump  process
    boolean ready;           // if tcpdump and lsof get permission, the value is true
    FileWriter fw;           // write record
    BufferedWriter bw;       // record writer
    static String localIP;

    public RunTCP() {
        try {
            hashTable = new HashTable();

            // get the superuser permission
            String commands = "su";
            process = Runtime.getRuntime().exec(commands);

            ir = new InputStreamReader(process.getInputStream());
            input = new BufferedReader(ir);
            os = new DataOutputStream(process.getOutputStream());

            // check the user
            os.writeBytes("id\n");
            String line;
            boolean permission = false;
            while ((line = input.readLine()) != null) {
                if (line.contains("root")) {
                    permission = true;
                }
                if (!input.ready()) {
                    break;
                }
            }

            if (permission) {
                System.out.println("tcpdump got permission");
                MainActivity.ShowMsg("tcpdump got permission");
                ready = hashTable.ready;
            } else {
                System.out.println("tcpdump cannot get permission");
                MainActivity.ShowMsg("tcpdump cannot get permission");
                ready = false;
            }
            localIP = hashTable.localIP;

        } catch (IOException e1) {
            System.out.println("permission denied");
            MainActivity.ShowMsg("permission denied");
            e1.printStackTrace();
        }
    }

    public void run() {
        running = true;     // a flag. when it turns to "false", terminate tcpdump

        try {
            Packet initialPackets[] = new Packet[200]; // existing connections
            int numberOfPackets = 0; // number of existing connections

            // run lsof to get existing connections
            os.writeBytes("data/local/lsof +c 0 -i 2>/dev/null | grep -E 'TCP|UDP'\n");
            os.flush();

            // regex to parse packet
            //                               0       1         2         3         4         5      6        7           8       9     10
//                                           app                                 IPv4              TCP       src    srcport    dst    dstport
            Pattern tcp = Pattern.compile("([^ ]) +([0-9]+) +([0-9]+) +([^ ]+) +([^ ]+) +([0-9]+) +([^ ]+) +([^:]+):([0-9]+)->([^:]+):([0-9]+)");
            //[0].android.chrome [1]31012    [2]10051  [3]146u  IPv4 493928       TCP 192.168.0.17:33806->62.254.123.65:80 (CLOSE_WAIT)

            long t = System.currentTimeMillis();
            boolean firstLine = true;
            while (true) {
                if (!input.ready()) {
                    if (System.currentTimeMillis() - t > 2000)   // if lsof returns nothing
                        break;
                } else {
                    String line = input.readLine();
                    Matcher matcher = tcp.matcher(line);
                    if (!matcher.matches()) {
                        // TODO save this mismatch
                    }

                    // TODO remove?
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }

                    Packet newPacket = new Packet();      //make SYN packet for the connection

                    newPacket.protocol = matcher.group(6);
                    newPacket.srcIP = matcher.group(7);
                    newPacket.srcPort = matcher.group(8);
                    newPacket.length = "0";
                    newPacket.timeStamp = " ";
                    newPacket.TCPflags = "S " + matcher.group(0) + " " + matcher.group(1);
                    initialPackets[numberOfPackets++] = newPacket;
                    System.out.println(newPacket.toString());

                    if (!input.ready()) {
                        break;
                    }
                }
            }

            System.out.println("Starting tcpdump");

            // run tcpdump
            os.writeBytes("/data/local/tcpdump -v -n tcp\n");
            String line;


/*

    88.221.134.226.80 > 192.168.0.17.47836: Flags [.], cksum 0x1610 (correct), ack 556, win 488, options [nop,nop,TS val 1828728669 ecr 163128], length 0
13:33:40.633956 IP (tos 0x0, ttl 58, id 27515, offset 0, flags [DF], proto TCP (6), length 342)
    88.221.134.226.80 > 192.168.0.17.47836: Flags [P.], cksum 0x937e (correct), seq 1:291, ack 556, win 488, options [nop,nop,TS val 1828728669 ecr 163128], length 290: HTTP, length: 290
	HTTP/1.1 304 Not Modified
	Content-Type: application/javascript
	Last-Modified: Wed, 30 Sep 2015 10:51:44 GMT
	ETag: 34bd4a6455a2d6f5c858e3afb4a009fe
	Cache-Control: public, max-age=424
	Expires: Mon, 23 Nov 2015 13:40:44 GMT
	Date: Mon, 23 Nov 2015 13:33:40 GMT
	Connection: keep-alive

13:33:40.634088 IP (tos 0x0, ttl 64, id 32747, offset 0, flags [DF], proto TCP (6), length 52)
    192.168.0.17.47836 > 88.221.134.226.80: Flags [.], cksum 0xa09f (incorrect -> 0x15de), ack 291, win 245, options [nop,nop,TS val 163131 ecr 1828728669], length 0
13:33:40.765841 IP (tos 0x0, ttl 64, id 8423, offset 0, flags [DF], proto TCP (6), length 455)
    192.168.0.17.54289 > 54.230.196.57.80: Flags [P.], cksum 0x6f03 (correct), seq 492:895, ack 33684, win 1315, options [nop,nop,TS val 163144 ecr 2293450274], length 403: HTTP, length: 403
	GET /pub/cx/v2.8.15-1-59dedc7/cx.js HTTP/1.1
	Host: cdn.beanstock.com
	Connection: keep-alive
	Accept: *\/*
            User-Agent: Mozilla/5.0 (Linux; Android 4.4.2; XT1032 Build/KLB20.9-1.10-1.24-1.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.76 Mobile Safari/537.36
            Referer: http://www.last.fm/user/crunchiness
            Accept-Encoding: gzip, deflate, sdch
            Accept-Language: en-GB,en-US;q=0.8,en;q=0.6


*/



            System.out.println("tcpdump started");

            // first line of a packet
            Pattern timePattern = Pattern.compile("[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]+ ");
            Packet currentPacket = new Packet();
            while (running) {
                if (input.ready()) {
                    line = input.readLine();
                    if (line == null) {
                        continue;
                    }

                    final Packet newpacket = new Packet();
//                    newpacket.ReadPacket(line);

                    Matcher timeMatcher = timePattern.matcher(line);
                    if (timeMatcher.find()) {
                        // finish old packet and start new

                        String inf = newpacket.destIP + newpacket.destPort + newpacket.srcIP + newpacket.srcPort + newpacket.protocol;

//                        String detectedApp = hashTable.getApp(newPacket);

                        String packetStr = currentPacket.toString();
                        bw.write(packetStr + "\nboba\n");
                        bw.flush();
                        currentPacket = new Packet();
                    } else {
                        currentPacket.addLine(line);
                    }
//
//                    // wtf
//                    if (isFirstPacket) {
//                        isFirstPacket = false;
//                        // fake the SYN packets of existing connections
//                        for (int i = 0; i < numberOfPackets; i++) {
//                            bw.write(newPacket.timeStamp + initialPackets[i].toString() + ";");
//                            bw.flush();
//                        }
//                    }

//                    // Skip packets without source IP
//                    if (newPacket.srcIP.equals(" ")) {
//                        continue;
//                    }

//                    // process non-SYN packet
//                    if (newPacket.protocol.equals("TCP") && !newPacket.TCPflags.contains("S")) {
//                        bw.write(newPacket.toString() + ";");
//                        bw.flush();
//                        continue;
//                    }

//                    String detectedApp = hashTable.getApp(newPacket);

                    // Only writes packets of recognized apps
//                    if (!detectedApp.equals("IP WRONG")) {
//                        detectedApp = newPacket.toString() + " " + detectedApp;
//                        bw.write(detectedApp + ";");
//                        bw.flush();
//                    }

                } else {
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

    public void destroyTCP() throws IOException {
        running = false;
        closeFile();
//        postprocess();
        process.destroy();
    }

    //open log1.txt
    public void openFile() throws IOException {
        File folder = new File("/data/local/Warden");
        if (!folder.exists() || !folder.isDirectory()) {
            folder.mkdir();
        }
        File writefile = new File("/data/local/Warden/log1.txt");

        try {
            if (writefile.exists()) {
                writefile.delete();
            }
            writefile.createNewFile();
            System.out.println("log file created");
            //MainActivity.ShowMsg("log file created");
        } catch (Exception e) {
            System.out.println("failed to create file");
            //MainActivity.ShowMsg("failed to create file");
        }
        Button buttonStart = findViewById(R.id.button1);

        fw = new FileWriter("/data/local/Warden/log1.txt", true);
        bw = new BufferedWriter(fw);
    }

    private Button findViewById(int button1) {
        // TODO Auto-generated method stub
        return null;
    }

    //close log1.txt and get it readable
    public void closeFile() throws IOException {
        bw.close();
        fw.close();

        Runtime.getRuntime().exec("chmod 777 /data/local/Warden/log1.txt\n");
    }


    public String readFile(String filename) {
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
            for (int i = 0; i < h.length - 1; i++) {
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


    public String getProtocol(String msg) {
        String[] s = msg.split(" ");
        int p = -1;
        for (int i = 0; i < s.length; i++) {
            if (s[i].equals("proto")) {
                p = i;
                break;
            }
        }
        if (p == -1) return "null";
        else return s[p + 1];
    }

    //post-process. find associated process for non-SYN packet. Result is log.txt
    public static void postprocess() throws IOException {
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
        } catch (IOException e) {
            System.out.print("Fail to read file");
            e.printStackTrace();
        }

        String temp[] = content.split(";");
        System.out.println(temp.length);
        InputStream in = null;
        BufferedReader reader = null;

        File outputf = new File(outputfile);
        if (outputf.exists()) {
            outputf.delete();
        }
        outputf.createNewFile();

        FileWriter fw;
        BufferedWriter bw;
        fw = new FileWriter(outputfile, true);
        bw = new BufferedWriter(fw);

        Map<String, String> portproc = new HashMap<String, String>();

        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            int cc = 0;

            for (int z = 0; z < temp.length - 1; z++) {
                tempString = temp[z];
                line++;

                String inf[] = tempString.split(" +");
                String port;
                String proc;
                if (inf[1].equals(localIP)) port = inf[2];
                else port = inf[4];

                if (inf[6].equals("UDP")) {
                    bw.write(tempString + "\n");
                    bw.flush();
                } else if (inf.length > 8) {
                    proc = inf[8];
                    portproc.put(port, proc);
                    bw.write(tempString + "\n");
                    bw.flush();
                } else {
                    if (portproc.containsKey(port)) {
                        proc = portproc.get(port);
                    } else {
                        proc = "cannot find syn packet";
                    }
                    bw.write(tempString + " " + proc + "\n");
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
