import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

        private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

        @Override
        public void run()
        {
            try {
                // Set up communication
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
                AtomicBoolean running = new AtomicBoolean(true);

                // Notify client
                outToClient.writeBytes("Connected\r\n");
                outToClient.writeBytes("Quit command is '!, Enter, Enter'\r\n");

                while (running.get()){
                    boolean requestcomplete = false;

                    // Build request
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

                    // Check for exit command
                    for (String line : lines) {
                        if (line.equals("!")) {
                            running.set(false);
                        }
                    }
                    if (!running.get()) {
                        break;
                    }

                    // Split request line into method, target file and http version
                    String[] requestline = lines[0].split("\\s+");

                    // Check validity of request line
                    if (requestline.length < 2 || requestline.length > 3) {
                        outToClient.writeBytes("400 Bad Request\n");
                        outToClient.writeBytes("\r\n");
                        break;
                    } else if (requestline.length == 2) {
                        requestline = new String[] {requestline[0], requestline[1], "HTTP/1.0"};
                    }

                    if (requestline[1].equals("/")) {
                        requestline[1] = "/index.html";
                    }

                    // HTTP versie controleren
                    if (requestline[2].equals("HTTP/1.0")) {
                        running.set(false);
                    } else if (requestline[2].equals("HTTP/1.1")) {
                        boolean hostgiven = false;
                        for (String line : lines) {
                            if (line.equals("Host: localhost")) {
                                hostgiven = true;
                            }
                            if (line.equals("Connection: close")) {
                                running.set(false);
                            }
                        }
                        if (!hostgiven) {
                            outToClient.writeBytes("400 Bad Request\n");
                            outToClient.writeBytes("\r\n");
                            continue;
                        }
                    } else {
                        outToClient.writeBytes("400 Bad Request\n");
                        outToClient.writeBytes("\r\n");
                        continue;
                    }

                    // Get current date
                    Calendar calendar = Calendar.getInstance();
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                    String date = dateFormat.format(calendar.getTime());

                    // Choose correct method
                    switch (requestline[0]) {
                        case "HEAD":
                            head(outToClient, requestline, lines, date);
                            break;
                        case "GET":
                            get(outToClient, requestline, lines, date);
                            break;
                        case "PUT":
                            put(inFromClient, outToClient, requestline, date);
                            break;
                        case "POST":
                            post(inFromClient, outToClient, requestline, date);
                            break;
                        case "DELETE":
                            delete(inFromClient, outToClient, requestline, date);
                            break;
                        default:
                            outToClient.writeBytes("400 Bad Request\n");
                            outToClient.writeBytes("\r\n");
                            break;
                    }
                }
                outToClient.writeBytes("Connection closed\r\n");
                this.socket.close();
            } catch(IOException e) {
                // Write "500 Server Error" somehow
            }
        }

        private void head(DataOutputStream out, String[] request, String[] lines, String date) throws  IOException {
            //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                out.writeBytes(request[2] + " 404 Not found\r\n");
                out.writeBytes("\r\n");
                return;
            }

            Date moddate = new Date(htmlFile.lastModified());

            for (String line : lines) {
                if (line.length() > 18){
                    if (line.substring(0, 18).equals("If-Modified-Since:")) {
                        Date reqdate = new Date();
                        try {
                            reqdate = dateFormat.parse(line.substring(19));
                        } catch(Exception e) {
                            out.writeBytes(request[2] + " 400 Bad Request\r\n");
                            out.writeBytes("\r\n");
                            return;
                        }
                        if (moddate.compareTo(reqdate) > 0) {
                            out.writeBytes(request[2] + " 304 Not Modified\r\n");
                            out.writeBytes("\r\n");
                            return;
                        }
                    }
                }
            }

            String htmlString = FileUtils.readFileToString(htmlFile);

            //output headers
            out.writeBytes(request[2] + " 200 OK\r\n");
            out.writeBytes("Content-Type: " + FilenameUtils.getExtension("src" + request[1]) + "\r\n");
            out.writeBytes("Content-Length: " + htmlString.length() + "\r\n");
            out.writeBytes("Date: " + date + "\r\n");
            out.writeBytes("\r\n");
        };

        private void get(DataOutputStream out, String[] request, String[] lines, String date) throws  IOException {
             //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                out.writeBytes(request[2] + " 404 Not found\r\n");
                out.writeBytes("\r\n");
                return;
            }

            Date moddate = new Date(htmlFile.lastModified());

            for (String line : lines) {
                if (line.length() > 18){
                    if (line.substring(0, 18).equals("If-Modified-Since:")) {
                        Date reqdate = new Date();
                        try {
                            reqdate = dateFormat.parse(line.substring(19));
                        } catch(Exception e) {
                            out.writeBytes(request[2] + " 400 Bad Request\r\n");
                            out.writeBytes("\r\n");
                            return;
                        }
                        if (moddate.compareTo(reqdate) > 0) {
                            out.writeBytes(request[2] + " 304 Not Modified\r\n");
                            out.writeBytes("\r\n");
                            return;
                        }
                    }
                }
            }

            String htmlString = FileUtils.readFileToString(htmlFile);

            //output headers
            out.writeBytes(request[2] + " 200 OK\r\n");
            out.writeBytes("Content-Type: " + FilenameUtils.getExtension("src" + request[1]) + "\r\n");
            out.writeBytes("Content-Length: " + htmlString.length() + "\r\n");
            out.writeBytes("Date: " + date + "\r\n");
            out.writeBytes("\r\n");

            //output requested file
            out.writeBytes(htmlString);
            out.writeBytes("\r\n");
        };

        private void put(BufferedReader in, DataOutputStream out, String[] request, String date) throws  IOException {
            String response = " 200 OK\r\n";

            //Check if file already exists
            File htmlFile = new File("src" + request[1]);
            if (request[1].equals("index.html") || request[1].equals("template.html") || request[1].equals("deletemessage.html")) {
                out.writeBytes(request[2] + " 403 Forbidden\r\n");
                out.writeBytes("\r\n");
                return;
            }else if(!htmlFile.exists() || htmlFile.isDirectory()) {
                response = " 201 Created\r\n";
            }

            out.writeBytes(request[2] + " 100 Continue\r\n");

            boolean bodycomplete = false;

            // Build body
            String body = "";
            while (!bodycomplete) {
                String inputline = in.readLine();
                if (inputline.equals("")) {
                    bodycomplete = true;
                }
                body += inputline;
            }

            out.writeBytes(request[2] + response);
            out.writeBytes("Content Location: src" + request[1] + "\r\n");
            out.writeBytes("Date: " + date + "\r\n");
            out.writeBytes("\r\n");
        };

        private void post(BufferedReader in, DataOutputStream out, String[] request, String date) throws  IOException {
            //Check if file already exists
            File file = new File("src" + request[1]);
            if(!file.exists() || file.isDirectory()) {
                out.writeBytes(request[2] + " 404 Not found\r\n");
                out.writeBytes("\r\n");
                return;
            }

            out.writeBytes(request[2] + " 100 Continue\r\n");

            boolean bodycomplete = false;

            // Build body
            String body = "";
            while (!bodycomplete) {
                String inputline = in.readLine();
                if (inputline.equals("")) {
                    bodycomplete = true;
                }
                body += inputline;
            }

            out.writeBytes("Host: localhost\r\n");
            out.writeBytes("Content-Type:" + FilenameUtils.getExtension("src" + request[1]) + "\r\n");
            out.writeBytes("Content-Length: " + body.length() + "\r\n");
            out.writeBytes("Date: " + date + "\r\n");
            out.writeBytes("\r\n");
        };

        private  void delete(BufferedReader in, DataOutputStream out, String[] request, String date) throws  IOException {
            //Check if file already exists
            File file = new File("src" + request[1]);

            if (request[1].equals("/index.html") || request[1].equals("/template.html") || request[1].equals("/deletemessage.html")) {
                out.writeBytes(request[2] + " 403 Forbidden\r\n");
                out.writeBytes("\r\n");
                return;
            } else if (!file.exists() || file.isDirectory()) {
                out.writeBytes(request[2] + " 404 Not found\r\n");
                out.writeBytes("\r\n");
                return;
            }

            if (file.delete()) {
                //Fetch delete message
                File message = new File("src/deletemessage.html");
                String confirmdelete = FileUtils.readFileToString(message);

                out.writeBytes(request[2] + " 200 OK\r\n");
                out.writeBytes("Date: " + date + "\r\n");
                out.writeBytes("\r\n");

                //output delete message
                out.writeBytes(confirmdelete);
                out.writeBytes("\r\n");
            } else {
                out.writeBytes(request[2] + "403 Forbidden\r\n");
                out.writeBytes("\r\n");
            }
        };

    }
}