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
                System.out.println(timeStamp);
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
        this.ipTOS = m.group(1);
        this.ipTTL = m.group(2);
        this.ipID = m.group(3);
        this.ipOFFSET = m.group(4);
        this.ipFLAGS = m.group(5);
        this.ipPROTOCOL = m.group(6);
        this.ipLENGTH = m.group(7);
    }

    public boolean isInitialized() {
        System.out.println(timeStamp);
        return timeStamp != null;
    }

    public void parseSecondaryLine(String line) throws BadInputException {
//    41.249.197.208.63145 > 192.168.0.20.4852: Flags [P.], cksum 0xacdf (correct), seq 246:284, ack 276, win 229, options [nop,nop,TS val 1454965 ecr 6631861], length 38
        Pattern p = Pattern.compile("    ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+) > ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\\.([0-9]+): Flags \\[([^]]*)\\], cksum [^ ]* \\((correct|incorrect)\\), seq ([0-9:]+), ack ([0-9]+), win ([0-9]+), options \\[([^]]*)\\], length ([0-9]+)");
        Matcher m = p.matcher(line);
        if (!m.matches()) {
            throw new BadInputException("Failed to parse secondary.");
        }
    }
}
