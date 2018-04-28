package com.mandelag.smartcctv;

import com.mandelag.extractor.InputStreamExtractor;
import com.mandelag.extractor.InputStreamMarker;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
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
import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

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
public class FXHelloCVController implements Initializable
{
	// the FXML button
	@FXML
	private Button button;
	// the FXML image view
	@FXML
	private ImageView currentFrame;
	
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	// the id of the camera to be used
	private static int cameraId = 0;
	
        private String cctvUrl ="http://202.51.112.91:676/image2";
        private String objectId = "";
        
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
        protected void startCamera()
	{
		if (!this.cameraActive)
		{
				this.cameraActive = true;
				
				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						try {
                                                URL url = new URL(cctvUrl);
                                                URLConnection huc = url.openConnection();

                                                    try(InputStream is = new BufferedInputStream(huc.getInputStream()); BufferedReader rs = new BufferedReader(new InputStreamReader(is))) {
                                                        int[] magicInt = {0xff,0xd8,0xff};
                                                        byte[] magicIntByte = new byte[magicInt.length];
                                                        for(int i=0; i < magicInt.length ; i++) {
                                                                magicIntByte[i] = (byte) magicInt[i];
                                                        }
                                                        Consumer<byte[]> byteConsumer = (b) -> {
                                                            try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
                                                                bos.write(magicIntByte);
                                                                bos.write(b);
                                                                bos.flush();

                                                                byte[] imageBytes = bos.toByteArray();

                                                                Mat frame = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);//CV_LOAD_IMAGE_UNCHANGED
                                                                //Mat rot = Imgproc.getRotationMatrix2D(new Point(frame.width()/2, frame.height()/2), 21, 1);
                                                                //Imgproc.warpAffine(frame, frame, rot, new Size(frame.width(), frame.height()));
                                                                MatOfRect matOfRect = new MatOfRect();
                                                                carsClassifier.detectMultiScale(frame, matOfRect, 1.4, 0, 0, new Size(30, 30), new Size(65,65));
                                                                //System.out.print(matOfRect.dump());
                                                                Rect[] t = matOfRect.toArray();
                                                                if(t.length > 0) {
                                                                    System.out.println(t.length + " car(s) detected.");
                                                                    for(int i=0; i<t.length; i++) {
                                                                    Imgproc.rectangle(frame, new Point(t[i].x, t[i].y), 
                                                                        new Point(t[i].x+t[i].width-1, t[i].y+t[i].height-1), 
                                                                        new Scalar(255,255,0));
                                                                    }
                                                                }                                                                
                                                                Image imageToShow = Utils.mat2Image(frame);
                                                                updateImageView(currentFrame, imageToShow);

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
                                                        InputStreamMarker imageExtract = new InputStreamMarker(magicInt, intArray, byteConsumer);

                                                        InputStreamExtractor ise = new InputStreamExtractor(is, new InputStreamMarker[]{imageExtract});
                                                        ise.extract();
                                                    }catch(IOException e){
                                                        System.err.println(e);
                                                    }
                                                }catch(IOException e){
                                                    System.err.println(e);
                                                }
					}
                                        
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
				// update the button content
				this.button.setText("Stop Camera");

		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.button.setText("Start Camera");
			
			// stop the timer
			this.stopAcquisition();
		}
	}
        
	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the {@link Mat} to show
	 */
	private Mat grabFrame()
	{
		// init everything
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frame);
				
				// if the frame is not empty, process it
				if (!frame.empty())
				{
					//Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
				}
				
			}
			catch (Exception e)
			{
				// log the error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		
		return frame;
	}
	
	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition()
	{
		if (this.timer!=null && !this.timer.isShutdown())
		{
			try
			{
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}
		
		if (this.capture.isOpened())
		{
			// release the camera
			this.capture.release();
		}
	}
	
	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image)
	{
		Utils.onFXThread(view.imageProperty(), image);
	}
	
	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed()
	{
		this.stopAcquisition();
	}

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        startCamera();
    }//ActionEvent event
	
}