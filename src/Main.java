import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

class HTTPClient {



    public static void main(String args[]) throws Exception
    {
        /*
         * Variables
         */
        boolean ended = false;
        String substr = "";
        String byteString= "H";
        String fullText;
        String htmlText;

        // Creating byte array for incoming bytes
        byte[] bytes= new byte[100];
        byte [] restBytes;

        // Creating new html template
        File htmlTemplateFile = new File("src/template.html");

        // Creating new array list for images in html file
        ArrayList<String> images = new ArrayList<String>();

        // Checking for correct input of user
        if (args.length != 3)
            throw new Exception("Incorrect input");

        // Initializing the variables from user arguments
        String HTTPCommand = args[0];
        URI uri = new URI(args[1]);
        String path = uri.getPath();
        int port = Integer.valueOf(args[2]);

        /*
         * Connection
         */

        // Creating new socket to connect to URI with correct port
        Socket clientSocket = new Socket(path,port);

        // Get the input stream from the clientsocket
        InputStream inputStream = clientSocket.getInputStream();

        // Create output stream for requests to socket
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        // TODO remove this: BufferedReader inFromServer = new BufferedReader(new InputStreamReader(inputStream));
        // TODO String sentence = inFromUser.readLine();

        /*
         * Check for which HTTP command to execute
         */

        // Incoming HTTP command from request is GET, so calling get method with
        // correct path and data output stream
        if (HTTPCommand.equals("GET")) {
            Get(path,outToServer,"/index.html");
        }

        // Incoming HTTP command from request is HEAD
        else if (HTTPCommand.equals("HEAD")) {
            Head(path,outToServer, "/index.html");
        }

        // Incoming HTTP command from request is PUT
        else if (HTTPCommand.equals("PUT")) {
            Put(path,port);
        }

        // Incoming HTTP command from request is POST
        else if (HTTPCommand.equals("POST")) {
            Post(path,port);
        }

        /*
         * Convert the bytes from input stream to string
         */

        //Read from input stream
        inputStream.read();

        //Calling up the method to cast bytes of input stream to string
       fullText = byteToString("H",bytes,inputStream);

       //Sometimes the inputStream.available() does not see all the data immediately, so we check if we have all the
       //data for our html file with ends in </HTML> or </html>, if the String does not contain one of these two elements
       // then we keep converting bytes of inputStream to String and add the result onto the existing string until it contains one
       // of the two html elements.
        if (HTTPCommand.equals("GET")) {
            while (!(fullText.contains("</HTML>") || fullText.contains("</html>"))) {
                fullText += byteToString("",bytes, inputStream);
            }
        }

        //Find the html text part of the response from the HTTP Command
        htmlText = searchForHtml(fullText);

        /*
         * Parsing
         */

        //This part finds the title part, body part and the images of the html text part
        //For the title part it looks for the title tags and remembers the start and end index of the title.
        //For the body part it works the same as the title part.
        //For the images this function searches for the image tags and remembers the name + extensions of the found images.
        int titlestart=0, titleend=0, bodystart=0, bodyend=0;
        for (int i=0; i < htmlText.toCharArray().length - 9; i++) {
            Character c = (htmlText.charAt(i));
            int imageEnd = 0;
            if (c.equals('<')) {
                if (htmlText.substring(i+1,i+7).equals("TITLE>") || htmlText.substring(i+1,i+7).equals("title>")) {
                    titlestart = i+7;
                }
            }
            if (c.equals('<')) {
                if (htmlText.substring(i+1,i+8).equals("/TITLE>") || htmlText.substring(i+1,i+8).equals("/title>")) {
                    titleend = i;
                }
            }
            if (c.equals('<')) {
                if (htmlText.substring(i+1,i+6).equals("BODY>") || htmlText.substring(i+1,i+6).equals("body>")) {
                    bodystart = i+6;
                }
            }
            if (c.equals('<')) {
                if (htmlText.substring(i+1,i+7).equals("/BODY>") || htmlText.substring(i+1,i+7).equals("/body>")) {
                    bodyend = i;
                }
            }
            if (htmlText.substring(i,i+10).equals("<IMG SRC=\"")) {
                for (int j=i+11;j <htmlText.length()-1;j++) {
                    if (htmlText.substring(j,j+1).equals("\"") || htmlText.substring(j,j+1).equals("\"")) {
                        imageEnd = j;
                        break;
                    }
                }
            }
            if (imageEnd != 0) {
                images.add(htmlText.substring(i+10,imageEnd));
            }
        }
        System.out.println("Image: "+images+" length: "+images.size());

        /*
         * Convert bytes to images
         */

        if (images.size() != 0) {
            ImageInputStream iis = ImageIO.createImageInputStream(inputStream);
            outToServer.writeBytes("GET /faq.png HTTP/1.1\n");
            outToServer.writeBytes("Host: tinyos.net \n");
            outToServer.writeBytes("\n");
            byte[] buffer = new byte[4096];
            BufferedImage image = ImageIO.read(iis);
            System.out.println("Image: "+image);
            JFrame frame = new JFrame();
            JLabel label = new JLabel(new ImageIcon(image));
            frame.getContentPane().add(label, BorderLayout.CENTER);
            frame.pack();
            frame.setVisible(true);
            for (int i=0;i <images.size();i++) {

                //inputStream.read(buffer);
                //BufferedImage bImageFromConvert = ImageIO.read(inputStream);
                //ImageIO.write(bImageFromConvert, "jpg", new File(
                 //       "c:/new-darksouls.jpg"));

                System.out.println("Buffer: "+buffer.length);
                ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
                BufferedImage bImage2 = ImageIO.read(bis);
                System.out.println("bImage2: "+bImage2);
                ImageIO.write(bImage2, "jpg", new File("output.jpg") );
            }
        }

        //We print the header of the response of the HTTP Command to the terminal:
        System.out.println(searchForHeader(fullText));


        String htmlString = FileUtils.readFileToString(htmlTemplateFile);
        String title = htmlText.substring(titlestart, titleend);
        String body = htmlText.substring(bodystart, bodyend);
        String header= ""; //TODO
        htmlString = htmlString.replace("$header",header);
        htmlString = htmlString.replace("$title", title);
        htmlString = htmlString.replace("$body", body);
        File newHtmlFile = new File("src/new.html");
        FileUtils.writeStringToFile(newHtmlFile, htmlString);


        clientSocket.close();
    }

    /**
     * This method sends the HTTP command GET out to the server with the provided path and resource.
     * This is all done in HTTP 1.1
     * @param path
     * @param outToServer
     * @param resource
     * @throws IOException
     */
    public static void Get(String path,DataOutputStream outToServer,String resource) throws IOException
    {
        outToServer.writeBytes("GET "+resource+" HTTP/1.1\n");
        outToServer.writeBytes("Host: "+path+" \n");
        outToServer.writeBytes("\n");
    }

    public static void Head(String path, DataOutputStream outToServer,String resource) throws IOException
    {
        outToServer.writeBytes("HEAD "+resource+" HTTP/1.1\n");
        outToServer.writeBytes("Host: "+path+" \n");
        outToServer.writeBytes("\n");
    }

    public static void Put(String path, int port)
    {
        //String userInput = System.in;
    }

    public static void Post(String path, int port)
    {

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
    public static String byteToString(String byteString,byte[] bytes,InputStream inputStream) throws IOException
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

    public static String searchForHeader(String fullText) {
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

    public static String searchForHtml(String fullText)
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

}