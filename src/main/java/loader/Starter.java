package loader;

import client.ClientStart;
import server.App;

public class Starter {
    public static void main(String[] args) {
//        log("Server starting...");
//        new PersistSocketServer().run();
////        new SessionSocketServer().run();
////        new SessionChannelServer().run();
////        new SessionSerializeServer().run();
////        new SessionSelectorServer().run();
//        log("Server finished");
//        log("Client starts...");
//        Constructor.construct().run();
//        log("Client Stops,,,,");

        new Thread(new ClientStart()).start();
        new Thread(new App()).start();
    }
}
