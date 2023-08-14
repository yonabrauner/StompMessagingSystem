package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        String serverType = args[1];
        switch (serverType){
            case("tpc"): {
                System.out.println("starting tpc server...");
                Server.threadPerClient(
                        port, //port
                        () -> new StompProtocol(),      // protocol factory
                        () -> new StompEncoderDecoder() // message encoder decoder factory
                ).serve();
            }

            case("reactor"):{
                System.out.println("starting reeactor server...");
                Server.reactor(
                        Runtime.getRuntime().availableProcessors(),
                        port, //port
                        () -> new StompProtocol(),      //protocol factory
                        () -> new StompEncoderDecoder() //message encoder decoder factory 
                ).serve();
            }
        }

    }
}

