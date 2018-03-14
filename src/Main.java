import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

class HTTPClient {
    public static void main(String args[]) throws Exception
    {
        if (args.length != 3)
            throw new Exception("Incorrect input");
        String command = args[0], uri = args[1], port = args[2];
        byte[] bufferText = new byte[4096];

        //BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
        //Socket clientSocket = new Socket("www.google.com", 80);
        System.out.println("ARGUMENTEN: 1e"+args[0]+" en 2e: "+args[1]+" en 3e: "+args[2]);
        Socket clientSocket = new Socket(args[1], Integer.valueOf(args[2]));
        InputStream inputStream = clientSocket.getInputStream();
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        //BufferedReader inFromServer = new BufferedReader(new InputStreamReader(inputStream));
        //String sentence = inFromUser.readLine();
        System.out.println("InputStream Available "+inputStream.available());
        outToServer.writeBytes(args[0] + " /index.html HTTP/1.1\n");
        outToServer.writeBytes("Host: "+args[1]+"\n");
        //outToServer.writeBytes("GET /index.html HTTP/1.1 \n");
        outToServer.writeBytes("\n");
        boolean ended = false;
        String htmlInput = "";
        String substr = "";
        inputStream.read(bufferText);
        String str = new String(bufferText);
        System.out.println("string: "+str);
        System.out.println("---------------------");
        ArrayList<String> images = new ArrayList<String>();
        /*while (!ended) {
            String line = inFromServer.readLine();
            //System.out.println(line);
            if (line.length() > 6) {
                substr = line.substring(line.length() - 7, line.length());
            }
            if (substr.equals("</html>") || substr.equals("</HTML>")) {
                ended = true;
            }
            htmlInput += line;
        }*/
        //System.out.println(htmlInput);

        int titlestart=0, titleend=0, bodystart=0, bodyend=0;
        for (int i=0; i < htmlInput.toCharArray().length - 9; i++) {
            Character c = (htmlInput.charAt(i));
            int imageEnd = 0;
            if (c.equals('<')) {
                //System.out.println("< found"+" "+htmlInput.substring(i+1,i+7));
                if (htmlInput.substring(i+1,i+7).equals("TITLE>") || htmlInput.substring(i+1,i+7).equals("title>")) {
                    titlestart = i+7;
                    //System.out.println(htmlInput.charAt(titlestart));
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
            System.out.println("********test***********");
            outToServer.writeBytes("GET /faq.png HTTP/1.1\n");
            outToServer.writeBytes("Host: tinyos.net \n");
            outToServer.writeBytes("\n");
            for (int i=0;i <images.size();i++) {
                byte[] buffer = new byte[4096];
                String img = images.get(i);
                inputStream.read(buffer);
                // for each byte in the buffer
                System.out.println("******************");
                System.out.println(buffer.length);
                /*for(byte b:buffer) {

                    // convert byte to character
                    char c = (char)b;

                    // prints character
                    System.out.print(c);
                }
                System.out.println("\n");*/
                //BufferedImage imageBuf = ImageIO.read(new ByteArrayInputStream(buffer));
                //System.out.println("ImageBuff "+ imageBuf);
                ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
                System.out.println("bis: "+bis);
                BufferedImage bImage2 = ImageIO.read(bis);
                System.out.println("bImage2: "+bImage2);
                ImageIO.write(bImage2, "jpg", new File("output.jpg") );
                System.out.println("image created");
                //System.out.println("Inputstream reading: "+ inputStream.read());
            }
        }

        File htmlTemplateFile = new File("src/template.html");
        String htmlString = FileUtils.readFileToString(htmlTemplateFile);
        String title = htmlInput.substring(titlestart, titleend);
        String body = htmlInput.substring(bodystart, bodyend);
        String header= ""; //TODO
        htmlString = htmlString.replace("$header",header);
        htmlString = htmlString.replace("$title", title);
        htmlString = htmlString.replace("$body", body);
        File newHtmlFile = new File("src/new.html");
        FileUtils.writeStringToFile(newHtmlFile, htmlString);


        clientSocket.close();
    }
}