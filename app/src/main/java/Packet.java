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
    private String tcpFLAGS;
    private String tcpCKSUM;
    private String tcpSEQ;
    private String tcpACK;
    private String tcpWIN;
    private String tcpOPTIONS;
    private String length;
    private String other;
    private String tcpPROTOCOL;
    private String tcpLENGTH;

    public Packet(String line) throws IP6Exception, BadInputException {
        this.makePacket(line);
    }

    public Packet() {

    }

    /**
     * Makes a packet from IP line
     *
     * @param line IP line string
     * @throws IP6Exception
     * @throws BadInputException
     */
    private void makePacket(String line) throws IP6Exception, BadInputException {
        // 18:03:58.938770 IP (tos 0x0, ttl 108, id 4844, offset 0, flags [none], proto UDP (17), length 48)
        Pattern timePattern = Pattern.compile("([0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]+) IP(.).?\\((.*)\\)");
        Matcher matcher = timePattern.matcher(line);
        if (matcher.matches()) {
            if (matcher.group(2).charAt(0) == ' ') {
                timeStamp = matcher.group(1);
                String params = matcher.group(3);
                parseIPParams(params);
            } else if (matcher.group(2).charAt(0) == '6') {
                throw new IP6Exception();
            } else {
                throw new BadInputException("Couldn't read past 'IP'.");
            }
        } else {
            throw new BadInputException("Line does not begin with time stamp.");
        }
    }

    private void parseIPParams(String params) throws BadInputException {
        // tos 0x0, ttl 64, id 5999, offset 0, flags [DF], proto TCP (6), length 575
        Pattern p = Pattern.compile("tos ([^,]*), ttl ([0-9]+), id ([0-9]+), offset ([0-9]+), flags \\[(.*)\\], proto ([^ ]*) \\([0-9]+\\), length ([0-9]+)");
        Matcher m = p.matcher(params);
        if (!m.matches()) {
            throw new BadInputException("Failed to read IP params.");
        }
        ipTOS = m.group(1);
        ipTTL = m.group(2);
        ipID = m.group(3);
        ipOFFSET = m.group(4);
        ipFLAGS = m.group(5);
        ipPROTOCOL = m.group(6);
        ipLENGTH = m.group(7);
    }

    public boolean isInitialized() {
        return timeStamp != null;
    }

    public void parseSecondaryLine(String line) throws BadInputException {
        switch (ipPROTOCOL) {
            case "TCP":
                parseTCPLine(line);
                break;
            case "UDP":
                parseUDPLine(line);
                break;
            default:
                throw new IllegalStateException("Protocol must have been set to TCP or UDP but it wasn't.");
        }
    }

    private void parseTCPLine(String line) throws BadInputException {
        //     41.249.197.208.63145 > 192.168.  0. 20.4852: Flags [P.], cksum 0xacdf (correct), seq 246:284, ack 276, win 229, options [nop,nop,TS val 1454965 ecr 6631861], length 38
        //    192.168.  0. 20.53887 >  91.234.200.114.  80: Flags [P.], cksum 0xc015 (correct), seq 1:1080,  ack 1,   win 229, length 1079: HTTP, length: 1079
        Pattern[] tcpPatterns = {
                Pattern.compile("    ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+) > ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+): Flags \\[([^]]*)\\], cksum [^ ]* \\((correct|incorrect[^)]*)\\), seq ([0-9:]+), ack ([0-9]+), win ([0-9]+), length ([0-9]+)"),
                Pattern.compile("    ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+) > ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+): Flags \\[([^]]*)\\], cksum [^ ]* \\((correct|incorrect[^)]*)\\), seq ([0-9:]+), ack ([0-9]+), win ([0-9]+), options \\[([^]]*)\\], length ([0-9]+)"),
                Pattern.compile("    ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+) > ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+): Flags \\[([^]]*)\\], cksum [^ ]* \\((correct|incorrect[^)]*)\\), seq ([0-9:]+), ack ([0-9]+), win ([0-9]+), length ([0-9]+): ([A-Z]+), length: ([0-9]+)"),
                Pattern.compile("    ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+) > ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+): Flags \\[([^]]*)\\], cksum [^ ]* \\((correct|incorrect[^)]*)\\), seq ([0-9:]+), ack ([0-9]+), win ([0-9]+), options \\[([^]]*)\\], length ([0-9]+): ([A-Z]+), length: ([0-9]+)")
        };

        int matched = -1;
        Matcher m = null;
        for (int i = 0; i < tcpPatterns.length; i++) {
            m = tcpPatterns[i].matcher(line);
            if (m.matches()) {
                matched = i;
                break;
            }
        }

        if (matched == -1) {
            throw new BadInputException("Failed to parse TCP line.");
        }

        srcIP = m.group(1);
        srcPort = m.group(2);
        dstIP = m.group(3);
        dstPort = m.group(4);
        tcpFLAGS = m.group(5);
        tcpCKSUM = m.group(6);
        tcpSEQ = m.group(7);
        tcpACK = m.group(8);
        tcpWIN = m.group(9);
        length = m.group(11);
        if (matched == 1 || matched == 3) {
            tcpOPTIONS = m.group(10);
        }
        if (matched == 2) {
            tcpPROTOCOL = m.group(11);
            tcpLENGTH = m.group(12);
        }
        if (matched == 3) {
            tcpPROTOCOL = m.group(12);
            tcpLENGTH = m.group(13);
        }
    }

    private void parseUDPLine(String line) throws BadInputException {
        Pattern udpPattern = Pattern.compile("    ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+) > ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+): UDP, length ([0-9]+)");
        Matcher m = udpPattern.matcher(line);
        if (!m.matches()) {
            throw new BadInputException("Failed to parse UDP line.");
        }
        srcIP = m.group(1);
        srcPort = m.group(2);
        dstIP = m.group(3);
        dstPort = m.group(4);
        length = m.group(5);
    }

    public void parseTertiaryLine(String line) {
        other += line + "\n";
    }
}
