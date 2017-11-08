import javafx.scene.canvas.GraphicsContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

// Class to interface with some structure to load the initial rail configuration
// Should be able to load a file, XML or a hard coded constructor, imo use hard coded at first
public class RailConfigurationLoader
{
    private ArrayList<TrackLine> trackLines;
    private RailConfiguration rc;

    public RailConfigurationLoader(RailConfiguration rc)
    {
        trackLines = new ArrayList<>();
        this.rc = rc;
    }

    // Loads a new file to configure an initial train track/light/switch setup
    public void loadNewConfiguration(String configFileName, GraphicsContext gcDraw)
    {
        trackLines.clear();
        try
        {
            InputStream inputFile = getClass().getResourceAsStream("Configurations/" + configFileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputFile,"UTF-8"));
            String line;
            int trackLineNum = 0; // Tells us what trackLine we are working on. 0 is the first trackLine, 3 is the 4th... etc.
            while((line = reader.readLine()) != null)
            {
                String[] components = line.split(",");
                TrackLine tl = new TrackLine(components, gcDraw, trackLineNum);
                trackLines.add(tl);
                trackLineNum++;
            }
        }
        catch(IOException e) { System.out.println(e.getMessage()); }

        rc.loadTracks(trackLines);
    }



}
