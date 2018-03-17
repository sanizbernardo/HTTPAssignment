import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
                List<String> input = new ArrayList<String>();

                while (!requestcomplete) {
                    String inputline = inFromClient.readLine();
                    if (inputline.equals("")) {
                        requestcomplete = true;
                    }
                    input.add(inputline);
                }

                String[] lines = new String[input.size()];
                input.toArray(lines);

                String[] requestline = lines[0].split("\\s+");

                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                String date = format.format(now);

                File f = new File("src" + requestline[1]);
                if(!f.exists() || f.isDirectory()) {
                    outToClient.writeBytes("404 Not found");
                    //Throw error
                }

                switch (requestline[0]) {
                    case "HEAD":
                        confirm(outToClient, requestline[2]);
                        head(outToClient, lines, date);
                        break;
                    case "GET":
                        confirm(outToClient, requestline[2]);
                        get(outToClient, lines, date);
                        break;
                    case "PUT":
                        confirm(outToClient, requestline[2]);
                        put(outToClient, lines, date);
                        break;
                    case "POST":
                        confirm(outToClient, requestline[2]);
                        post(outToClient, lines, date);
                        break;
                    case "DELETE":
                        confirm(outToClient, requestline[2]);
                        delete(outToClient, lines, date);
                        break;
                    default:
                        outToClient.writeBytes("400 Bad Request");
                        break;
                }
            } catch(IOException e) {

            }

        }

        private boolean isValidRequest(String[] request) {
            boolean b = true;
            if (request.length != 3 || !request[2].equals("HTTP/1.1")) {
                b = false;
            }
            return  b;
        };

        private void confirm(DataOutputStream out, String httpversion) throws  IOException {
            out.writeBytes(httpversion + " 200 OK\n");
        };

        private void head(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Head detected");
            File htmlFile = new File("src/index.html");
            String htmlString = FileUtils.readFileToString(htmlFile);

            out.writeBytes("Content-Type: " + "\n");
            out.writeBytes("Content-Length: " + htmlString.length() + "\n");
            out.writeBytes("Date: " + date + "\n");
            out.writeBytes("\n");
        };

        private void get(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Get detected");
            File htmlFile = new File("src/index.html");
            String htmlString = FileUtils.readFileToString(htmlFile);

            out.writeBytes("Content-Type: " + "\n");
            out.writeBytes("Content-Length: " + htmlString.length() + "\n");
            out.writeBytes("Date: " + date + "\n");
            out.writeBytes("\n");
            out.writeBytes(htmlString);
        };

        private void put(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Put detected");
            out.writeBytes("Date: " + date + "\n");
            out.writeBytes("\n");
        };

        private void post(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Post detected");
            out.writeBytes("Date: " + date + "\n");
            out.writeBytes("\n");

        };

        private  void delete(DataOutputStream out, String[] request, String date) throws  IOException {
            File htmlFile = new File("src/deletemessage.html");
            String htmlString = FileUtils.readFileToString(htmlFile);

            System.out.println("Delete detected");
            out.writeBytes("Date: " + date + "\n");
            out.writeBytes("\n");
            out.writeBytes(htmlString);
        };

    }
}