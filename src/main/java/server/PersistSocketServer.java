package server;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static util.Logs.log;

public class PersistSocketServer implements Runnable {
    private static ArrayList<EchoProtocol> clients = new ArrayList<>();
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private final String host;
    private final int port;
    private final int timeout;

    public PersistSocketServer(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout=timeout;
    }

    @Override
    public void run() {
        try (var serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(host, port));
            while (true) {
                var clientSocket = serverSocket.accept();
                log("connected " + clientSocket);
                clientSocket.setSoTimeout(timeout*1000);
                var echoProtocol = new EchoProtocol(clients, clientSocket);
                clients.add(echoProtocol);
                pool.submit(echoProtocol);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class EchoProtocol implements Runnable {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        private static final char GS = 0x1D;
        private static final char RS = 0x1E;

        protected static ArrayList<EchoProtocol> clients;
        private final Socket socket;
        private final BufferedInputStream in;
        private final PrintWriter out;

        public EchoProtocol(ArrayList<EchoProtocol> echoclients, Socket socket) throws Exception {
            this.socket = socket;
            clients = echoclients;
            in = new BufferedInputStream(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        }

        @Override
        public void run() {
            try (socket) {
                tryRun();
            } catch (Exception e) {
                e.printStackTrace();
            }
            log("Finished socket " + socket);
        }

        private String readInputStream() throws Exception {
                var baos = new ByteArrayOutputStream();
                char ch = ' ';
                while (ch != RS) {
                    int chInt = 0;
                    try {
                        chInt = in.read();
                    }
                    catch (Exception e){
                        socket.close();
                        throw new SocketException("Server Socket Timeout. No Messages were received.");
                    }
                    if (chInt == -1) throw new IOException("Socket has been closed by a client outage." +
                            "Please reconnect.");
                    ch = (char) chInt;
                    baos.write(ch);
                }
                return baos.toString(StandardCharsets.UTF_8);
        }

        private void tryRun() throws Exception {
            while (true) {
                var receivedString = readInputStream();
                log("RECEIVED STRING " + receivedString);
                var parsedMessage = parseMessage(receivedString);
                //log("Received from " + socket + ": " + parsedMessage);
                sendMessageToAll(parsedMessage);
                }
            }

        private String parseMessage(String pack) {
            var buff = pack.split(String.valueOf(GS)); //Arrays.toString(<>)
            if (buff[0].equals("T_REGISTER")) return "New connected user " + buff[1];
            return buff[1] + " says: " + buff[2];
        }

        private void sendMessageToAll(String msg) {
            for (EchoProtocol client : clients) {
                client.out.println(formatter.format(date) + " " + msg);
            }
        }
    }
}