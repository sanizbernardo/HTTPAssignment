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
                    String htmlText = "";
                    String header;
                    int contentLen=0;

                    /*
                     * Check if this is the first iteration or if the user has submitted a new request. If the user has submitted a new request,
                     * replace the variables of HTTP command and path with the arguments provided in the new response.
                     */
                    if (counter > 1) {
                        ArrayList<String> args=parseRequest(request);
                        if (args.size() != 3) {
                            outToServer.writeBytes("!\n\n");
                            break;
                        }
                        else {

                            HTTPCommand = args.get(0);
                            resource = args.get(1);
                            request = "/";
                        }
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
                            PutOrPost(HTTPCommand, resource, outToServer);
                            break;
                        case "POST":
                            PutOrPost(HTTPCommand, resource, outToServer);
                            break;
                        case "DELETE":
                            Delete(outToServer, resource);
                            break;
                        default:
                            continue;
                    }


                    /*
                     * Convert the bytes from input stream to string
                     */
                    // Looking for header in inputstream until byte 13 10 13 10
                    header = splitUpHeaderRest(new byte[1],inputStream);
                    //Determine the content length from the header for reading our html part from the inputstream
                    if (HTTPCommand.equals("GET") || (HTTPCommand.equals("HEAD")) || (HTTPCommand.equals("POST")))
                        contentLen = findContentLen(header);


                    // Looking for html part of input stream after header until inputstream is empty and convert bytes to string
                    if (HTTPCommand.equals("DELETE") && header.contains("200")) {
                        htmlText = searchForHtml(new byte[100],inputStream);
                    }
                    else if (HTTPCommand.equals("PUT") || HTTPCommand.equals("POST")) {}
                    else if(!HTTPCommand.equals("HEAD") && !HTTPCommand.equals("DELETE")) {
                        htmlText = byteToString(bytes, inputStream, contentLen);
                    }

                    //We print the header of the response of the HTTP Command to the terminal.

                    System.out.println(header);

                    /*
                     * Parsing
                     */

                    //This part calls the method to find the title part and body part of the html text part
                    //For the title part it looks for the title tags and remembers the start and end index of the title.
                    //For the body part it works the same as the title part.
                    int titleStart = 0, titleEnd = 0, bodyStart = 0, bodyEnd = 0;
                    int [] titleIndex = findBeginEndIndex(htmlText, "title>", "/title>");
                    int [] bodyIndex = findBeginEndIndex(htmlText, "body>", "/body>");
                    titleStart = titleIndex[0];
                    titleEnd = titleIndex[1];
                    bodyStart = bodyIndex[0];
                    bodyEnd = bodyIndex[1];

                    /*
                     * Convert bytes to images
                     */

                    // If the method above has found any images, loop through all the found images and
                    // call up getImage method on every image
                    // to convert the bytes of the images in inputStream into files.
                    System.out.println("Image: "+getImages(htmlText));
                    if (getImages(htmlText).size() != 0) {
                        for (String img : getImages(htmlText)) {
                            if (img.length() != 0)
                                getImage("",img, path, inputStream, outToServer);
                        }
                    }



                    // Use the found parameters of the title and body part of the html part and take these out of the string so we can
                    // replace the title and body part of the html template to create our own template.
                    if (! isImage(getFileExtension(resource))) {
                        String htmlString = FileUtils.readFileToString(htmlTemplateFile);
                        String title = htmlText.substring(titleStart, titleEnd);
                        String body = htmlText.substring(bodyStart, bodyEnd);
                        htmlString = htmlString.replace("$title", title);
                        htmlString = htmlString.replace("$body", body);
                        File newHtmlFile = new File("src/new.html");
                        FileUtils.writeStringToFile(newHtmlFile, htmlString);
                    }
                    else {
                        System.out.println("Hier??");
                        getImage("client/",resource,path,inputStream,outToServer);
                    }

                    //Check if the user wants to do another request
                    if (uri.toString().contains("tcpipguide.com"))
                        break;
                    else {
                        System.out.println("Type your next request or type \"STOP\" if you want to close the client.");
                        request = scanner.nextLine();
                        if (request.equals("STOP")) {
                            break;
                        }
                        counter++;
                    }


                } catch (IOException e) {
                    System.out.println("IOException happened");
                    break;
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
            StringBuilder stringB = new StringBuilder();
            stringB.append("<p>");
            String line = "";
            System.out.println("Press provide body for your "+putOrPost+" request, to finish send ENTER as line: ");
            while (!(line = scanner.nextLine()).equals("")) {
                System.out.println(line.equals("\n"));
                stringB.append(line);
            }
            System.out.println("test");
            stringB.append("</p>");
            String data = stringB.toString();
            outToServer.writeBytes(putOrPost +" "+path+" HTTP/1.1\r\n");
            outToServer.writeBytes("Host: localhost \r\n");
            outToServer.writeBytes(data+"\r\n\r\n");
            outToServer.flush();
        }


        /**
         * This method deletes a given file from the server.
         * @param outToServer
         * @param resource
         * @throws IOException
         */
        void Delete(DataOutputStream outToServer, String resource) throws IOException {
            Scanner scanner = new Scanner(System.in);
            outToServer.writeBytes("DELETE "+resource+" HTTP/1.1\r\n");
            System.out.print("Host: ");
            String line = scanner.nextLine();
            outToServer.writeBytes("Host: "+line+"\r\n");

            outToServer.writeBytes("\r\n");
            outToServer.flush();
        }

        /**
         * This method reads bytes from the input stream and writes them into a 100 byte array. It keeps a
         * variable chunk to know how many bytes it has read. It then converts these bytes into string and
         * adds the string to a string that will be result of this method. A counter is also being kept, to which
         * the value of chunk keeps getting added onto.
         * The method keeps repeating these steps until the counter is larger than or equal to the given content length.
         * The method then returns the final string.
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
         * This method converts bytes to string as it reads the bytes from the input stream and stops until
         * the string contains </html>, at which point we return the string that was read from the bytes
         * of the input stream.
         * @param bytes
         * @param inputStream
         * @return
         * @throws IOException
         */
        public String searchForHtml(byte[] bytes, InputStream inputStream) throws IOException {
            String byteString = "";
            int chunk = inputStream.read(bytes);

            while (!byteString.contains("</html>")) {
                String substr = new String(bytes, StandardCharsets.UTF_8).substring(0, chunk);
                byteString += substr;
                chunk = inputStream.read(bytes);
                if (chunk == 0)
                    break;
            }
            return byteString;

        }


        /**
         * This method reads bytes from input stream and tries to stop reading until the end of the header.
         * This end is indicated by the following 4 bytes: 13 10 13 10. For each byte of this combination
         * the method has a boolean value. The methods checks byte per byte and changes the boolean value of
         * the correspondending boolean value to true if the byte is found.
         *.If all the boolean values for each byte of the combination are found to be true, the method breaks
         * out of the while loop and returns the String of all the bytes it has read up until the 4 byte combination.
         * @param bytes
         * @param inputStream
         * @return
         * @throws IOException
         */
        String splitUpHeaderRest(byte [] bytes,InputStream inputStream) throws IOException {
            boolean rBool1 = false;
            boolean rBool2 = false;
            boolean nBool1 = false;
            boolean nBool2 = false;
            int length = inputStream.read(bytes);
            int i=0;
            String line = "";
            while((length) != -1) {
                byte b = bytes[0];
                if (b == 13 && !rBool1) {
                    rBool1 = true;
                } else if (b == 10 && rBool1 && !nBool1) {
                    nBool1 = true;
                } else if (b == 13 && rBool1 && nBool1 && !rBool2) {
                    rBool2 = true;
                } else if (b == 10 && rBool1 && nBool1 && rBool2 && !nBool2) {
                    nBool2 = true;
                    line += new String(bytes,StandardCharsets.UTF_8);
                    break;
                } else if (b != 13 || b != 10) {
                    rBool1 = false;
                    rBool2 = false;
                    nBool1 = false;
                    nBool2 = false;
                }

                if (rBool1 && rBool2 && nBool1 && nBool2) {
                    break;
                }
                line += new String(bytes,StandardCharsets.UTF_8);
                length = inputStream.read(bytes);
            }


            return line;
        }



        /**
         * This method sends out a GET command for the given image to the given host,
         * finds the given image inside the input stream and takes the right amount of bytes and writes
         * these bytes into a file that will be stored inside the source folder.
         * @param path
         * @param image
         * @param host
         * @param inputStream
         * @param outToServer
         * @throws IOException
         */
        void getImage(String path,String image, String host, InputStream inputStream,DataOutputStream outToServer) throws IOException {
            if (image.charAt(0) == '/') {
                image = image.substring(1);
            }
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
            // If the image from the server is stored in another folder than the source, a new directory will be created inside source.
            FileOutputStream fos = new FileOutputStream("src/"+image);
            byte [] htmlByte = new byte[4096];
            int chunk =inputStream.read(htmlByte);

            // This loop will keep looping as long as there is data in the input stream and the amount of iterations won't exceed the
            // content length that was found in the header.
            int j = 0;
            while (chunk != -1)  {
                for (byte b: htmlByte) {
                }
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
         * This method searches for the directory of the provided image if it's in another directory
         * and returns the directory of the image.
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
                    if (htmlText.substring(i + 1, i + 1 + searchBegin.length()).equals(searchUpperBegin)
                            || htmlText.substring(i + 1, i + 1 + searchBegin.length()).equals(searchBegin)) {
                        start = i + 1 + searchBegin.length();
                    }
                }
                if (c.equals('<')) {
                    if (htmlText.substring(i + 1, i + 1 + searchEnd.length()).equals(searchUpperEnd)
                            || htmlText.substring(i + 1, i + 1 + searchEnd.length()).equals(searchEnd)) {
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
            String [] strings = request.split("\\s+");
            for (String str: strings){
                args.add(str);
            }
            return args;
        } // str[] strings = lines[0].split("\\s+") + is optioneel voor meerdere

        /**
         * This method takes in a string and checks for "Content-length".
         * If the string does not contain "Content-length" it returns -1,
         * else it returns the integer value for content length.
         * @param line
         * @return
         */
        int findContentLen(String line) {
            String contentLength = "";
            boolean cond = false;
            outerloop:
            for (int i = 0; i< line.length()-15;i++) {
                if (line.substring(i,i+15).equals("Content-Length:")) {
                    for (int j = i+15;j < line.length();j++) {
                        if (!Character.toString(line.charAt(j)).matches("-?\\d+") && cond)
                            break outerloop;
                        if (Character.toString(line.charAt(j)).matches("-?\\d+")) {
                            contentLength += line.charAt(j);
                            cond = true;
                        }
                    }
                }
            }
            if (cond)
                return Integer.parseInt(contentLength);
            else
                return -1;
        }

        public static String getFileExtension(String fullName) {
            String fileName = new File(fullName).getName();
            int dotIndex = fileName.lastIndexOf('.');
            return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
        }

        public static boolean isImage(String tag) {
            String [] imgTags = {"png","jpeg","jpg","img","jfif","exif","tiff","bmp","ppm"
                    ,"pgm","bat"};
            for (String str: imgTags) {
                if (str.equals(tag))
                    return true;
            }
            return false;

        }


    }


}