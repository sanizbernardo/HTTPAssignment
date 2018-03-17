import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class TCPServer
{
    public static void main(String argv[]) throws Exception
    {
        ServerSocket welcomeSocket = new ServerSocket(80);
        int i = 0;
        while(true)
        {
            Socket connectionSocket = welcomeSocket.accept();
            if (connectionSocket != null)
            {
                Handler request = new Handler(connectionSocket);
                Thread thread = new Thread(request, "Thread " + Integer.toString(i));
                i +=1;
                thread.start();
            }
        }
    }

    static class Handler implements Runnable
    {
        Socket socket;
        private Handler(Socket socket)
        { this.socket = socket; }
        @Override
        public void run()
        {
            try {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());

                outToClient.writeBytes("Connected\n");

                boolean requestcomplete = false;
                String request = "";

                while (!requestcomplete) {
                    String input = inFromClient.readLine();
                    if (input.equals("")) {
                        requestcomplete = true;
                    }
                    request += input;
                    request += " ";
                }

                /*
                String[] words = request.split("\\s+");
                String method = words[0];
                String uri = words[1];
                String httpversion = words[2];

                switch (method) {
                    case "HEAD":
                        confirm(outToClient, httpversion);
                        head(outToClient, request);
                        break;
                    case "GET":
                        confirm(outToClient, httpversion);
                        get(outToClient, request);
                        break;
                    case "PUT":
                        confirm(outToClient, httpversion);
                        put();
                        break;
                    case "POST":
                        confirm(outToClient, httpversion);
                        post();
                        break;
                    default:
                        outToClient.writeBytes("400 Bad Request");
                }
                */

                if (request.substring(0, 3).equals("HEAD")) {
                    confirm(outToClient);
                    head(outToClient, request);
                } else if (request.substring(0, 3).equals("GET")) {
                    confirm(outToClient);
                    get(outToClient, request);
                } else if (request.substring(0, 3).equals("PUT")) {
                    confirm(outToClient);
                    put();
                } else if (request.substring(0, 3).equals("POST")) {
                    confirm(outToClient);
                    post();
                } else {
                    outToClient.writeBytes("400 Bad Request");
                }
            } catch(IOException e) {

            }

        }

        private void confirm(DataOutputStream out) throws  IOException {
            out.writeBytes("HTTP/1.1 200 OK\n");
        };

        private void head(DataOutputStream out, String request) throws  IOException {
            File htmlFile = new File("src/index.html");
            String htmlString = FileUtils.readFileToString(htmlFile);

            DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            out.writeBytes("Content-Type: " + "\n");
            out.writeBytes("Content-Length: " + htmlString.length() + "\n");
            out.writeBytes("Date: " + date.format(now) + "\n");
            out.writeBytes("\n");
        };

        private void get(DataOutputStream out, String request) throws  IOException {
            File htmlFile = new File("src/index.html");
            String htmlString = FileUtils.readFileToString(htmlFile);

            DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            out.writeBytes("Content-Type: " + "\n");
            out.writeBytes("Content-Length: " + htmlString.length() + "\n");
            out.writeBytes("Date: " + date.format(now) + "\n");
            out.writeBytes("\n");
            out.writeBytes(htmlString);
        };

        private void put() {

        };

        private void post() {

        };
    }
}