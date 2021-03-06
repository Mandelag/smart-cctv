package com.mandelag.smartcctv;

import com.mandelag.extractor.InputStreamExtractor;
import com.mandelag.extractor.InputStreamMarker;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;

/**
 * The controller for our application, where the application logic is
 * implemented. It handles the button for starting/stopping the camera and the
 * acquired video stream.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @author <a href="http://max-z.de">Maximilian Zuleger</a> (minor fixes)
 * @version 2.0 (2016-09-17)
 * @since 1.0 (2013-10-20)
 *
 */
public class FXHelloCVController implements Initializable {

    // the FXML image view
    @FXML
    private ImageView currentFrame;

    // the id of the camera to be used
    private static int cameraId = 0;

    private String cctvUrl = "http://202.51.112.91:676/image2";
    private String objectId = "";
    private InetAddress groupAddress;
    private DatagramSocket ds;

    private static CascadeClassifier carsClassifier = new CascadeClassifier();

    static {
        carsClassifier.load("C:\\Users\\keenan\\Documents\\NetBeansProjects\\JavaFXApplication2\\src\\res\\cars.xml");
    }

    public FXHelloCVController(String cctvUrl, String objectId) {
        if (cctvUrl != null) {
            this.cctvUrl = cctvUrl;
            this.objectId = objectId;
        }
    }

    @FXML
    protected void startCamera() {
        Runnable frameGrabber = new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try {
                        URL url = new URL(cctvUrl);
                        URLConnection huc = url.openConnection();

                        try (InputStream is = new BufferedInputStream(huc.getInputStream()); BufferedReader rs = new BufferedReader(new InputStreamReader(is))) {
                            int[] magicInt = {0xff, 0xd8, 0xff};
                            byte[] magicIntByte = new byte[magicInt.length];
                            for (int i = 0; i < magicInt.length; i++) {
                                magicIntByte[i] = (byte) magicInt[i];
                            }
                            Consumer<byte[]> byteConsumer = (b) -> {
                                try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                    bos.write(magicIntByte);
                                    bos.write(b);
                                    bos.flush();

                                    byte[] imageBytes = bos.toByteArray();

                                    Mat frame = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);//CV_LOAD_IMAGE_UNCHANGED
                                    //Mat rot = Imgproc.getRotationMatrix2D(new Point(frame.width()/2, frame.height()/2), 21, 1);
                                    //Imgproc.warpAffine(frame, frame, rot, new Size(frame.width(), frame.height()));
                                    MatOfRect matOfRect = new MatOfRect();
                                    carsClassifier.detectMultiScale(frame, matOfRect, 1.4, 0, 0, new Size(30, 30), new Size(65, 65));
                                    //System.out.print(matOfRect.dump());
                                    Rect[] t = matOfRect.toArray();
                                    if (t.length > 0) {
                                        System.out.println(t.length + " car(s) detected.");
                                        for (int i = 0; i < t.length; i++) {
                                            Imgproc.rectangle(frame, new Point(t[i].x, t[i].y),
                                                    new Point(t[i].x + t[i].width - 1, t[i].y + t[i].height - 1),
                                                    new Scalar(255, 255, 0));
                                        }
                                    }
                                    Image imageToShow = Utils.mat2Image(frame);
                                    updateImageView(currentFrame, imageToShow);
                                    //MatOfByte buf = new MatOfByte();
                                    //Imgcodecs.imencode(".JPEG", frame, buf);
                                    //sendData(buf.toArray());
                                    
                                } catch (IOException e) {
                                    System.err.println(e);
                                }
                            };
                            String separator = huc.getHeaderField("content-type").split("boundary=")[1];
                            byte[] close = separator.getBytes();
                            int[] intArray = new int[close.length];
                            for (int i = 0; i < close.length; i++) {
                                intArray[i] = (int) close[i];
                            }
                            InputStreamMarker imageExtract = new InputStreamMarker(magicInt, intArray, byteConsumer);

                            InputStreamExtractor ise = new InputStreamExtractor(is, new InputStreamMarker[]{imageExtract});
                            ise.extract();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                }
            }
        };
        new Thread(frameGrabber).start();
    }
    
    private void sendData(byte[] data) {
        try {
            if(ds == null) {
                ds = new DatagramSocket(6012);
            }
            groupAddress = InetAddress.getByName("224.6.12.1");
            DatagramPacket dp = new DatagramPacket(data, data.length, groupAddress, 6006);
            ds.send(dp);
        } catch (IOException ex) {
            Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if(ds != null) {
                ds.close();
            }
        }
        
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view the {@link ImageView} to update
     * @param image the {@link Image} to show
     */
    private void updateImageView(ImageView view, Image image) {
        Utils.onFXThread(view.imageProperty(), image);
    }

    /**
     * On application close, stop the acquisition from the camera
     */
    protected void setClosed() {
        
        System.exit(0);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        startCamera();
    }

}
