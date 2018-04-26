/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javafxapplication2;

import com.mandelag.extractor.InputStreamExtractor;
import com.mandelag.extractor.InputStreamMarker;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Consumer;

/**
 *
 * @author Keenan
 */
public class HttpMJPEGCapture {
    public static void main(String[] args) throws IOException {
        //http://114.110.17.6:8921/JpegStream.cgi?username=bit&password=guest&channel=1
        URL url = new URL("http://114.110.17.6:8924/JpegStream.cgi?username=bit&password=guest&channel=1");
        URLConnection huc = url.openConnection();
        
        try(InputStream is = new BufferedInputStream(huc.getInputStream()); BufferedReader rs = new BufferedReader(new InputStreamReader(is))) {
            Consumer<byte[]> byteConsumer = (b) -> {
                try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("C:\\Users\\keenan\\extract.jpeg"))){
                    bos.write(new byte[]{(byte)255,(byte)216,(byte)255,(byte)224});
                    bos.write(b);
                    bos.flush();
                }catch(IOException e){
                    System.err.println(e);
                }
            };
            String separator = huc.getHeaderField("content-type").split("boundary=")[1];
            byte[] close = separator.getBytes();
            int[] intArray = new int[close.length];
            for(int i=0; i < close.length ; i++) {
                intArray[i] = (int) close[i];
            }
            InputStreamMarker imageExtract = new InputStreamMarker(new int[]{255,216,255,224}, intArray, byteConsumer);
            
            InputStreamExtractor ise = new InputStreamExtractor(is, new InputStreamMarker[]{imageExtract});
            ise.extract();
            /*
            try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("C:\\Users\\keenan\\stream.jpeg"))){
                int r ;
                while( (r = is.read()) >= 0) {
                    bos.write(r);
                    bos.flush();
                }
            }catch(IOException e){
                    System.err.println(e);
            }
            */

        }catch(IOException e){
            System.err.println(e);
        }
    }
    
}
