import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.*;

class HTTPClient {
    public static void main(String args[]) throws Exception
    {
        //if (args.length != 3)
        //    throw new Exception("Incorrect input");
        //    System.exit(-1);
        //String command = args[0], uri = args[1], port = args[2];

        BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
        Socket clientSocket = new Socket("www.google.com", 80);
        //Socket clientSocket = new Socket(args[1], Integer.valueOf(args[2]));
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        //String sentence = inFromUser.readLine();
        //outToServer.writeBytes(args[0] + " " + sentence + "\n");
        outToServer.writeBytes("GET /index.html HTTP/1.1 \n");
        outToServer.writeBytes("\n");
        boolean ended = false;
        while (!ended) {
            String line = inFromServer.readLine();
            System.out.println(line);
            if (line.equals("</BODY></HTML>")) {
                ended = true;
            }
        }

        int titlestart=0, titleend=0, bodystart=0, bodyend=0;
        for (int i=0; i < htmlString.toCharArray().length - 8; i++) {
            Character c = ((Character)htmlString.charAt(i));

            if (c.equals('<')) {
                //System.out.println("< found" + htmlString.substring(i+1,i+6));
                if (htmlString.substring(i+1,i+7).equals("title>")) {
                    titlestart = i+7;
                    System.out.println(htmlString.charAt(titlestart));
                }
            }
            if (c.equals('<')) {
                //System.out.println("< found");
                if (htmlString.substring(i+1,i+8).equals("/title>")) {
                    titleend = i-1;
                    System.out.println(htmlString.charAt(titleend));
                }
            }
            if (c.equals('<')) {
                //System.out.println("< found");
                if (htmlString.substring(i+1,i+6).equals("body>")) {
                    bodystart = i+6;
                    System.out.println(htmlString.charAt(bodystart));
                }
            }
            if (c.equals('<')) {
                //System.out.println("< found");
                if (htmlString.substring(i+1,i+7).equals("/body>")) {
                    bodyend = i-1;
                    System.out.println(htmlString.charAt(bodyend));
                }
            }
        }
        System.out.println(titlestart + " " + titleend + " "+bodystart+" "+ bodyend);



        File htmlTemplateFile = new File("template.html");
        String htmlString = FileUtils.readFileToString(htmlTemplateFile);
        String title = htmlString.substring(titlestart, titleend);
        String body = htmlString.substring(bodystart, bodyend);
        htmlString = htmlString.replace("$title", title);
        htmlString = htmlString.replace("$body", body);
        File newHtmlFile = new File("new.html");
        FileUtils.writeStringToFile(newHtmlFile, htmlString);


        clientSocket.close();
    }
}