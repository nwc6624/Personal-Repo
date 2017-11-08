import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Anna Carey on 10/18/17
 *
 * Class for all track pieces. Assumes a train can sit on it and it has neighbors.
 * Will extend thread. Run method will consist of checking for and reacting to messages.
 *
 *
 */

public class RailTrack extends Thread implements IMessagable, IDrawable {
    
    private String NAME;                              // Formal name of the train, useful for trace
    private static int trackIncrement = 1;            // ID number for a given track piece
    private Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();  //list of all messages, held in order of receiving them, to be acknowledged.
    private IMessagable leftNeighbor = null;           //left neighbor 'this' can send and receive messages from
    private IMessagable rightNeighbor = null;          //right neighbor 'this' can send and receive messages from
    private boolean DEBUG = true;                     //turn this flag on to print out a message log.
    private static Image trackImg;                    // Image that we use to draw a track.
    private RailLight trackLight;                     // Light that is affixed on a track.
    private boolean reserved;

    private GraphicsContext gcDraw;
    private int canvasX;
    private int canvasY;

    public RailTrack()
    {
        NAME = "Track" + trackIncrement;
        trackIncrement++;
        // Doing this to save the resources from creating a million images for each track.
        if(trackImg == null) { trackImg = new Image("Track.png"); }
        reserved = true;
    }
    public RailTrack(RailLight trackLight)
    {
        this();
        this.trackLight = trackLight;
    }
    public RailTrack(GraphicsContext gcDraw, int x, int y)
    {
        this();
        this.gcDraw = gcDraw;
        canvasX = x;
        canvasY = y;
    }
    public RailTrack(RailLight trackLight, GraphicsContext gcDraw, int x, int y)
    {
        this(trackLight);
        this.gcDraw = gcDraw;
        canvasX = x;
        canvasY = y;
    }
    
    /**
     * @param left IMessagable piece to the left of this piece. Initialized at runtime.
     * @param right IMessagable piece to the right of this piece. Initialized at runtime.
     *       null if no neighbor or a IMessagable class to which 'this' can pass messages.
     */
    public void setNeighbors(IMessagable left, IMessagable right)
    {
        leftNeighbor = left;
        rightNeighbor = right;
    }
    
    /**
     * @return true if this track has a light.
     */
    public boolean hasLight()
    {
        return trackLight == null;
    }
    
    /**
     * @return the direction in which the light is green.
     *   Returns null if the track has no light.
     */
    public Direction trackLightGreenDirection()
    {
        if(hasLight()) return trackLight.getGreenDirection();
        else return null;
    }
    
    //Reserves the track and (ideally) prevents any other traffic from passing over it.
    private void reserve(Direction trainComingFrom)
    {
        if(trackLight!=null)
        {
            trackLight.reserve(trainComingFrom);
        }
        reserved = true;
    }
    
    private void unreserve()
    {
        reserved = false;
    }
    
    /**
     * Check pendingMessages. If it is empty, wait. (Notify is in the recMessage() method)
     */
    public void run()
    {
        while(true)
        {
            while (!pendingMessages.isEmpty())
            {
                readMessage(pendingMessages.poll());
            }
            //wait
            try
            {
                wait();
            }
            catch(Exception e) {}
        }
    }
    
