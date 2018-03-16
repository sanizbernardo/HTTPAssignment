import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class TCPServer
{
    public static void main(String argv[]) throws Exception
    {
        ServerSocket welcomeSocket = new ServerSocket(6789);
        while(true)
        {
            Socket connectionSocket = welcomeSocket.accept();
            int i = 0;
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
                String request = inFromClient.readLine();
                String[] args = request.split("\\s+");
                if (args[0].equals("HEAD")) {
                    confirm(outToClient);
                    head(outToClient);
                } else if (args[0].equals("GET")) {
                    confirm(outToClient);
                    get(outToClient);
                } else if (args[0].equals("PUT")) {
                    confirm(outToClient);
                    put();
                } else if (args[0].equals("POST")) {
                    confirm(outToClient);
                    post();
                } else {
                    outToClient.writeBytes("400 Bad Request");
                }
            } catch(IOException e) {
                //outToClient.writeBytes("500 Server Error");
            }

        }

        private void confirm(DataOutputStream out) throws  IOException {
            out.writeBytes("HTTP /1.1 200 OK");
        };

        private void head(DataOutputStream out) throws  IOException {
            File htmlFile = new File("src/webpage.html");
            String htmlString = FileUtils.readFileToString(htmlFile);

            DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            out.writeBytes("Content-Type: ");
            out.writeBytes("Content-Length: " + htmlString.length());
            out.writeBytes("Date: " + date.format(now));
        };

        private void get(DataOutputStream out) throws  IOException {
            File htmlFile = new File("src/webpage.html");
            String htmlString = FileUtils.readFileToString(htmlFile);

            DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            out.writeBytes("Content-Type: " + "\n");
            out.writeBytes("Content-Length: " + htmlString.length() + "\n");
            out.writeBytes("Date: " + date.format(now) + "\n \n");
            out.writeBytes(htmlString);
        };

        private void put() {

        };

        private void post() {

        };
    }
}