import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class TCPServer
{
    public static void main(String argv[]) throws Exception
    {
        ServerSocket welcomeSocket = new ServerSocket(6000);
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

        private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

        @Override
        public void run()
        {
            try {
                System.out.println("---- New Request---");

                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());

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

                if (requestline[1].equals("/")) {
                    System.out.println("Req line");
                    requestline[1] = "/index.html";
                }
                 //Check if request is valid, give error message otherwise
                if (!isValidRequest(requestline)) {
                    outToClient.writeBytes("400 Bad Request\r\n");
                }

                //get current date
                Calendar calendar = Calendar.getInstance();
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String date = dateFormat.format(calendar.getTime());

                //Choose correct method
                switch (requestline[0]) {
                    case "HEAD":
                        head(outToClient, requestline, lines, date);
                        break;
                    case "GET":
                        get(outToClient, requestline, lines, date);
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
                        outToClient.writeBytes("400 Bad Request\r\n");
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

        private String head(DataOutputStream out, String[] request, String[] lines, String date) throws  IOException {
            System.out.println("Head detected: "+request[1] );
            //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                out.writeBytes("404 Not found\r\n");
            }

            Date moddate = new Date(htmlFile.lastModified());

            for (String line : lines) {
                if (line.length() > 18){
                    if (line.substring(0, 18).equals("If-Modified-Since:")) {
                        Date reqdate = new Date();
                        try {
                            reqdate = dateFormat.parse(line.substring(19));
                        } catch(Exception e) {
                            out.writeBytes("400 Bad Request\r\n");
                        }
                        if (moddate.compareTo(reqdate) > 0) {
                            out.writeBytes("304 Not Modified\r\n");
                        }
                    }
                }
            }

            String htmlString = FileUtils.readFileToString(htmlFile);

            //output headers
            out.writeBytes(request[2] + " 200 OK\r\n");
            out.writeBytes("Content-Type: " + "\r\n");
            out.writeBytes("Content-Length: " + htmlString.length() + "\r\n");
            out.writeBytes("Date: " + date + "\r\n");
            out.writeBytes("\r\n");
            out.flush();

            return htmlString;
        };

        private void get(DataOutputStream out, String[] request, String[] lines, String date) throws  IOException {
            System.out.println("Get detected");

            String htmlString = head(out, request, lines, date);

            //output requested file
            out.writeBytes(htmlString);
        };

        private void put(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Put detected");

            String response = " 200 OK\r\n";

            //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                response = " 201 Created\r\n";
            }

            out.writeBytes(request[2] + response);
            out.writeBytes("Content Location: src" + request[1] + "\r\n");
            out.writeBytes("Date: " + date + "\r\n");
            out.writeBytes("\r\n");
        };

        private void post(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Post detected");
            //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                out.writeBytes("404 Not found\r\n");
            }

            out.writeBytes("Host: localhost\r\n");
            out.writeBytes("Content-Type: \r\n");
            out.writeBytes("Content-Length: " + "\r\n");
            out.writeBytes("Date: " + date + "\r\n");
            out.writeBytes("\r\n");

        };

        private  void delete(DataOutputStream out, String[] request, String date) throws  IOException {
            System.out.println("Delete detected");
            //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                out.writeBytes("404 Not found\r\n");
            }

            //Fetch delete message
            File message = new File("src/deletemessage.html");
            String confirmdelete = FileUtils.readFileToString(message);

            out.writeBytes("Date: " + date + "\r\n");
            out.writeBytes("\r\n");

            //output delete message
            out.writeBytes(confirmdelete);
        };

    }
}