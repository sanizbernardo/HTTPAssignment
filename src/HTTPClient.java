import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

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
            while (true) {
                try {
                    /*
                     * Variables
                     */
                    Scanner scanner = new Scanner(System.in);
                    String fullText;
                    String htmlText;
                    String request;

                    // Creating byte array for incoming bytes
                    byte[] bytes = new byte[100];

                    /*
                     * Check for which HTTP command to execute
                     */

                    // Incoming HTTP command from request is GET, so calling get method with
                    // correct path and data output stream
                    switch (HTTPCommand) {
                        case "HEAD":
                            Head(path, outToServer, "/");
                            break;
                        case "GET":
                            Get(path, outToServer, "/");
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

                    //Read from input stream
                    inputStream.read();

                    //Calling up the method to cast bytes of input stream to string
                    fullText = byteToString("H", bytes, inputStream);

                    //Sometimes the inputStream.available() does not see all the data immediately, so we check if we have all the
                    //data for our html file with ends in </HTML> or </html>, if the String does not contain one of these two elements
                    // then we keep converting bytes of inputStream to String and add the result onto the existing string until it contains one
                    // of the two html elements.
                    if (HTTPCommand.equals("GET")) {
                        while (!(fullText.contains("</HTML>") || fullText.contains("</html>"))) {
                            fullText += byteToString("", bytes, inputStream);
                        }
                    }

                    //Find the html text part of the response from the HTTP Command
                    htmlText = searchForHtml(fullText);

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
                        System.out.println(searchForHeader(fullText));
                    else if (HTTPCommand.equals("HEAD"))
                        System.out.println(fullText);


                    // Use the found parameters of the title and body part of the html part and take these out of the string so we can
                    // replace the title and body part of the html template to create our own template.
                    String htmlString = FileUtils.readFileToString(htmlTemplateFile);
                    String title = htmlText.substring(titleStart, titleEnd);
                    String body = htmlText.substring(bodyStart, bodyEnd);
                    htmlString = htmlString.replace("$title", title);
                    htmlString = htmlString.replace("$body", body);
                    File newHtmlFile = new File("src/new.html"+counter);
                    FileUtils.writeStringToFile(newHtmlFile, htmlString);

                    if ((request = scanner.next()).length() ==0)
                        break;
                    counter ++;


                } catch (IOException e) {

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
        public void Get(String host,DataOutputStream outToServer,String resource) throws IOException
        {
            outToServer.writeBytes("GET "+resource+" HTTP/1.1\r\n");
            outToServer.writeBytes("Host: "+host+"\r\n");
            outToServer.writeBytes("\r\n");
        }

        /**
         * This method sends the HTTP command HEAD out to the server with the provided
         * @param host
         * @param outToServer
         * @param resource
         * @throws IOException
         */
        public void Head(String host, DataOutputStream outToServer,String resource) throws IOException
        {
            outToServer.writeBytes("HEAD "+resource+" HTTP/1.1\r\n");
            outToServer.writeBytes("Host: "+host+"\r\n");
            outToServer.writeBytes("\r\n");
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
        public void PutOrPost(String putOrPost,String path, DataOutputStream outToServer) throws IOException {
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
        }

        /**
         * This method deletes a given file from the server.
         * @param host
         * @param outToServer
         * @param file
         * @throws IOException
         */
        public void Delete(String host, DataOutputStream outToServer, String file) throws IOException {
            outToServer.writeBytes("DELETE /"+file+" HTTP/1.1");
            outToServer.writeBytes("Host: "+host+"\r\n");
            outToServer.writeBytes("\r\n");
        }

        /**
         * This method contains a while loop that checks whether the available data in the input stream is larger than
         * 100 bytes, if this is the case then the method will convert bytes of the input stream in blocks of 100 bytes
         * to string. If the available data in the input stream is smaller than 100 bytes, it will take the
         * remaining bytes left in the input stream and turn those remaining bytes into a string and add it onto the
         * longer string that was created during the while loop.
         * @param byteString
         * @param bytes
         * @param inputStream
         * @return
         * @throws IOException
         */
        public String byteToString(String byteString,byte[] bytes,InputStream inputStream) throws IOException
        {
            byte[] restBytes;
            //Continue doing so until the amount of data in input stream is less than 100 bytes
            while (inputStream.available() > 100) {
                inputStream.read(bytes, 0, 100);
                byteString += new String(bytes, StandardCharsets.UTF_8);;
                bytes = new byte[100];
            }

            // The remaining bytes in input stream smaller than 100 is put into 'rest'
            // These remaining bytes will be cast to string and added onto the whole string that was created in the while loop above.
            int rest = inputStream.available();
            restBytes = new byte[rest];
            inputStream.read(restBytes, 0, rest);
            byteString += new String(restBytes, StandardCharsets.UTF_8);
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

        /**
         * This method searches for the html text of response of the HTTP Command, the response contains
         * a header part and a http text part.
         * @param fullText
         * @return
         */

        public String searchForHtml(String fullText)
        {
            String html = "";
            for (int i=0;i <fullText.length();i++) {
                if (fullText.substring(0, i).contains("<HTML>") || fullText.substring(0, i).contains("<html>")) {
                    html = fullText.substring(i - 6, fullText.length());
                    break;
                }
            }
            return html;
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
        public void getImage(String image, String host, InputStream inputStream,DataOutputStream outToServer) throws IOException {
            // Initialize some variables that will be needed in this method
            byte [] bytes = new byte [1];
            // Send out the GET request to the server
            Get(host,outToServer,"/"+image);

            // This part of the method checks for the combination of bytes 13 10 13 10 in this order. This combination
            // signals for the end of the header and the begin of the actual response from the GET command.
            int length;
            String line = "";
            String contentLength = "";
            boolean rBool1 = false;
            boolean rBool2 = false;
            boolean nBool1 = false;
            boolean nBool2 = false;
            while((length = inputStream.read(bytes)) != -1) {
                line += new String(bytes,0,length);
                for (byte b: bytes){
                    if (b == 13)
                        rBool1 = true;
                    if (b == 10 && rBool1)
                        nBool1 = true;
                    if (b == 13 && rBool1 && nBool1)
                        rBool2 = true;
                    if (b == 10 && rBool1 && nBool1 && rBool2)
                        nBool2 = true;
                    if (b != 13 && b != 10) {
                        rBool1 = false;
                        rBool2 = false;
                        nBool1 = false;
                        nBool2 = false;
                    }
                }
                if (rBool1 && rBool2 && nBool1 && nBool2)
                    break;
            }

            // This part searches for the content length in the header.
            outerloop:
            for (int i = 0; i< line.length()-15;i++) {
                if (line.substring(i,i+15).equals("Content-Length:")) {
                    for (int j = i+15;j < line.length();j++) {
                        if (line.charAt(j) == 'C')
                            break outerloop;
                        if (Character.toString(line.charAt(j)).matches("-?\\d+")) {
                            contentLength += line.charAt(j);
                        }
                    }
                }
            }

            // If the image from the server is stored in another folder than the source, a new directory will be created inside source.
            int j = 0;
            if (detectPathImg(image) != null) {
                new File(detectPathImg("src/"+image)).mkdirs();
            }

            FileOutputStream fos = new FileOutputStream("src/"+image);
            int contentLen = Integer.parseInt(contentLength);
            byte [] htmlByte = new byte[4096];
            int chunk =inputStream.read(htmlByte);

            // This loop will keep looping as long as there is data in the input stream and the amount of iterations won't exceed the
            // content length that was found in the header.
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
        public ArrayList<String> getImages(String html) {
            ArrayList<String> images = new ArrayList<>();
            Document doc = Jsoup.parse(html);

            // Get all img tags
            Elements img = doc.getElementsByTag("img");

            // Loop through img tags
            for (Element el : img) {
                images.add(el.attr("src"));
                images.add(el.attr("lowsrc"));
            }
            return images;
        }

        /**
         * This method searches for the directory of the provided image if it's in another directory and returns the directory of the image.
         * @param image
         * @return
         */
        public String detectPathImg(String image) {
            String [] str = new String [2];
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

        public int[] findBeginEndIndex(String htmlText, String searchBegin, String searchEnd) {
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




    }


}