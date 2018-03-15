import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
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
        String htmlInput = "H";
        String substr = "";
        String byteString= "";

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
            Get(path,outToServer);
        }

        // Incoming HTTP command from request is HEAD
        else if (HTTPCommand.equals("HEAD")) {
            Head(path,port);
        }

        // Incoming HTTP command from request is PUT
        else if (HTTPCommand.equals("PUT")) {
            Put(path,port);
        }

        // Incoming HTTP command from request is POST
        else if (HTTPCommand.equals("POST")) {
            Post(path,port);
        }

        //Read from input stream to check amount of data.
        inputStream.read();
        int dataLength = inputStream.available();
        System.out.println("dataLength: "+dataLength);
        String hundredbyte;

        while (inputStream.available() > 100) {
            inputStream.read(bytes, 0, 100);
            hundredbyte = new String(bytes, StandardCharsets.UTF_8);
            byteString += hundredbyte;
            bytes = new byte[100];

        }
        int rest = inputStream.available();
        restBytes = new byte[rest];
        System.out.println("Restbyte: "+restBytes.length);
        inputStream.read(restBytes, 0, rest);
        hundredbyte = new String(restBytes, StandardCharsets.UTF_8);
        byteString += hundredbyte;
        htmlInput += byteString;



/*
        if (inputStream.available() < 100) {
            System.out.println("Available: "+inputStream.available());
            inputStream.read(bytes);
        }
        else {
            System.out.println("Not Available " + inputStream.available());
            int j = 0;
            int dataLength1 = inputStream.available();
            System.out.println("dataLength 2: "+dataLength1);
            int rest = inputStream.available()%bytes.length;
            int dataLength2 = inputStream.available();
            System.out.println("dataLength3: "+dataLength2);
            int bound = (inputStream.available()-rest)/(bytes.length);

            while (j < bound) {
                //System.out.println("Loop print 1");
                inputStream.read(bytes, 0, 100);
                //System.out.println("Loop print 2");

                j += 1;
                System.out.println(j);
                //System.out.println("Loop print 3");
                //System.out.println("HundredByte: "+hundredbyte);

            }
            restBytes = new byte[rest];
            System.out.println("Restbyte: "+restBytes.length);
            inputStream.read(restBytes, 0, rest);
            hundredbyte = new String(restBytes, StandardCharsets.UTF_8);
            System.out.println("HUNDREDSTRING: "+hundredbyte);
            byteString += hundredbyte;

        }
        System.out.println("----------------------------------------------");
        htmlInput += byteString;
        System.out.println("********************************************************");*/
/*
            System.out.println("Modulo: "+(inputStream.available() % 100));
            for (int i = 0;i < (inputStream.available() % 100) ; i++) {
                System.out.println("Not Available "+inputStream.available());
                System.out.println("I: "+i);
                inputStream.read(bytes, i * 100, 100);
                hundredbyte = new String(bytes, StandardCharsets.UTF_8);
                byteString += hundredbyte;
                j += 1;
                bytes = new byte[100];
            }
            inputStream.read(bytes, (j + 1) * 100, inputStream.available());
            byteString += new String(bytes, StandardCharsets.UTF_8);
        }*/
        System.out.println("ByteString: "+htmlInput);

         //Make new byte array with the size of the data and read all the data from input stream to byte array
        /*int size = inputStream.available();
        System.out.println(size);
        byte [] bytes = inputStream.readAllBytes();;//new byte[size];
        System.out.println(inputStream.read(bytes, 0, size));
        int len = inputStream.read(bytes, 0, size);
        System.out.println(len);
        // Cast the bytes read from the input stream to String
        String reqReponse = new String(bytes, StandardCharsets.UTF_8);
        System.out.println(reqReponse);*/

        //
/*
        while (!ended) {
            String line = inFromServer.readLine();
            System.out.println(line);
            if (line.length() > 6) {
                substr = line.substring(line.length() - 7, line.length());
            }
            if (substr.equals("</html>") || substr.equals("</HTML>")) {
                ended = true;
            }
            htmlInput += line;
        }
        //System.out.println(htmlInput);

        int titlestart=0, titleend=0, bodystart=0, bodyend=0;
        for (int i=0; i < htmlInput.toCharArray().length - 9; i++) {
            Character c = (htmlInput.charAt(i));
            int imageEnd = 0;
            if (c.equals('<')) {
                if (htmlInput.substring(i+1,i+7).equals("TITLE>") || htmlInput.substring(i+1,i+7).equals("title>")) {
                    titlestart = i+7;
                }
            }
            if (c.equals('<')) {
                if (htmlInput.substring(i+1,i+8).equals("/TITLE>") || htmlInput.substring(i+1,i+8).equals("/title>")) {
                    titleend = i;
                }
            }
            if (c.equals('<')) {
                if (htmlInput.substring(i+1,i+6).equals("BODY>") || htmlInput.substring(i+1,i+6).equals("body>")) {
                    bodystart = i+6;
                }
            }
            if (c.equals('<')) {
                if (htmlInput.substring(i+1,i+7).equals("/BODY>") || htmlInput.substring(i+1,i+7).equals("/body>")) {
                    bodyend = i;
                }
            }
            if (htmlInput.substring(i,i+10).equals("<IMG SRC=\"")) {
                for (int j=i+11;j <htmlInput.length()-1;j++) {
                    if (htmlInput.substring(j,j+1).equals("\"") || htmlInput.substring(j,j+1).equals("\"")) {
                        imageEnd = j;
                        break;
                    }
                }
            }
            if (imageEnd != 0) {
                images.add(htmlInput.substring(i+10,imageEnd));
            }
        }
        System.out.println("Image: "+images+" length: "+images.size());

        if (images.size() != 0) {
            outToServer.writeBytes("GET /faq.png HTTP/1.1\n");
            outToServer.writeBytes("Host: tinyos.net \n");
            outToServer.writeBytes("\n");
            for (int i=0;i <images.size();i++) {
                byte[] buffer = new byte[4096];
                inputStream.read(buffer);
                ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
                BufferedImage bImage2 = ImageIO.read(bis);
                ImageIO.write(bImage2, "jpg", new File("output.jpg") );
            }
        }

        String htmlString = FileUtils.readFileToString(htmlTemplateFile);
        String title = htmlInput.substring(titlestart, titleend);
        String body = htmlInput.substring(bodystart, bodyend);
        String header= ""; //TODO
        htmlString = htmlString.replace("$header",header);
        htmlString = htmlString.replace("$title", title);
        htmlString = htmlString.replace("$body", body);
        File newHtmlFile = new File("src/new.html");
        FileUtils.writeStringToFile(newHtmlFile, htmlString);

*/
        clientSocket.close();
    }

    public static void Get(String path,DataOutputStream outToServer) throws IOException {
        outToServer.writeBytes("GET /index.html HTTP/1.1\n");
        outToServer.writeBytes("Host: "+path+" \n");
        outToServer.writeBytes("\n");
        outToServer.flush();
    }

    public static void Head(String path, int port) {

    }

    public static void Put(String path, int port) {

    }

    public static void Post(String path, int port) {

    }
}