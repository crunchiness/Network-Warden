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

import android.widget.Button;

public class RunTCP extends Thread {

    private enum Line {
        PRIMARY, SECONDARY, TERTIARY, EMPTY
    }

    Process process;         // process running tcpdump
    boolean running;         // a flag. when it turns to "false", terminate tcpdump
    HashTable hashTable;        // table
    InputStreamReader ir;    // output stream of tcpdump  process
    BufferedReader input;    // read output stream of tcpdump  process
    DataOutputStream os;     // input stream of tcpdump  process
    boolean ready;           // if tcpdump and lsof get permission, the value is true
    BufferedWriter bw;       // record writer
    BufferedWriter errorOutput;
    BufferedWriter failOutput;
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
                ready = hashTable.ready;
            } else {
                System.out.println("tcpdump cannot get permission");
                ready = false;
            }
            localIP = hashTable.localIP;

        } catch (IOException e1) {
            System.out.println("permission denied");
            e1.printStackTrace();
        }
    }

    /**
     * Determines what kind of line this is
     * @param line line
     * @return enum with the kind
     */
    private static Line lineIs(String line) {
        if (line.length() == 0) {
            return Line.EMPTY;
        } else if (line.charAt(0) == ' ') {
            return Line.SECONDARY;
        } else if (line.charAt(0) == '\t') {
            return Line.TERTIARY;
        }
        return Line.PRIMARY;
    }

    private static void finishPacket(HashTable ht, Packet packet, BufferedWriter output, BufferedWriter failOutput) throws IOException {
        if (packet.isIgnore()) {
            failOutput.write(packet.toString());
        } else {
            String detectedApp = ht.getApp(packet);
            output.write(packet.toString());
        }
    }

    public void run() {
        running = true;     // a flag. when it turns to "false", terminate tcpdump

        try {
            int maxConnections = 200;
            Packet[] initialPackets = new Packet[maxConnections]; // existing connections
            int i = 0;

            // run lsof to get existing connections

            os.writeBytes("data/local/lsof +c 0 -i 2>/dev/null | grep -E 'TCP|UDP'\n");
            os.flush();

            long t = System.currentTimeMillis();
            boolean firstLine = true;

            while (true) {
                if (!input.ready()) {
                    if (System.currentTimeMillis() - t > 2000) {  //if lsof return nothing
                        break;
                    }
                } else {
                    String line = input.readLine();
                    System.out.println(line);
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    line = line.replace(':', ' ');
                    line = line.replace('-', ' ');
                    line = line.replace('>', ' ');

                    String inf[] = line.split(" +");

                    String protocol = inf[6];
                    String srcIp = inf[7];
                    String srcPort = inf[8];
                    String dstIp = (inf.length > 10) ? inf[9] : "*";
                    String dstPort = (inf.length > 10) ? inf[10] : "*";
                    String length = "0";
                    String time = " ";
                    String tcpFlags = "S " + inf[0] + " " + inf[1];

                    // make SYN packet for the connection
                    Packet pkt = new Packet(protocol, srcIp, srcPort, dstIp, dstPort, length, time, tcpFlags);

                    initialPackets[i++] = pkt;

                    if (!input.ready() || i == maxConnections) {
                        break;
                    }
                }
            }

            System.out.println("Starting tcpdump");

            // run tcpdump
            os.writeBytes("/data/local/tcpdump -v -n tcp\n");
            String line;

            System.out.println("tcpdump started");

            Packet pkt = new Packet();

            while (running) {
                if (input.ready()) {
                    line = input.readLine();
                    if (line == null) {
                        continue;
                    }
                    switch (lineIs(line)) {
                        case PRIMARY:
                            if (pkt.isInitialized()) {
                                finishPacket(hashTable, pkt, bw, failOutput);
                            }
                            pkt = new Packet(line);
                            break;
                        case SECONDARY:
                            if (pkt.isInitialized()) {
                                pkt.parseSecondaryLine(line);
                            } else {
                                errorOutput.write("ERR1 " + line + "\n");
                            }
                            break;
                        case TERTIARY:
                            pkt.parseTertiaryLine(line);
                            break;
                        default:
                            errorOutput.write("ERR2 " + line + "\n");
                    }
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
        postprocess();
        process.destroy();
    }

    //open log1.txt
    public void openFiles() throws IOException {
        File folder = new File("/data/local/Warden");
        if (!folder.exists() || !folder.isDirectory()) {
            folder.mkdir();
        }
        File writeFile = new File("/data/local/Warden/log1.txt");
        File errorFile = new File("/data/local/Warden/error.txt");
        File failFile = new File("/data/local/Warden/fail.txt");

        try {
            if (writeFile.exists()) {
                writeFile.delete();
            }
            if (errorFile.exists()) {
                errorFile.delete();
            }
            if (failFile.exists()) {
                failFile.delete();
            }
            writeFile.createNewFile();
            errorFile.createNewFile();
            failFile.createNewFile();
            System.out.println("log file created");
        } catch (Exception e) {
            System.out.println("failed to create file");
        }

        Button buttonStart = findViewById(R.id.button1);

        bw = new BufferedWriter(new FileWriter("/data/local/Warden/log1.txt", true));
        errorOutput = new BufferedWriter(new FileWriter("/data/local/Warden/error.txt", true));
        failOutput = new BufferedWriter(new FileWriter("/data/local/Warden/fail.txt", true));
    }

    private Button findViewById(int button1) {
        // TODO Auto-generated method stub
        return null;
    }

    //close log1.txt and get it readable
    public void closeFile() throws IOException {
        bw.close();
        errorOutput.close();
        failOutput.close();

        Runtime.getRuntime().exec("chmod 777 /data/local/Warden/log1.txt\n");
    }

    // post-process. find associated process for non-SYN packet. Result is log.txt
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

        Map<String, String> portproc = new HashMap<>();

        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString;

            for (int z = 0; z < temp.length - 1; z++) {
                tempString = temp[z];

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
