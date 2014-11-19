package itext;

import utils.SimpleParser;

import javax.xml.ws.Endpoint;

public class WSRunner {

    public static void main (String[] args) throws Exception {

        int port = 9002;
        if (args.length > 0) {
            if (SimpleParser.IntOrZero(args[0]) > 0) {
                port = SimpleParser.IntOrZero(args[0]);
            }
        }

        String address = "http://0.0.0.0:"+port+"/tokenfinder" ;
        Endpoint endpoint_1 = Endpoint.publish(address, new TokenFinder());

        address = "http://0.0.0.0:"+port+"/imagestamper" ;
        Endpoint endpoint_2 = Endpoint.publish(address, new ImageStamper());

        System.out.println(address);
        System.out.println("Hit return to stop...");
        System.in.read();
        System.out.println("Stopping server");
        endpoint_1.stop();
        endpoint_2.stop();
        System.out.println("Server stopped");
    }
}
