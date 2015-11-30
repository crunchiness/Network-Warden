package network.warden;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Packet {

    private String timeStamp;
    private String ipTOS;
    private String ipTTL;
    private String ipID;
    private String ipOFFSET;
    private String ipFLAGS;
    private String ipPROTOCOL;
    private String ipLENGTH;
    private String srcIP;
    private String srcPort;
    private String dstIP;
    private String dstPort;
    private String other = "";

    private final HashMap<String, String> values = new HashMap<>();
    private boolean ignore = false;
    private String ipLine;
    private String secondaryLine;
    private String appName;

    public Packet(String line) {
        makePacket(line);
    }

    public Packet(String protocol, String srcIp, String srcPort, String dstIp, String dstPort, String length, String time, String tcpFlags) {
        makePacket(protocol, srcIp, srcPort, dstIp, dstPort, length, time, tcpFlags);
    }

    public Packet() {

    }

    /**
     * Makes a packet from IP line
     *
     * @param line IP line string
     */
    private void makePacket(String line) {
        ipLine = line;
        // 18:03:58.938770 IP (tos 0x0, ttl 108, id 4844, offset 0, flags [none], proto UDP (17), length 48)
        Pattern timePattern = Pattern.compile("([0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]+) IP(.).?\\((.*)\\)");
        Matcher matcher = timePattern.matcher(line);
        if (matcher.matches() && matcher.group(2).charAt(0) == ' ') {
            timeStamp = matcher.group(1);
            String params = matcher.group(3);
            parseIPParams(params);
        } else {
            ignore = true;
        }
    }


    private void makePacket(String protocol, String srcIp, String srcPort, String dstIp, String dstPort, String length, String time, String tcpFlags) {
        // TODO
    }

    private void parseIPParams(String params) {
        // tos 0x0, ttl 64, id 5999, offset 0, flags [DF], proto TCP (6), length 575
        Pattern p = Pattern.compile("tos ([^,]*), ttl ([0-9]+), id ([0-9]+), offset ([0-9]+), flags \\[(.*)\\], proto ([^ ]*) \\([0-9]+\\), length ([0-9]+)");
        Matcher m = p.matcher(params);
        if (m.matches()) {
            ipTOS = m.group(1);
            ipTTL = m.group(2);
            ipID = m.group(3);
            ipOFFSET = m.group(4);
            ipFLAGS = m.group(5);
            ipPROTOCOL = m.group(6);
            ipLENGTH = m.group(7);
        } else {
            ignore = true;
        }
    }

    public boolean isInitialized() {
        return (timeStamp != null) || ignore;
    }

    public void parseSecondaryLine(String line) {
        if (ignore) {
            secondaryLine = line;
        } else {
            switch (ipPROTOCOL) {
                case "TCP":
                    parseTCPLine(line);
                    break;
                case "UDP":
                    parseUDPLine(line);
                    break;
                default:
                    ignore = true;
                    secondaryLine = line;
            }
        }
    }

    private void parseTCPLine(String line) {
        //      2. 84. 36.148.51197 > 192.168.  0. 20. 6881: Flags [.],  cksum 0x3c43 (correct), ack 4121294785, win 30706, options [nop,nop,TS val 151940689 ecr 6631215], length 0
        //    103. 47.133. 65.37679 > 192.168.  0. 20.34965: Flags [R.], cksum 0x8082 (correct), seq 0,       ack 182279039, win 0, length 0
        //     41.249.197.208.63145 > 192.168.  0. 20. 4852: Flags [P.], cksum 0xacdf (correct), seq 246:284, ack 276,       win 229, options [nop,nop,TS val 1454965 ecr 6631861], length 38
        //    192.168.  0. 20.53887 >  91.234.200.114.   80: Flags [P.], cksum 0xc015 (correct), seq 1:1080,  ack 1,         win 229, length 1079: HTTP, length: 1079

        Pattern addresses = Pattern.compile("    ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+) > ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+):.*");

        // Parse addresses and ports
        Matcher addressMatcher = addresses.matcher(line);
        if (!addressMatcher.matches()) {
            ignore = true;
            secondaryLine = line;
            return;
        }

        srcIP = addressMatcher.group(1);
        srcPort = addressMatcher.group(2);
        dstIP = addressMatcher.group(3);
        dstPort = addressMatcher.group(4);

        HashMap<String, Pattern> patterns = new HashMap<>();
        patterns.put("flags", Pattern.compile("Flags \\[([^]]*)\\]"));
        patterns.put("cksum", Pattern.compile("cksum [0-9a-fx]+ (correct|incorrect(?:.*))"));
        patterns.put("seq", Pattern.compile("seq ([0-9:]+)"));
        patterns.put("ack", Pattern.compile("ack ([0-9]+)"));
        patterns.put("win", Pattern.compile("win ([0-9]+)"));
        patterns.put("options", Pattern.compile("options \\[(.*)\\]"));
        patterns.put("tcpProtocol", Pattern.compile(": ([A-Z]+),"));
        patterns.put("tcpLength", Pattern.compile("length: ([0-9]+)$"));


        // Parse everything else
        for (Map.Entry<String, Pattern> entry: patterns.entrySet()) {
            String key = entry.getKey();
            Pattern p = entry.getValue();
            Matcher m = p.matcher(line);
            if (m.find()) {
                values.put(key, m.group(1));
            } else {
                values.put(key, "");
            }
        }
    }

    private void parseUDPLine(String line) {
        Pattern addresses = Pattern.compile("    ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+) > ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+): UDP, length ([0-9]+)");
        Matcher m = addresses.matcher(line);
        if (m.matches()) {
            srcIP = m.group(1);
            srcPort = m.group(2);
            dstIP = m.group(3);
            dstPort = m.group(4);
            values.put("udpLength", m.group(5));
        } else {
            ignore = true;
            secondaryLine = line;
        }
    }

    public void parseTertiaryLine(String line) {
        other += line + "\n";
    }

    public boolean isIgnore() {
        return ignore;
    }

    public String toString() {
        if (ignore) {
            String str = "";
            if (ipLine != null) {
                str += "I: " + ipLine;
                str += (str.charAt(str.length()-1) == '\n') ? "" : "\n";
            }
            if (secondaryLine != null) {
                str += "S: " + secondaryLine;
                str += (str.charAt(str.length()-1) == '\n') ? "" : "\n";
            }
            if (other.length() > 0) {
                str += "O: " + other;
                str += (str.charAt(str.length()-1) == '\n') ? "" : "\n";
            }
            return str;
        } else {
            // schema
            // ip                                      tcp
            // protocol;tos;ttl;id;offset;flags;length;flags;cksum;seq;ack;win;options;tcpProtocol;tcpLength
            // ip                                      udp
            // protocol;tos;ttl;id;offset;flags;length;udpLength
            String packet = String.format("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;", appName, ipPROTOCOL, timeStamp, ipTOS, ipTTL, ipID, ipOFFSET, ipFLAGS, ipLENGTH, srcIP, srcPort, dstIP, dstPort);
            for (String value : values.values()) {
                packet += value + ";";
            }
            return packet.substring(0, packet.length() - 1) + "\n";
        }
    }

    public boolean isTCP() {
        return ipPROTOCOL.equals("TCP");
    }

    public double getTimeSec() {
        // 18:03:58.938770
        Pattern p = Pattern.compile("([0-9]{2}):([0-9]{2}):([0-9]{2})\\.([0-9]+)");
        Matcher matcher = p.matcher(timeStamp);
        if (matcher.matches()) {
            double h = Double.parseDouble(matcher.group(1));
            double m = Double.parseDouble(matcher.group(2));
            double s = Double.parseDouble(matcher.group(3));
            double etc = Double.parseDouble("0." + matcher.group(4));
            return ((h * 60 + m) * 60 + s) + etc;
        } else {
            return 0;
        }
    }

    public String getSrcIP() {
        return srcIP;
    }

    public String getSrcPort() {
        return srcPort;
    }

    public String getDstIP() {
        return dstIP;
    }

    public String getDstPort() {
        return dstPort;
    }

    public String getProtocol() {
        return ipPROTOCOL;
    }

    public void setApp(String appName) {
        this.appName = appName;
    }
}
