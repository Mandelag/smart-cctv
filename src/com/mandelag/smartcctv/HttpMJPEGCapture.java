/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mandelag.smartcctv;

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
import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 *
 * @author Keenan
 */
public class HttpMJPEGCapture {
    public static void main(String[] args) throws IOException {
        //http://114.110.17.6:8921/JpegStream.cgi?username=bit&password=guest&channel=1
        /*"http://202.51.112.91:676/image2"*/
//        URL url = new URL("http://114.110.17.6:8924/JpegStream.cgi?username=bit&password=guest&channel=1");
//        URL url = new URL("http://202.51.112.91:676/image2");
        URL url = new URL("http://202.51.112.91:686/mjpg/video.mjpg");
        URLConnection huc = url.openConnection();
        
        //int[] preImg = new int[]{224,0,16,74,70,73,70};
        int[] preImg = new int[]{0xff,0xd8,0xff};
        byte[] preImgByte = new byte[preImg.length];
        for(int i=0; i < preImg.length ; i++) {
                preImgByte[i] = (byte) preImg[i];
        }
        
        try(InputStream is = new BufferedInputStream(huc.getInputStream()); BufferedReader rs = new BufferedReader(new InputStreamReader(is))) {
            Consumer<byte[]> byteConsumer = (b) -> {
                System.out.println("got byte!");
                try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("C:\\Users\\keenan\\extract.jpeg"))){
                    bos.write(preImgByte);
                    bos.write(b);
                }catch(IOException e){
                    System.err.println(e);
                }
            };
            String separator = huc.getHeaderField("content-type").split("boundary=")[1];
            System.out.println(huc.getHeaderFields());
            if(separator.startsWith("--")) {
                //separator = separator.substring(2);
            }
            System.out.println(separator);
            byte[] close = separator.getBytes();//Charset.forName("UTF-8")
            int[] intArray = new int[close.length];
            for(int i=0; i < close.length ; i++) {
                intArray[i] = (int) close[i];
            }
            
            
            InputStreamMarker imageExtract = new InputStreamMarker(preImg, intArray, byteConsumer);
            
            InputStreamExtractor ise = new InputStreamExtractor(is, new InputStreamMarker[]{imageExtract});
            ise.extract();
            int r;
                while( (r = is.read()) >= 0) {
                    System.out.println(r + " " + (char) r);
                }
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
