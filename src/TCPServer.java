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
        // Initialize the server socket with a port
        ServerSocket welcomeSocket = new ServerSocket(6000);
        int i = 0;

        // always true -> server remains active
        while(true)
        {
            // Initialize the socket on which to accept the client
            Socket connectionSocket = welcomeSocket.accept();
            if (connectionSocket != null)
            {
                // New request from client, starting new thread for new request
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

        /**
         * This is the main function for running the server and handles everything.
         */
        @Override
        public void run()
        {
            try {
                // Set up communication
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
                AtomicBoolean running = new AtomicBoolean(true);
                System.out.println(inFromClient.ready());
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
                    // Checks if incoming request contains info about content length, if so, save the content length
                    String contentLen = "";
                    for (String str:lines) {
                        if (str.contains("Content-length"))
                            contentLen = str;
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

                    // Changing the resource from request to /index.html from /, because our homepage is at /index.html
                    if (requestline[1].equals("/")) {
                        requestline[1] = "/index.html";
                    }

                    // Check for HTTP version
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
                            put(inFromClient, outToClient, requestline, date,contentLen);
                            break;
                        case "POST":
                            post(inFromClient, outToClient, requestline, date,contentLen);
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

        /**
         * This method will be called when the client performs a HEAD request.
         * The method will first check if the request is valid, else will write out
         * error messages corresponding to the error made.
         * Then the method will write out the header that would be expected from the response that was made by
         * the client.
         * @param out
         * @param request
         * @param lines
         * @param date
         * @throws IOException
         */
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

        /**
         * This method will be called when the client performs a GET request.
         * The method will first check if the request is valid, else will write out
         * error messages corresponding to the error made.
         * Then the method will write out the header that would be expected from the response that was made by
         * the client and will also write out the body of the page that was requested.
         * @param out
         * @param request
         * @param lines
         * @param date
         * @throws IOException
         */
        private void get(DataOutputStream out, String[] request, String[] lines, String date) throws  IOException {
             //Check if file already exists
            byte [] byteOutput;
            File htmlFile = new File("src" + request[1]);
            System.out.println();
            if(!htmlFile.exists() || htmlFile.isDirectory()) {
                out.writeBytes(request[2] + " 404 Not found\r\n");
                out.writeBytes("\r\n");
                return;
            }

            Date moddate = new Date(htmlFile.lastModified());

            for (String line : lines) {
                if (line.length() > 18) {
                    if (line.substring(0, 18).equals("If-Modified-Since:")) {
                        Date reqdate = new Date();
                        try {
                            reqdate = dateFormat.parse(line.substring(19));
                        } catch (Exception e) {
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
            byteOutput = htmlString.getBytes();

            //output headers
            out.writeBytes(request[2] + " 200 OK\r\n");
            out.writeBytes("Content-Type: " + FilenameUtils.getExtension("src" + request[1]) + "\r\n");
            out.writeBytes("Content-Length: " + byteOutput.length + "\r\n");
            out.writeBytes("Date: " + date + "\r\n");
            out.writeBytes("\r\n");

            //output requested file
            out.write(byteOutput);
            out.writeBytes("\r\n");
        };

        /**
         * This method will be called when the client performs a PUT response.
         * The method will first check if the request is valid, else will write out
         * error messages corresponding to the error made.
         * Then it will read the data the client has provided for the html file it wants to write and will
         * create a html file with the name provided by the user and fill it with data
         * provided by the client.
         * @param in
         * @param out
         * @param request
         * @param date
         * @param contentLen
         * @throws IOException
         */
        private void put(BufferedReader in, DataOutputStream out, String[] request, String date, String contentLen) throws  IOException {
            String response = " 200 OK\r\n";
            int contentLength = extractNumber(contentLen);
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
            out.writeBytes(request[2] + response);
            out.writeBytes("Content Location: src" + request[1] + "\r\n");
            out.writeBytes("Date: " + date + "\r\n");
            out.writeBytes("\r\n");
            boolean bodycomplete = false;

            // Build body
            String body = "";
            while (!bodycomplete) {
                String inputline = in.readLine();
                if (inputline.equals("")) {
                    bodycomplete = true;
                }
                body += inputline;
                if (body.length() == contentLength)
                    break;
            }
            String htmlString = FileUtils.readFileToString(new File("src/template.html"));
            htmlString = htmlString.replace("$body", body);
            FileUtils.writeStringToFile(htmlFile, htmlString);


        };

        /**
         * This method will be called if the client performs a POST request.
         * The method will first check if the request is valid, else will write out
         * error messages corresponding to the error made.
         * Then it will read the data the client has provided for the html file it wants to write to and will
         * write this data to the html file named with the info given from the user.
         * @param in
         * @param out
         * @param request
         * @param date
         * @param contentLen
         * @throws IOException
         */
        private void post(BufferedReader in, DataOutputStream out, String[] request, String date, String contentLen) throws  IOException {
            //Check if file already exists
            File file = new File("src" + request[1]);
            if(!file.exists() || file.isDirectory()) {
                out.writeBytes(request[2] + " 404 Not found\r\n");
                out.writeBytes("\r\n");
                return;
            }

            out.writeBytes(request[2] + " 100 Continue\r\n");
            out.writeBytes("Host: localhost\r\n");
            out.writeBytes("Content-Type:" + FilenameUtils.getExtension("src" + request[1]) + "\r\n");
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

            out.writeBytes("Content-Length: " + body.length() + "\r\n");
            out.writeBytes("Date: " + date + "\r\n");
            out.writeBytes("\r\n");


        };

        /**
         * This method will be called when the client performs a DELETE request.
         * It wiill if the client did not make any errors while making this request, and if the
         * client has made errors, it will write the correct error message to the user.
         * If no errors were made, the correct page will be deleted and a header
         * will be written out to the user.
         * @param in
         * @param out
         * @param request
         * @param date
         * @throws IOException
         */
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


        /**
         * This method extracts a number from a string if there is one
         * and returns the integer.
         * @param str
         * @return
         */
        public static int extractNumber(final String str) {

            StringBuilder sb = new StringBuilder();
            boolean found = false;
            for(char c : str.toCharArray()){
                if(Character.isDigit(c)){
                    sb.append(c);
                    found = true;
                } else if(found){
                    break;
                }
            }

            return Integer.parseInt(sb.toString());
        }



    }
}