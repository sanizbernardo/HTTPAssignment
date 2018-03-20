import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

class HTTPClient {



    public static void main(String args[]) throws Exception
    {
        // Checking for correct input of user
        if (args.length != 3)
            throw new Exception("Incorrect input");

        // Initializing the variables from user arguments
        URI uri = new URI(args[1]);
        String path = uri.getPath();
        int port = Integer.valueOf(args[2]);


        // Creating new socket to connect to URI with correct port
        Socket clientSocket = new Socket(path,port);
        Handler request = new Handler(clientSocket,args);
        request.run();



    }

    static class Handler implements Runnable
    {
        Socket clientSocket;
        String HTTPCommand;
        URI uri;
        String path;
        int port;

        private Handler(Socket socket, String [] args) throws URISyntaxException {
            this.clientSocket = socket;
            HTTPCommand = args[0];
            uri = new URI(args[1]);
            path = uri.getPath();
            port = Integer.valueOf(args[2]);

        }
        public void run() {

            int counter = 1;

            InputStream inputStream = null;
            DataOutputStream outToServer = null;
            try {
                // Get the input stream from the clientsocket
                inputStream = clientSocket.getInputStream();
                // Create output stream for requests to socket
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Creating new html template
            File htmlTemplateFile = new File("src/template.html");
            String request = "";
            String resource = "/";
            while (true) {
                try {
                    /*
                     * Variables
                     */
                    Scanner scanner = new Scanner(System.in);
                    String fullText;
                    String htmlText;
                    String header;

                    /*
                     * Check if this is the first iteration or if the user has submitted a new request. If the user has submitted a new request,
                     * replace the variables of HTTP command and path with the arguments provided in the new response.
                     */
                    if (counter > 1) {
                        ArrayList<String> args=parseRequest(request);
                        HTTPCommand = args.get(0);
                        resource = args.get(1);
                        request = "/";
                    }

                    // Creating byte array for incoming bytes
                    byte[] bytes = new byte[100];

                    /*
                     * Check for which HTTP command to execute
                     */

                    // Incoming HTTP command from request is GET, so calling get method with
                    // correct path and data output stream
                    switch (HTTPCommand) {
                        case "HEAD":
                            Head(path, outToServer, resource);
                            break;
                        case "GET":
                            Get(path, outToServer, resource);
                            break;
                        case "PUT":
                            PutOrPost(HTTPCommand, path, outToServer);
                            break;
                        case "POST":
                            PutOrPost(HTTPCommand, path, outToServer);
                            break;
                        case "DELETE":
                            Delete(uri.toString(), outToServer, path);
                            break;
                        default:
                            break;
                    }


                    /*
                     * Convert the bytes from input stream to string
                     */

                    // Looking for header in inputstream until byte 13 10 13 10
                    header = splitUpHeaderRest(new byte[1],inputStream);

                    //Determine the content length from the header for reading our html part from the inputstream
                    int contentLen = findContentLen(header);


                    // Looking for html part of input stream after header until inputstream is empty and convert bytes to string
                    htmlText = byteToString(bytes, inputStream,contentLen);
                    // Add header and html part together
                    fullText = header;
                    fullText += htmlText;


                    /*
                     * Parsing
                     */

                    //This part calls the method to find the title part and body part of the html text part
                    //For the title part it looks for the title tags and remembers the start and end index of the title.
                    //For the body part it works the same as the title part.
                    int titleStart = 0, titleEnd = 0, bodyStart = 0, bodyEnd = 0;
                    titleStart = findBeginEndIndex(htmlText, "title>", "/title>")[0];
                    titleEnd = findBeginEndIndex(htmlText, "title>", "/title>")[1];
                    bodyStart = findBeginEndIndex(htmlText, "body>", "/body>")[0];
                    bodyEnd = findBeginEndIndex(htmlText, "body>", "/body>")[1];

                    /*
                     * Convert bytes to images
                     */

                    // If the method above has found any images, loop through all the found images and call up getImage method on every image
                    // to convert the bytes of the images in inputStream into files.
                    if (getImages(htmlText).size() != 0) {
                        for (String img : getImages(htmlText)) {
                            if (img.length() != 0)
                                getImage(img, path, inputStream, outToServer);
                        }
                    }

                    //We print the header of the response of the HTTP Command to the terminal.
                    if (HTTPCommand.equals("GET"))
                        System.out.println(header);
                    else if (HTTPCommand.equals("HEAD"))
                        System.out.println(fullText);


                    // Use the found parameters of the title and body part of the html part and take these out of the string so we can
                    // replace the title and body part of the html template to create our own template.
                    String htmlString = FileUtils.readFileToString(htmlTemplateFile);
                    String title = htmlText.substring(titleStart, titleEnd);;
                    String body = htmlText.substring(bodyStart, bodyEnd);
                    htmlString = htmlString.replace("$title", title);
                    htmlString = htmlString.replace("$body", body);
                    File newHtmlFile = new File("src/new.html");
                    FileUtils.writeStringToFile(newHtmlFile, htmlString);

                    System.out.println("Type your next request or type \"STOP\" if you want to close the client.");
                    request = scanner.nextLine();
                    if (request.equals("STOP")) {
                        break;
                    }
                    counter ++;


                } catch (IOException e) {
                    System.out.println("IOException happened");
                }
            }
            try {
                System.out.println("Socket closing, goodbye...");
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Socket close failed.");
            }

        }

        /**
         * This method sends the HTTP command GET out to the server with the provided host and resource.
         * This is all done in HTTP 1.1
         * @param host
         * @param outToServer
         * @param resource
         * @throws IOException
         */
        void Get(String host,DataOutputStream outToServer,String resource) throws IOException
        {
            if (host.contains("httpbin.org"))
                resource ="/get";
            outToServer.writeBytes("GET "+resource+" HTTP/1.1\r\n");
            outToServer.writeBytes("Host: "+host+"\r\n");
            outToServer.writeBytes("\r\n");
            outToServer.flush();
        }

        /**
         * This method sends the HTTP command HEAD out to the server with the provided
         * @param host
         * @param outToServer
         * @param resource
         * @throws IOException
         */
        void Head(String host, DataOutputStream outToServer,String resource) throws IOException
        {
            outToServer.writeBytes("HEAD "+resource+" HTTP/1.1\r\n");
            outToServer.writeBytes("Host: "+host+"\r\n");
            outToServer.writeBytes("\r\n");
            outToServer.flush();
        }

        /**
         * This method will be used for the HTTP commands PUT and POST since the server expect the same input
         * in both cases.
         * First, it writes to the server either a PUT or a POST commando.
         * Then it asks for the host, afterwards it asks for the content type and next it asks for the content length.
         * At last it asks as input from the user the content to be PUT or POSTed on the server.
         * @param putOrPost
         * @param path
         * @param outToServer
         * @throws IOException
         */
        void PutOrPost(String putOrPost,String path, DataOutputStream outToServer) throws IOException {
            Scanner scanner = new Scanner(System.in);
            outToServer.writeBytes(putOrPost +" /"+path+" HTTP/1.1\r\n");
            System.out.print("Host: ");
            outToServer.writeBytes("Host: "+scanner.nextLine()+"\r\n");
            System.out.print("Content-type: ");
            outToServer.writeBytes("Content-type: "+scanner.nextLine()+"\r\n");
            System.out.print("Content-length: ");
            outToServer.writeBytes("Content-length: "+scanner.nextLine()+"\r\n");
            outToServer.writeBytes("\r\n");
            System.out.println("Please input the content: ");
            outToServer.writeBytes(scanner.nextLine()+"\r\n");
            outToServer.flush();
        }


        /**
         * This method deletes a given file from the server.
         * @param host
         * @param outToServer
         * @param file
         * @throws IOException
         */
        void Delete(String host, DataOutputStream outToServer, String file) throws IOException {
            outToServer.writeBytes("DELETE /"+file+" HTTP/1.1");
            outToServer.writeBytes("Host: "+host+"\r\n");
            outToServer.writeBytes("\r\n");
            outToServer.flush();
        }

        /**
         * This method reads the bytes from the input stream and the amount read is saved in
         * a variable 'chunk'. We check if there is more data left in the input stream and if 'chunk'
         * is larger than 0 (thus there has been data read from the input stream). If this is the case,
         * we convert the bytes read to string and add it to the final string. We read again and check again
         * for the same conditions. If these conditions are not met, we jump out of the loop and check
         * if chunk is larger than 0, and if that is the case, we write the remaining bytes to string
         * and add it to the final string.
         * @param bytes
         * @param inputStream
         * @return
         * @throws IOException
         */
        String byteToString(byte[] bytes,InputStream inputStream,int contentLen) throws IOException
        {
            String byteString = "";
            int j = 0;
            int chunk = inputStream.read(bytes);

            while (chunk != -1)  {
                String substr = new String(bytes,StandardCharsets.UTF_8).substring(0,chunk);
                byteString += substr;
                j+= chunk;
                if (j >= contentLen) {
                    break;
                }
                chunk = inputStream.read(bytes);
            }

            return byteString;

        }

        /**
         * This method searches for the header of response of the HTTP Command, the response contains
         * a header part and a http text part.
         * @param fullText
         * @return
         */

        public String searchForHeader(String fullText) {
            String header = "";
            for (int i=0;i <fullText.length();i++) {
                if (fullText.substring(0,i).contains("<HTML>") || fullText.substring(0,i).contains("<html>")) {
                    header =  fullText.substring(0,i-6);
                    break;
                }
            }
            return header;
        }


        String splitUpHeaderRest(byte [] bytes,InputStream inputStream) throws IOException {
            boolean rBool1 = false;
            boolean rBool2 = false;
            boolean nBool1 = false;
            boolean nBool2 = false;
            int length;
            String line = "";
            while((length = inputStream.read(bytes)) != -1) {
                line += new String(bytes,0,length);
                for (byte b: bytes){
                    if (b == 13 && !rBool1) {
                        rBool1 = true;
                    }
                    else if (b == 10 && rBool1 && !nBool1) {
                        nBool1 = true;
                    }
                    else if (b == 13 && rBool1 && nBool1 && !rBool2) {
                        rBool2 = true;
                    }
                    else if (b == 10 && rBool1 && nBool1 && rBool2 && !nBool2) {
                        nBool2 = true;
                        break;
                    }
                    else if (b != 13 || b != 10) {
                        rBool1 = false;
                        rBool2 = false;
                        nBool1 = false;
                        nBool2 = false;
                    }
                }
                if (rBool1 && rBool2 && nBool1 && nBool2) {
                    break;
                }
            }
            return line;
        }



        /**
         * This method sends out a GET command for the given image to the given host,
         * finds the given image inside the input stream and takes the right amount of bytes and writes
         * these bytes into a file that will be stored inside the source folder.
         * @param image
         * @param host
         * @param inputStream
         * @param outToServer
         * @throws IOException
         */
        void getImage(String image, String host, InputStream inputStream,DataOutputStream outToServer) throws IOException {
            // Initialize some variables that will be needed in this method
            byte [] bytes = new byte [1];
            // Send out the GET request to the server
            Get(host,outToServer,"/"+image);

            // This part of the method checks for the combination of bytes 13 10 13 10 in this order. This combination
            // signals for the end of the header and the begin of the actual response from the GET command.
            String line = splitUpHeaderRest(bytes,inputStream);

            // This part searches for the content length in the header.
            int contentLen = findContentLen(line);

            // If the image from the server is stored in another folder than the source, a new directory will be created inside source.
            if (detectPathImg(image) != null) {
                new File(detectPathImg("src/"+image)).mkdirs();
            }

            FileOutputStream fos = new FileOutputStream("src/"+image);
            byte [] htmlByte = new byte[4096];
            int chunk =inputStream.read(htmlByte);

            // This loop will keep looping as long as there is data in the input stream and the amount of iterations won't exceed the
            // content length that was found in the header.
            int j = 0;
            while (chunk != -1)  {
                fos.write(htmlByte,0,chunk);
                j+= chunk;
                if (j >= contentLen) {
                    break;
                }
                chunk = inputStream.read(htmlByte);
            }


            outToServer.flush();


        }

        /**
         * This method uses jsoup to parse the document and searches for img tags inside the html document and returns an arraylist of all the images.
         * @param html
         * @return
         */
        ArrayList<String> getImages(String html) {
            ArrayList<String> images = new ArrayList<>();
            Document doc = Jsoup.parse(html);

            // Get all img tags
            Elements img = doc.getElementsByTag("img");

            // Loop through img tags
            for (Element el : img) {
                //search for src
                images.add(el.attr("src"));
            }
            return images;
        }

        /**
         * This method searches for the directory of the provided image if it's in another directory and returns the directory of the image.
         * @param image
         * @return
         */
        String detectPathImg(String image) {
            int i =0;
            if (image.contains("/")) {
                for (i = image.length()-1;i >-1;i--) {
                    if (image.charAt(i) == '/')
                        break;
                }
            }
            if (i == 0)
                return null;
            else {
                return image.substring(0, i + 1);

            }
        }

        /**
         * This method checks for the begin and end index of a certain part of the given html file.
         * @param htmlText
         * @param searchBegin
         * @param searchEnd
         * @return
         */
        int[] findBeginEndIndex(String htmlText, String searchBegin, String searchEnd) {
            String searchUpperBegin = searchBegin.toUpperCase(), searchUpperEnd = searchEnd.toUpperCase();
            int start = 0, end = 0;
            for (int i = 0; i < htmlText.toCharArray().length - 9; i++) {
                Character c = (htmlText.charAt(i));
                if (c.equals('<')) {
                    if (htmlText.substring(i + 1, i + 1 + searchBegin.length()).equals(searchUpperBegin) || htmlText.substring(i + 1, i + 1 + searchBegin.length()).equals(searchBegin)) {
                        start = i + 1 + searchBegin.length();
                    }
                }
                if (c.equals('<')) {
                    if (htmlText.substring(i + 1, i + 1 + searchEnd.length()).equals(searchUpperEnd) || htmlText.substring(i + 1, i + 1 + searchEnd.length()).equals(searchEnd)) {
                        end = i;
                    }
                }
            }
            int [] result = {start, end};
            return result;
        }

        /**
         * This method takes in a request as a string that contains 3 seperate words. The request is
         * always of the form : "HTTPCommand RESOURCE HTTP/1.1". It splices the whole string up into
         * these 3 parts and saves each string separately into an arraylist.
         * @param request
         * @return
         */
        ArrayList<String> parseRequest(String request) {
            ArrayList<String> args = new ArrayList<>();
            int j = 0;
            int i = 0;
            for (i=0;i < request.length();i++) {
                if (request.charAt(i) == ' ') {
                    if (j < i)
                        args.add(request.substring(j,i));
                    j = i+1;
                }
            }
            if (j < i)
                args.add(request.substring(j,i));
            return args;
        }

        int findContentLen(String line) {
            String contentLength = "";
            boolean cond = false;
            outerloop:
            for (int i = 0; i< line.length()-15;i++) {
                if (line.substring(i,i+15).equals("Content-Length:")) {
                    for (int j = i+15;j < line.length();j++) {
                        if (Character.toString(line.charAt(j)).matches("-?\\d+") && cond)
                            break outerloop;
                        if (Character.toString(line.charAt(j)).matches("-?\\d+")) {
                            contentLength += line.charAt(j);
                            cond = true;
                        }
                    }
                }
            }
            return Integer.parseInt(contentLength);
        }


    }


}