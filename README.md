Only collects IP, TCP and UDP.


Output file schema:

TCP:
    ip                                      tcp
    protocol;tos;ttl;id;offset;flags;length;flags;cksum;seq;ack;win;options;tcpProtocol;tcpLength
UDP:
    ip                                      udp
    protocol;tos;ttl;id;offset;flags;length;udpLength