    /**
     * @param m message
     *  input
     *  first message in pendingMessages.
     *  Any non-null Message.
     *
     *  Parses and acts on the given Message.
     *
     *  HELLOTEST
     *      Forwards message to right neighbor.
     *  SEARCH_FOR_ROUTE
     *      Adds itself to the sender list.
     *      Checks who the message is from and forwards the message to its other neighbor.
     *      If it came from the left and the message is going to the right but right is null, for example, the message
     *          just doesn't get sent anywhere.
     *  RESERVE_ROUTE
     *      Reserves itself and its light, if applicable.
     *      Then pops the next member off the sender list in Message m and forwards the message to that Rail component
     *      IF it is a neighbor of this track. If it is not, an error message is printed and the message is dropped.
     *      Adds itself to the sender list, then sends it off.
     *  REQUEST_NEXT_TRACK
     *      Pulls train from the sender list
     *      Pops the next sender, which is the track the train was previously on
     *      Pushes itself and then the next track the train should be going to to the sender list
     *      Sends the message back to the train.
     */
    private void readMessage(Message m)
    { //todo: switchcase?
        if(m.type == MessageType.HELLOTEST)
        {
            m.pushSenderList(this);
            if(rightNeighbor!=null) sendMessage(m, rightNeighbor);
            else if(DEBUG) System.out.println("End of the line reached at "+this.toString());
        }
        
        //SEARCH_FOR_ROUTE
        else if(m.type == MessageType.SEARCH_FOR_ROUTE)
        {
            IMessagable mostRecentSender = m.peekSenderList();
            IMessagable neighborToSendTo=null;
            m.pushSenderList(this); //sign before you pass it on.
            
            //look for which neighbor sent this message. Send this message to your other neighbors.
            
            //If the message came from your right, send it to your left, and vis versa.
            if(mostRecentSender==leftNeighbor || mostRecentSender==rightNeighbor)
            {
                if(mostRecentSender==this.leftNeighbor) neighborToSendTo = rightNeighbor;
                if(mostRecentSender==this.rightNeighbor) neighborToSendTo = leftNeighbor;
                if(neighborToSendTo!=null)
                {
                    sendMessage(m,neighborToSendTo);
                    //Only one instance of this message needed because only one instance is being sent out.
                }
                //else... //todo: If we need to send a negative 'no route found' message back, we can do that here.
                //maybe the train only acts if it finds a route. Otherwise... It just sits? Maybe after a while it gets
                // a new destination. If we send emssages back we'd have to wait a set amount of time for all the answers
                //to 'come in' as well.
            }
            else
            {
                if(DEBUG) printNeighborDebug(mostRecentSender, m.type.toString());
                printNeighborError(m.type.toString());
            }
        }
        
        //RESERVE_ROUTE
        //The first member is the one the message came from. The next 'sender' is the one the message needs to go to.
        else if(m.type == MessageType.RESERVE_ROUTE)
        {
            //Tracks don't need to check if they CAN protect. They don't have anything to do.
            //todo: Check if already reserved? Second train
            
            //Actually pop the sender this time. It will be either the right or left neighbor, if this was done correctly.
            m.popSenderList(); //RailTrack doesn't care who it came from, just where it's going.
            IMessagable nextSenderInList = m.popSenderList();
            m.pushSenderList(this);
            if(nextSenderInList == leftNeighbor)
            {
                //the train will be coming from the left to the right; The light should be green facing the left.
                reserve(Direction.LEFT);
                sendMessage(m, leftNeighbor);
            }
            else if(nextSenderInList == rightNeighbor)
            {
                reserve(Direction.RIGHT);
                sendMessage(m,rightNeighbor);
            }
            else
            {
                if(DEBUG) printNeighborDebug(nextSenderInList, m.type.toString());
                printNeighborError(m.type.toString());
            }
        }
        
        //REQUEST_NEXT_TRACK
        else if(m.type == MessageType.REQUEST_NEXT_TRACK)
        {
            if(m.peekSenderList() instanceof Train)
            {
                Train train = (Train)m.popSenderList();
                IMessagable trainPrevTrack = m.popSenderList();
                IMessagable nextForTrain = null;
                Direction trainComingFrom;
                if(trainPrevTrack == leftNeighbor)
                {
                    trainComingFrom = Direction.LEFT;
                    nextForTrain = rightNeighbor;
                }
                else if(trainPrevTrack == rightNeighbor)
                {
                    trainComingFrom = Direction.RIGHT;
                    nextForTrain = leftNeighbor;
                }
                else
                {
                    System.err.println(toString()+"got a request from a train that didn't just come from its neighbor.");
                }
                m.pushSenderList(this);
                m.pushSenderList(nextForTrain);
                unreserve();
                sendMessage(m, train);
            }
            else
            {
                System.err.println(toString()+" got a message of type REQUEST_NEXT_TRACK from "+m.peekSenderList().toString()
                    +" is not a train.");
            }
        }
    }
    
    /**
     * @param mostRecentSender Neighbor who just sent the message
     * @param messageType Type of message that went wrong
     *
     * This method prints a debug statement "this.toString just got a message type messageType from mostRecentSender,
     *                    which is not a neighbor. No message sent."
     */
    private void printNeighborDebug(IMessagable mostRecentSender, String messageType)
    {
        System.out.println(this.toString()+" just got a message (type "+messageType+") from "+mostRecentSender+", which is"
            +" not a neighbor. No message sent.");
    }
    
    /**
     * @param type Type of message that went wrong
     *
     * This method prints a System err statement "Message passed from Rail peice to another that was not a neighbor.
     *             Message type: type."
     */
    private void printNeighborError(String type)
    {
        System.err.println("Message passed from Rail piece to another that was not a neighbor. Message type: "+type);
    }
    
    /**
     *
     * @param x x location to begin drawing on the canvas
     * @param y y location to begin drawing on the canvas
     * Draws the object on a canvas at location x,y according to its currrent state.
     */
    public void draw()
    {
        //TODO: Change color when the track is reserved
        if(reserved) { gcDraw.setFill(Color.BLUE); }
        else { gcDraw.setFill(Color.BLACK); }
        gcDraw.fillText(this.toString(), canvasX, canvasY);
        gcDraw.drawImage(trackImg, canvasX, canvasY);
        gcDraw.setFill(Color.BLACK);
    }
    
    /**
     * This method sends a MessageType.HELLOTEST down the line.
     */
    public void sendTestMessage()
    {
        sendMessage(new Message("TestTrain", this, MessageType.HELLOTEST, null), rightNeighbor);
    }
    
    private synchronized void sendMessage(Message message, IMessagable neighbor)
    {
        if(DEBUG) System.out.println(this.toString()+" sending message to "+neighbor.toString()+". Message is: "+message.toString());
        neighbor.recvMessage(message);
    }
    
    public synchronized void recvMessage(Message message)
    {
        if(DEBUG) System.out.println(this.toString()+" received a message. Message is: "+message.toString());
        pendingMessages.add(message);
        this.notify();
    }

    @Override
    public String toString()
    {
        return NAME;
    }

}
