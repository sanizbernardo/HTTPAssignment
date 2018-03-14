import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

class HTTPClient {
    public static void main(String args[]) throws Exception
    {
        if (args.length != 3)
            throw new Exception("Incorrect input");
        String command = args[0], uri = args[1], port = args[2];

        //BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
        //Socket clientSocket = new Socket("www.google.com", 80);
        System.out.println("ARGUMENTEN: "+args[1]+" en 2e: "+args[2]+" en 3e: "+args[0]);
        Socket clientSocket = new Socket(args[1], Integer.valueOf(args[2]));
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        //String sentence = inFromUser.readLine();
        outToServer.writeBytes(args[0] + " /index.html HTTP/1.1\n");
        outToServer.writeBytes("Host: "+args[1]+"\n");
        //outToServer.writeBytes("GET /index.html HTTP/1.1 \n");
        outToServer.writeBytes("\n");
        boolean ended = false;
        String htmlInput = "";
        String substr = "";
        ArrayList<String> images = new ArrayList<String>();
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
            System.out.println("Search for images: "+htmlInput.substring(i,i+10));
            if (htmlInput.substring(i,i+10).equals("<IMG SRC=\"")) {
                for (int j=i+11;j <htmlInput.length()-1;j++) {
                    System.out.println("Search for \"/: "+htmlInput.substring(j,j+2));
                    if (htmlInput.substring(j,j+2).equals("\"/")) {
                        imageEnd = j;
                        System.out.println("Gevonden image: "+htmlInput.substring(i+10,imageEnd));
                        break;
                    }
                }
            }
            if (imageEnd != 0) {
                images.add(htmlInput.substring(i+10,imageEnd));
            }
        }
        System.out.println("Image: "+images+" length: "+images.size());



        File htmlTemplateFile = new File("src/template.html");
        String htmlString = FileUtils.readFileToString(htmlTemplateFile);
        String title = htmlInput.substring(titlestart, titleend);
        String body = htmlInput.substring(bodystart, bodyend);
        htmlString = htmlString.replace("$title", title);
        htmlString = htmlString.replace("$body", body);
        File newHtmlFile = new File("src/new.html");
        FileUtils.writeStringToFile(newHtmlFile, htmlString);


        clientSocket.close();
    }
}