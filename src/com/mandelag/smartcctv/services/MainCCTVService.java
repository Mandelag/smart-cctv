/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mandelag.smartcctv.services;

import com.mandelag.extractor.InputStreamExtractor;
import com.mandelag.extractor.InputStreamMarker;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Consumer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author Keenan
 */
public class MainCCTVService {
    public static void main(String[] args) throws Exception {
        
        //if(args.length < 3 ) {
        //    System.out.println("    Usage: java -jar com.mandelag.smartcctv.MainCCTVService <ip> <web_port> <cctv_address>");
        //    return;
        //}
        String serverAddress = "localhost";
        String port = "9905";
        String cctv = "http://114.110.17.6:8896/image.jpg?type=motion";
        
        Server server = new Server(new InetSocketAddress(InetAddress.getByName(serverAddress), Integer.parseInt(port)));
        URL cctvUrl = new URL(cctv);
        URLConnection huc = cctvUrl.openConnection();
        
        
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        //context.addServlet("com.mandelag.smartcctv.services.CCTVServlet", "/smartcctv");
        CCTVServlet cs = new CCTVServlet();
        context.addServlet(new ServletHolder(cs), "/smartcctv");
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{context, new DefaultHandler()});
        server.setHandler(handlers);
        
        
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

            System.out.println(separator);
            byte[] close = separator.getBytes();//Charset.forName("UTF-8")
            int[] intArray = new int[close.length];
            for(int i=0; i < close.length ; i++) {
                intArray[i] = (int) close[i];
            }
            
            
            InputStreamMarker imageExtract = new InputStreamMarker(preImg, intArray, cs::receiveImage);
            
            InputStreamExtractor ise = new InputStreamExtractor(is, new InputStreamMarker[]{imageExtract});
            new Thread(() -> {
                try {
                    ise.extract();
                }catch(IOException e) {
                    
                }
            }).start();
            
            server.start();
            server.join();
            
        }catch(IOException e){
            System.err.println(e);
        }
    }
}
