package com.example.tcp;

public class Packet {
    String Time = " ";      //Timestamp
    String SrcIP = " ";
    String SrcPort = " ";
    String DestIP = " ";
    String DestPort = " ";
    String Protocol = " ";
    String length = " ";
    String TCPflags = " ";
    String serverinf = " ";

    //extract information from tcpdump's output.(split the string).
    public void ReadPacket(String line) {
        System.out.println("Packet line: \"" + line + "\"");
        //"20:01:51.296732 IP (tos 0x0, ttl 64, id 46782, offset 0, flags [DF], proto TCP (6), length 60)"
        //"    192.168.0.17.58485 > 216.58.210.45.443: Flags [S], cksum 0x6b50 (incorrect -> 0x97a8), seq 4002566851, win 14600, options [mss 1460,sackOK,TS val 2811005 ecr 0,nop,wscale 6], length 0"
        Time = line.split(" ")[0];
        String temp = line.replace(':', ' ');
        String temp2 = temp.replace(')', ' ');
        String[] inf = temp2.split(" ");

        boolean getsrc = false;
        boolean getdest = false;


        for (int i = 1; i < inf.length; i++) {
            //System.out.println(i);
            if (inf[i].equals("length")) {
                this.length = inf[i + 1];
                i++;
            } else if (inf[i].equals("proto")) {
                this.Protocol = inf[i + 1];
                i++;
            } else if (!getdest) {
                int point_count = 0;
                for (int j = 0; j < inf[i].length(); j++) {
                    if (inf[i].charAt(j) == '.') {
                        point_count++;
                    }
                    if (point_count == 4) {
                        if (!getsrc) {
                            this.SrcIP = inf[i].substring(0, j);
                            this.SrcPort = inf[i].substring(j + 1);
                            getsrc = true;
                        } else {
                            this.DestIP = inf[i].substring(0, j);
                            this.DestPort = inf[i].substring(j + 1);

                            if (i + 2 < inf.length) {
                                this.TCPflags = inf[i + 2].replace(',', ' ');
                            }
                            getdest = true;

                            if (Protocol.equals("UDP")) {
                                for (int k = j + 1; k < inf.length; k++)
                                    serverinf = serverinf + inf[k];
                            }
                        }
                        break;
                    }
                }
            }
        }
        if (this.Protocol.equals("UDP")) {
            this.TCPflags = " ";
        }
    }

    public String ToString() {
        return Time + " " + SrcIP + " " + SrcPort + " " + DestIP + " " + DestPort + " " + length + " " + Protocol + " " + TCPflags;
    }

    //return time represented by second
    public int GetTimeSec() {
        String t[] = Time.replace('.', ':').split(":");
        System.out.print("adsfasdf Time::");
        System.out.println("\"" + Time + "\"");
        return Integer.parseInt(t[0]) * 3600 + Integer.parseInt(t[1]) * 60 + Integer.parseInt(t[2]);
    }
}
