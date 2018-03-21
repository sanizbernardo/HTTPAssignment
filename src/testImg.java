import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class testImg {
    public static void main(String [] args) throws IOException {
        // open image
        File imgPath = new File("src/community.png");
        BufferedImage bufferedImage = ImageIO.read(imgPath);



        BufferedImage originalImage=ImageIO.read(imgPath);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ImageIO.write(originalImage, "png", baos );
        byte[] fileContent=baos.toByteArray();



        //byte[] fileContent = Files.readAllBytes(imgPath.toPath());
        FileOutputStream fos = new FileOutputStream("src/afbeelding.png");
        byte [] htmlByte = new byte[fileContent.length];

        fos.write(htmlByte,0,fileContent.length);


    }
}
