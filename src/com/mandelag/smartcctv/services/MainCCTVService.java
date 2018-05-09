/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mandelag.smartcctv.services;

import com.mandelag.extractor.InputStreamExtractor;
import com.mandelag.extractor.InputStreamMarker;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Consumer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

/**
 *
 * @author Keenan
 */
public class MainCCTVService {

    private static CascadeClassifier carsClassifier;    
    private int vehicleCount = 0;
    
    public static void main(String[] args) throws Exception {
        if(args.length < 4 ) {
            System.out.println("    Usage: java -jar com.mandelag.smartcctv.MainCCTVService <ip> <port> <cctv_address> <haar_classifier>");
            return;
        }
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        String serverAddress = args[0];
        String port = args[1];
        String cctv = args[2];
        String classifier = args[3];
        MainCCTVService cctvService = new MainCCTVService();
        
    }
    
    public void start(String serverAddress, String port, String  cctv, String classifier) throws Exception{
        carsClassifier = new CascadeClassifier();
        
        carsClassifier.load(classifier);
        System.out.println(classifier);

        Server server = new Server(new InetSocketAddress(InetAddress.getByName(serverAddress), Integer.parseInt(port)));
        URL cctvUrl = new URL(cctv);
        URLConnection huc = cctvUrl.openConnection();

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        CCTVServlet cs = new CCTVServlet(this);
        context.addServlet(new ServletHolder(cs), "/smartcctv");
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{context, new DefaultHandler()});
        server.setHandler(handlers);

        int[] preImg = new int[]{0xff, 0xd8, 0xff};
        byte[] preImgByte = new byte[preImg.length];
        for (int i = 0; i < preImg.length; i++) {
            preImgByte[i] = (byte) preImg[i];
        }

        try (InputStream is = new BufferedInputStream(huc.getInputStream()); BufferedReader rs = new BufferedReader(new InputStreamReader(is))) {

            Consumer<byte[]> imageProcessor = (b) -> {
                byte[] buf = new byte[b.length + 3];
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream(b.length + 3)) {
                    bos.write(preImgByte);
                    bos.write(b);
                    byte[] processedImage = processImage(bos.toByteArray());
                    //byte[] processedImage = bos.toByteArray();
                    cs.receiveImage(processedImage);
                } catch (IOException e) {
                }
            };
            String separator = huc.getHeaderField("content-type").split("boundary=")[1];

            byte[] close = separator.getBytes();//Charset.forName("UTF-8")
            int[] intArray = new int[close.length];
            for (int i = 0; i < close.length; i++) {
                intArray[i] = (int) close[i];
            }

            InputStreamMarker imageExtract = new InputStreamMarker(preImg, intArray, imageProcessor);

            InputStreamExtractor ise = new InputStreamExtractor(is, new InputStreamMarker[]{imageExtract});
            new Thread(() -> {
                try {
                    ise.extract();
                } catch (IOException e) {

                }
            }).start();

            server.start();
            server.join();

        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    private byte[] processImage(byte[] imageBytes) {
        Mat frame = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);//CV_LOAD_IMAGE_UNCHANGED
        //Mat rot = Imgproc.getRotationMatrix2D(new Point(frame.width()/2, frame.height()/2), 21, 1);
        //Imgproc.warpAffine(frame, frame, rot, new Size(frame.width(), frame.height()));
        MatOfRect matOfRect = new MatOfRect();
        carsClassifier.detectMultiScale(frame, matOfRect, 1.4, 0, 0, new Size(30, 30), new Size(100, 100));
        //System.out.print(matOfRect.dump());
        Rect[] t = matOfRect.toArray();
        if (t.length > 0) {
            System.out.println(t.length + " car(s) detected.");
            for (int i = 0; i < t.length; i++) {
                Imgproc.rectangle(frame, new Point(t[i].x, t[i].y),
                        new Point(t[i].x + t[i].width - 1, t[i].y + t[i].height - 1),
                        new Scalar(255, 255, 0), 2);
            }
        }
        vehicleCount = t.length;
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".JPEG", frame, matOfByte);
        frame.release();
        return matOfByte.toArray();
    }
    
    public int getVehicleCount(){
        return this.vehicleCount;
    }
}
