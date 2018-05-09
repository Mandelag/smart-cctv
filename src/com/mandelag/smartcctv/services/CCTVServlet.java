/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mandelag.smartcctv.services;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.nio.charset.Charset;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Keenan
 */
public class CCTVServlet extends HttpServlet {

    MainCCTVService cctvService;
    
    public CCTVServlet(MainCCTVService cctvService) {
        this.cctvService = cctvService;
    }
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleImage(response);
    }

    private void printDefault(HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().append("No CCTV found!");
        response.getWriter().flush();
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private void handleImage(HttpServletResponse response) {
        String boundary = "--hehehe";
        byte[] contentType = "\r\nContent-Type: image/jpeg\r\n".getBytes(Charset.forName("UTF-8"));
        byte[] contentLength = "Content-Length: ".getBytes(Charset.forName("UTF-8"));
        byte[] boundaryByte = boundary.getBytes(Charset.forName("UTF-8"));
        response.setContentType("multipart/x-mixed-replace;boundary="+boundary.substring(2));
        response.addHeader("Connection", "Keep-Alive");
        response.addHeader("Keep-Alive", "timeout=60000");

        
        try {
            response.getOutputStream().write(boundaryByte);
        }catch(IOException e){}
        while(true) {
            try {
                Thread.sleep(0);
                try{
                    response.getOutputStream().write(contentType);
                    response.getOutputStream().write(contentLength);
                    response.getOutputStream().write((image.length+"").getBytes(Charset.forName("UTF-8")));
                    //write header...
                    response.getOutputStream().write("\r\n\r\n".getBytes(Charset.forName("UTF-8")));
                    //response.getOutputStream().write(preImgByte);
                    response.getOutputStream().write(image);
                    response.getOutputStream().write(boundaryByte);
                    response.getOutputStream().flush();
                }catch(IOException e){}
            }catch(InterruptedException e){
                
            }
        }   
    }
    private byte[] image;
    private int carCount;
    
    int[] preImg = new int[]{0xff,0xd8,0xff};
    byte[] preImgByte = new byte[preImg.length];
    {for(int i=0; i < preImg.length ; i++) {
        preImgByte[i] = (byte) preImg[i];
    }}
    public void receiveImage(byte[] image) {
        this.image = image;
        Thread.currentThread().interrupt();
    }
    
    public void receiveCarCount(int carCount) {
        this.carCount = carCount;
    }

    @Override
    public void init() {
    }
}
