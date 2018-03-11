import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.*;

class HTTPClient {
    public static void main(String args[]) throws Exception
    {
        if (args.length != 3)
            throw new Exception("Incorrect input");
        String command = args[0], uri = args[1], port = args[2];

        //BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
        //Socket clientSocket = new Socket("www.google.com", 80);
        Socket clientSocket = new Socket(args[1], Integer.valueOf(args[2]));
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        //String sentence = inFromUser.readLine();
        outToServer.writeBytes(args[0] + " /index.html HTTP/1.1\n");
        //outToServer.writeBytes("GET /index.html HTTP/1.1 \n");
        outToServer.writeBytes("\n");
        boolean ended = false;
        String htmlInput = "";
        while (!ended) {
            String line = inFromServer.readLine();
            System.out.println(line);
            if (line.equals("</BODY></HTML>")) {
                ended = true;
            }
            htmlInput += line;
        }
        //System.out.println(htmlInput);

        int titlestart=0, titleend=0, bodystart=0, bodyend=0;
        for (int i=0; i < htmlInput.toCharArray().length - 9; i++) {
            Character c = (htmlInput.charAt(i));

            if (c.equals('<')) {
                //System.out.println("< found"+" "+htmlInput.substring(i+1,i+7));
                if (htmlInput.substring(i+1,i+7).equals("TITLE>")) {
                    titlestart = i+7;
                    //System.out.println(htmlInput.charAt(titlestart));
                }
            }
            if (c.equals('<')) {
                if (htmlInput.substring(i+1,i+8).equals("/TITLE>")) {
                    titleend = i-1;
                }
            }
            if (c.equals('<')) {
                if (htmlInput.substring(i+1,i+6).equals("BODY>")) {
                    bodystart = i+6;
                }
            }
            if (c.equals('<')) {
                if (htmlInput.substring(i+1,i+7).equals("/BODY>")) {
                    bodyend = i-1;
                }
            }
        }



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