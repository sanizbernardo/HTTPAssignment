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

        // always true -> server remains active
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

        private Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run()
        {
            try {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());

                //Confirm thread has been started
                outToClient.writeBytes("Connected\n");

                //
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

                //Split request line into method, target file and http version
                String[] requestline = lines[0].split("\\s+");

                //Check if request is valid, give error message otherwise
                if (!isValidRequest(requestline)) {
                    outToClient.writeBytes("400 Bad Request");
                    //throw error
                }

                //get current date
                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                String date = format.format(now);

                //Choose correct method
                switch (requestline[0]) {
                    case "HEAD":
                        head(outToClient, requestline, date);
                        break;
                    case "GET":
                        get(outToClient, requestline, date);
                        break;
                    case "PUT":
                        put(outToClient, requestline, date);
                        break;
                    case "POST":
                        post(outToClient, requestline, date);
                        break;
                    case "DELETE":
                        delete(outToClient, requestline, date);
                        break;
                    default:
                        outToClient.writeBytes("400 Bad Request");
                        //throw error
                        break;
                }
            } catch(IOException e) {

            }
        }

        private boolean isValidRequest(String[] request) throws  IOException{
            boolean b = true;

            //Request always contains method and file. Http version is optional, default to HTTP 1.0. Never more than 3
            //arguments in request line
            if (request.length < 2 || request.length > 3) {
                b = false;
            } else if (request.length ==  2) {
                request[2] = "HTTP/1.0";
            } else if (!request[2].equals("HTTP/1.1")) {
                b = false;
            }

            return  b;
        };

        private void head(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Head detected");

            //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                out.writeBytes("404 Not found");
                //Throw error
            }

            String htmlString = FileUtils.readFileToString(htmlFile);

            //Output headers
            out.writeBytes("Content-Type: " + "\n");
            out.writeBytes("Content-Length: " + htmlString.length() + "\n");
            out.writeBytes("Date: " + date + "\n");
            out.writeBytes("\n");
        };

        private void get(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Get detected");

            //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                out.writeBytes("404 Not found");
                //Throw error
            }

            String htmlString = FileUtils.readFileToString(htmlFile);

            //output headers
            out.writeBytes("Content-Type: " + "\n");
            out.writeBytes("Content-Length: " + htmlString.length() + "\n");
            out.writeBytes("Date: " + date + "\n");
            out.writeBytes("\n");

            //output requested file
            out.writeBytes(htmlString);
        };

        private void put(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Put detected");
            String response = " 200 OK\n";

            //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                response = " 201 Created\n";
            }

            out.writeBytes(request[2] + response);
            out.writeBytes("Content Location: src" + request[1]);
            out.writeBytes("Date: " + date + "\n");
            out.writeBytes("\n");
        };

        private void post(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Post detected");

            //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                out.writeBytes("404 Not found");
                //Throw error
            }

            out.writeBytes("Host: localhost");
            out.writeBytes("Content-Type: ");
            out.writeBytes("Content-Length: ");
            out.writeBytes("Date: " + date + "\n");
            out.writeBytes("\n");

        };

        private  void delete(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Delete detected");

            //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                out.writeBytes("404 Not found");
                //Throw error
            }

            //Fetch delete message
            File message = new File("src/deletemessage.html");
            String confirmdelete = FileUtils.readFileToString(message);

            out.writeBytes("Date: " + date + "\n");
            out.writeBytes("\n");

            //output delete message
            out.writeBytes(confirmdelete);
        };

    }
}