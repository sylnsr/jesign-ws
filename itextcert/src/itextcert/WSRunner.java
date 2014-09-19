package itextcert;

import utils.SimpleParser;

import javax.xml.ws.Endpoint;

public class WSRunner {

    public static void main (String[] args) throws Exception {
        int port = 9003;
        if (args.length > 0) {
            if (SimpleParser.IntOrZero(args[0]) > 0) {
                port = SimpleParser.IntOrZero(args[0]);
            }
        }

        String address = "http://127.0.0.1:"+port+"/certifier" ;
        Endpoint endpoint = Endpoint.publish(address, new Certifier());

        System.out.println(address);
        System.out.println("Hit return to stop...");
        System.in.read();
        System.out.println("Stopping server");
        endpoint.stop();
        System.out.println("Server stopped");
    }
}
