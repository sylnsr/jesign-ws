package docx4j;

import javax.xml.ws.Endpoint;

public class WSRunner {

    public static void main (String[] args) throws Exception {

        String address = "http://127.0.0.1:9001/textsubstitution" ;
        Endpoint endpoint = Endpoint.publish(address, new TextSubstitution());

        System.out.println(address);
        System.out.println("Hit return to stop...");
        System.in.read();
        System.out.println("Stopping server");
        endpoint.stop();
        System.out.println("Server stopped");
    }
}
