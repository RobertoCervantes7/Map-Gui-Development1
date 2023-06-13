import org.openstreetmap.gui.jmapviewer.*;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;
import java.awt.geom.AffineTransform;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * The Driver class is responsible for executing the main program flow.
 * It imports necessary libraries, sets up the graphical user interface (GUI),
 * and provides methods for plotting a trip on a map.
 */
public class Driver 
{
    /**
     * The animationSec field represents the duration in seconds for the trip animation.
     */
    public static int animationSec;
    
    /**
     * The trip field stores an ArrayList of TripPoint objects representing the trip data.
     */
    public static ArrayList<TripPoint> trip;
    
    /**
     * The arrow field stores a BufferedImage object representing an arrow image.
     */
    public static BufferedImage arrow;

    /**
     * The main method is the entry point of the program.
     * It initializes the GUI, sets up event listeners, and starts the program execution.
     *
     * @param args the command-line arguments
     * @throws FileNotFoundException if the required file is not found
     * @throws IOException if an I/O error occurs during file reading
     */
    public static void main(String[] args) throws FileNotFoundException, IOException 
    {
        TripPoint.readFile("triplog.csv");
        TripPoint.h2StopDetection();
        arrow = ImageIO.read(new File("arrow.png"));
        
        // Set up frame
        JFrame frame = new JFrame("Map Viewer");
        frame.setLayout(new BorderLayout());
        
        // Set up top panel for input selections
        JPanel topPanel = new JPanel();
        frame.add(topPanel, BorderLayout.NORTH);
        
        // Play button
        JButton play = new JButton("Play");
        
        // Checkbox to enable/disable stops
        JCheckBox includeStops = new JCheckBox("Include Stops");
        
        // Dropdown box to pick animation time
        String[] timeList = {"Animation Time", "15", "30", "60", "90"};
        JComboBox<String> animationTime = new JComboBox<String>(timeList);
        animationSec = 0;
        
        // Add components to top panel
        topPanel.add(animationTime);
        topPanel.add(includeStops);
        topPanel.add(play);
        
        // Set up mapViewer
        JMapViewer mapViewer = new JMapViewer();
        frame.add(mapViewer);
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mapViewer.setTileSource(new OsmTileSource.TransportMap());
        
        // Add listeners
        play.addActionListener(new ActionListener() 
        {
            public void actionPerformed(ActionEvent e) 
            {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() 
                {
                    @Override
                    protected Void doInBackground() throws Exception 
                    {
                        mapViewer.removeAllMapMarkers(); // Remove all markers from the map
                        mapViewer.removeAllMapPolygons();
                        
                        if (includeStops.isSelected()) 
                        {
                            trip = TripPoint.getTrip();
                        }
                        else 
                        {
                            trip = TripPoint.getMovingTrip();
                        }
                        
                        plotTrip(animationSec, trip, mapViewer);
                        return null;
                    }
                };
                worker.execute();
            }
        });
        
        animationTime.addItemListener(new ItemListener() 
        {
            @Override
            public void itemStateChanged(ItemEvent e) 
            {
                if (e.getStateChange() == ItemEvent.SELECTED) 
                {
                    Object selectedItem = animationTime.getSelectedItem();
                    if (selectedItem instanceof String) 
                    {
                        String selectedString = (String) selectedItem;
                        if (!selectedString.equals("Animation Time"))
                        {
                            animationSec = Integer.parseInt(selectedString);
                            System.out.println("Updated to " + animationSec);
                        }
                    }
                }
            }
        });

        // Set the map center and zoom level
        mapViewer.setDisplayPosition(new Coordinate(34.82, -107.99), 6);
    }
    
    /**
     * The plotTrip method plots the trip on the map with animation.
     *
     * @param seconds the duration in seconds for the animation
     * @param trip the ArrayList of TripPoint objects representing the trip data
     * @param map the JMapViewer object representing the map
     * @throws IOException if an I/O error occurs during image reading
     */
    public static void plotTrip(int seconds, ArrayList<TripPoint> trip, JMapViewer map) throws IOException {
        long delayTime = (seconds * 1000) / trip.size();

        Coordinate c1;
        Coordinate c2 = null;
        IconMarker marker;
        IconMarker prevMarker = null;
        MapPolygonImpl line;

        for (int i = 0; i < trip.size(); ++i) {
            c1 = new Coordinate(trip.get(i).getLat(), trip.get(i).getLon());
            if (i != 0) {
                c2 = new Coordinate(trip.get(i - 1).getLat(), trip.get(i - 1).getLon());
            }
            double angle = 0;
            if (c2 != null) {
                angle = Math.toDegrees(Math.atan2(c1.getLat() - c2.getLat(), c1.getLon() - c2.getLon()));
                angle = (angle + 360) % 360; // Make sure the angle is positive
                BufferedImage rotatedImage = rotateImageByDegrees(arrow, -angle + 90);
                marker = new IconMarker(c1, rotatedImage);
            } else {
                marker = new IconMarker(c1, arrow);
            }

            map.addMapMarker(marker);
            if (c2 != null) {
                line = new MapPolygonImpl(c1, c2, c2);
                line.setColor(Color.RED);
                line.setStroke(new BasicStroke(3));
                map.addMapPolygon(line);
                map.removeMapMarker(prevMarker);
            }
            try {
                Thread.sleep(delayTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            prevMarker = marker;
        }
    }

    /**
     * The rotateImageByDegrees method rotates an image by the specified angle in degrees.
     *
     * @param img the BufferedImage object to rotate
     * @param angle the angle in degrees to rotate the image
     * @return the rotated BufferedImage object
     */
    public static BufferedImage rotateImageByDegrees(BufferedImage img, double angle) 
    {
        double rads = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rads));
        double cos = Math.abs(Math.cos(rads));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotater = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotater.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - w) / 2, (newHeight - h) / 2);
        at.rotate(rads, w / 2, h / 2);
        g2d.setTransform(at);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        return rotater;
    }
}
