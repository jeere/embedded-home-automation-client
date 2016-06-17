package fi.oulu.tol.esde_2016_014.ohap;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.opimobi.ohap.CentralUnit;
import com.opimobi.ohap.Container;
import com.opimobi.ohap.Device;
import com.opimobi.ohap.Item;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;

import fi.oulu.tol.esde_2016_014.ping.ohap.IncomingMessage;
import fi.oulu.tol.esde_2016_014.ping.ohap.OutgoingMessage;

public class CentralUnitConnection extends CentralUnit{

    private static CentralUnitConnection INSTANCE = null;

    private static final String TAG = "CentralUnitConnection";
    private int nListeners = 0;
    private boolean running = false;
    private HandlerThread handlerThread = null;
    private IncomingThread incomingThread = null;
    private Socket socket = null;
    private Handler outgoingMessageHandler = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private URL url = null;

    public CentralUnitConnection() {
        super();
        Log.d(TAG, "Initializing central unit connection..");
        this.setName("OHAP Server");
    }

    public static CentralUnitConnection getInstance() {
        Log.w(TAG, "Instance asked.");
        if (null == INSTANCE) {
            Log.w(TAG, "There was no instance yet, creating..");
            INSTANCE = new CentralUnitConnection();
            Log.w(TAG, "Instance creation done.");
            return INSTANCE;
        }else{
            Log.w(TAG, "There was instance already, returning that..");
            return INSTANCE;
        }
    }

    public void initialize(URL url){
        this.url = url;
    }

    private void startNetworking(){
        running = true;

        handlerThread = new HandlerThread("The Handler Thread");
        handlerThread.start();
        Log.d(TAG, "handler is now running");

        incomingThread = new IncomingThread();
        incomingThread.start();
        Log.d(TAG, "Incoming thread is now running");
    }

    private void stopNetworking(){

        if(null != socket){
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x01).text("Good bye!");
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
        }

        running = false;
        Log.d(TAG, "handler thread created");
        handlerThread.quit();
        handlerThread = null;
        Log.d(TAG, "handlerThread is now stopped");

        if(incomingThread != null) {
            incomingThread.interrupt();
            Log.d(TAG, "Incoming thread is now interrupted");
            try {
                incomingThread.join();
                Log.d(TAG, "Incoming thread joined");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            incomingThread = null;
        }
        outgoingMessageHandler = null;
    }

    public void sendListeningStart(long containerIdentifier){
        if(null != socket){
            Log.w("sendListeningStart", "Sending Listening starts...");
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x0c).integer32(containerIdentifier);
            try {
                Log.w("sendListeningStart", "Trying to post message with containeridentifier: "+containerIdentifier);
                if(null != outgoingMessageHandler){
                    Log.w("sendListeningStart", "OutgoingMessage action exists, continue.");
                    outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
                }
            }catch (Exception e){
                Log.d("sendListeningStart", "Catched error");
            }
        }
    }

    private void sendListeningStop(long containerIdentifier){
        if(null != socket){
            Log.w("sendListeningStop", "Sending stop message for container: "+containerIdentifier);
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x0d).integer32(containerIdentifier);
            try {
                Log.w("sendListeningStop", "Trying to post message with containeridentifier: "+containerIdentifier);
                if(null != outgoingMessageHandler){
                    Log.w("sendListeningStop", "OutgoingMessage action exists, continue.");
                    outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
                }
            }catch (Exception e){
                Log.d("sendListeningStop", "Error while trying to send listening stop message for: "+containerIdentifier);
            }
        }
    }

    @Override
    public void changeBinaryValue(Device device, boolean value) {

        Log.d("changeBinaryValue", "Send binary device change message");
        Log.d("changeBinaryValue", "Device that has changed: "+device.getName()+" and value: "+value);

        if(null != socket){
            Log.w("changeBinaryValue", "Sending retrieve message for root container.");
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x0a)
                    .integer32(device.getId())
                    .binary8(value);
            try {
                if(null != outgoingMessageHandler){
                    Log.w("changeBinaryValue", "Sending the new device message for changing binary value.");
                    outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
                }
            }catch (Exception e){
                Log.d(TAG, "Catched error");
            }
        }

    }

    @Override
    public void changeDecimalValue(Device device, double value) {

        Log.d("changeDecimalValue", "Send decimal device change message");
        Log.d("changeDecimalValue", "Device that has changed: "+device.getName()+" and value: "+value);

        if(null != socket){
            Log.w("changeDecimalValue", "Sending retrieve message for root container.");
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x09)
                    .integer32(device.getId())
                    .decimal64(value);
            try {
                if(null != outgoingMessageHandler){
                    Log.w("changeBinaryValue", "Sending the new device message for changing decimal value.");
                    outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
                }
            }catch (Exception e){
                Log.d(TAG, "Catched error");
            }
        }

    }

    @Override
    public Item getItemById(long id) {
        return super.getItemById(id);
    }

    @Override
    public URL getURL() {
        return super.getURL();
    }

    @Override
    public void listeningStateChanged(Container container, boolean listening) {

        if(listening){
            nListeners++;
            Log.d(TAG, "New listener attached");
            if(nListeners == 1){
                Log.d(TAG, "Networking started");
                startNetworking();
            }
            Log.d(TAG, "Sending ListeningStart for container: " + container.getId());
            sendListeningStart(container.getId());
        }else{
            nListeners--;
            Log.d(TAG, "Listener removed");
            if(nListeners == 0){
                Log.d("CUC", "Stopping networking");
                stopNetworking();
            }
            Log.d(TAG, "Sending ListeningStop for container: " + container.getId());
            sendListeningStop(container.getId());
        }

    }

    /* Message handling */

    private class IncomingMessageAction implements Runnable {

        private IncomingMessage incomingMessage;
        public IncomingMessageAction(IncomingMessage incomingMessage) {
            this.incomingMessage = incomingMessage;
        }

        @Override
        public void run() {
            // Handle ping reply message.
            Log.d(TAG, "Incoming msg: " + incomingMessage);
            //handlePingResponse(incomingMessage);
            //Call how to handle incoming msg..?
            Log.w("handlePingResponse", "---- HANDLING INCOMING MESSAGE: " + incomingMessage);
            if (null != incomingMessage) {
                int messageType = incomingMessage.integer8();
                Log.w("handlePingResponse", "MessageType arrived: " + messageType);

                switch (messageType) {

                    case 0x0a:
                        long itemtype10Identifier = incomingMessage.integer32();
                        boolean itemtype10Value = incomingMessage.binary8();
                        Log.w("handlePingResponse", "Identifier arrived: " + itemtype10Identifier);
                        Log.w("handlePingResponse", "Identifier arrived: " + itemtype10Value);
                        break;

                    case 0x08:

                        Log.w("handlePingResponse", "----- message-type-container ------");
                        long itemIdentifier = incomingMessage.integer32();
                        long itemDataParentIdentifier = incomingMessage.integer32();
                        String itemDataName = incomingMessage.text();
                        String itemDataDescription = incomingMessage.text();
                        boolean itemDataInternal = incomingMessage.binary8();
                        Log.w("handlePingResponse", "item-identifier: " + itemIdentifier + "\n");
                        Log.w("handlePingResponse", "item-data-parent-identifier: " + itemDataParentIdentifier + "\n");
                        Log.w("handlePingResponse", "item-data-name: " + itemDataName + "\n");
                        Log.w("handlePingResponse", "item-data-description: " + itemDataDescription + "\n");
                        Log.w("handlePingResponse", "item-data-internal: " + itemDataInternal + "\n");
                        if(itemIdentifier == 0) {
                            Log.w("handlePingResponse", "Setting root container data");
                            CentralUnitConnection.getInstance().setName(itemDataName);
                            CentralUnitConnection.getInstance().setDescription(itemDataDescription);
                            Log.w("handlePingResponse", "Setting done.");
                            sendListeningStart(CentralUnitConnection.getInstance().getId());
                        }else{
                            Log.w("handlePingResponse", "Starting to add child container.");
                            if(CentralUnitConnection.getInstance().getItemById(itemIdentifier)==null) {

                                /*Add container to actual parent*/
                                Container newChildContainer = new Container( ((Container) CentralUnitConnection.getInstance().getItemById(itemDataParentIdentifier)), itemIdentifier);
                                newChildContainer.setName(itemDataName);
                                newChildContainer.setDescription(itemDataDescription);
                                Log.w("handlePingResponse", "Done adding child container...");
                            }else{
                                Log.w("handlePingResponse", "Container already existed");
                            }
                        }
                        break;

                    case 0x07:
                        Log.d(TAG, "DEVICE fetch arrived");

                        long id = incomingMessage.integer32();
                        boolean state = incomingMessage.binary8();
                        long parent = incomingMessage.integer32();
                        String name = incomingMessage.text();
                        String desc = incomingMessage.text();
                        boolean internal = incomingMessage.binary8();

                        Log.d(TAG, "id: " + id);
                        Log.d(TAG, "state: " + state);
                        Log.d(TAG, "parent: " + parent);
                        Log.d(TAG, "name: " + name);
                        Log.d(TAG, "desc: " + desc);
                        Log.d(TAG, "internal: " + internal);

                        if(CentralUnitConnection.getInstance().getItemById(id)==null){
                            Log.d(TAG, "Creating new device");
                            Device newDevice = new Device(((Container) CentralUnitConnection.getInstance().getItemById(parent)), id, Device.Type.ACTUATOR, Device.ValueType.BINARY);
                            newDevice.setName(name);
                            newDevice.setDescription(desc);
                            newDevice.setBinaryValue(state);
                            Log.d(TAG, "New device created");
                        }else{
                            Log.d(TAG, "Device already existed");
                        }
                        break;

                    case 0x04:
                        Log.d(TAG, "DEVICE fetch arrived");

                        long id4 = incomingMessage.integer32();
                        double value4 = incomingMessage.decimal64();
                        long parent4 = incomingMessage.integer32();
                        String name4 = incomingMessage.text();
                        String desc4 = incomingMessage.text();
                        boolean internal4 = incomingMessage.binary8();
                        double min = incomingMessage.decimal64();
                        double max = incomingMessage.decimal64();
                        double coord1 = incomingMessage.decimal64();
                        double coord2 = incomingMessage.decimal64();
                        String text;
                        try {
                             text = incomingMessage.text();
                        }catch (ArrayIndexOutOfBoundsException e){
                             text = "";
                        }

                        Log.d(TAG, "id: " + id4);
                        Log.d(TAG, "value: " + value4);
                        Log.d(TAG, "parent4: " + parent4);
                        Log.d(TAG, "name4: " + name4);
                        Log.d(TAG, "desc4: " + desc4);
                        Log.d(TAG, "internal4: " + internal4);
                        Log.d(TAG, "min: " + min);
                        Log.d(TAG, "max: " + max);
                        Log.d(TAG, "coord1: " + coord1);
                        Log.d(TAG, "coord2: " + coord2);
                        Log.d(TAG, "text: " + text);

                        if(CentralUnitConnection.getInstance().getItemById(id4)==null) {
                            Device newDeviceType4 = new Device(((Container) CentralUnitConnection.getInstance().getItemById(parent4)), id4, Device.Type.SENSOR, Device.ValueType.DECIMAL);
                            newDeviceType4.setName(name4);
                            newDeviceType4.setDescription(desc4);
                            newDeviceType4.setDecimalValue(value4);
                        }else{
                            Log.d(TAG, "Device already existed");
                        }
                        break;

                    case 0x03:
                        long pong_id = incomingMessage.integer32();
                        Log.w("handlePingResponse", "\npong-message-arrived: " + pong_id);
                        break;

                    default:
                        Log.w("handlePingResponse", "Unrecognised message type");
                        break;
                }
            } else {
                Log.w("handlePingResponse", "No incoming msgs yet");
            }
        }
    }

    private class OutgoingMessageAction implements Runnable {

        private OutgoingMessage outgoingMessage;
        public OutgoingMessageAction(OutgoingMessage outgoingMessage) {
            this.outgoingMessage = outgoingMessage;
        }

        @Override
        public void run() {
            if(outputStream != null){
                Log.d(TAG, "Starting to write message to server");
                try {
                    outgoingMessage.writeTo(outputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class IncomingThread extends Thread {

        @Override
        public void run() {
            super.run();

            Log.d(TAG, "Socket created");
            socket = new Socket();

            try {
                socket.setSoTimeout(5000);

                Log.d(TAG, "Socket address, url: " + url.getHost() + " and port: " + url.getPort());

                try {

                    socket.connect(new InetSocketAddress(url.getHost(), url.getPort()), 5000);
                    outgoingMessageHandler = new Handler(handlerThread.getLooper());
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();

                    Log.d(TAG, "Send login message");
                    OutgoingMessage outgoingMessage = new OutgoingMessage();
                    outgoingMessage.integer8(0x00)
                            .integer8(0x01)
                            .text("antti")
                            .text("password");
                    outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));

                    while (running) {
                        if (null != socket) {
                            IncomingMessage msg = new IncomingMessage();

                            try {
                                Log.d(TAG, "Starting new data fetch from server..");

                                boolean dataCame = false;
                                msg.readFrom(inputStream);

                                if (null != msg) {
                                    dataCame = true;
                                }

                                if (dataCame) {
                                    Log.d(TAG, "New Data received - " + msg);
                                    IncomingMessageAction newMessage = new IncomingMessageAction(msg);
                                    Log.d(TAG, "New message: " + newMessage);
                                    Handler incomingHandler = new Handler(Looper.getMainLooper());
                                    incomingHandler.post(newMessage);
                                    Log.d(TAG, "New message posting done.. ");
                                }

                            } catch (SocketTimeoutException e) {
                                Log.d(TAG, "Socket timeout");
                                Log.d(TAG, "Done, no new updates");
                            }
                        }
                    }

                } catch (IOException e) {
                    Log.d(TAG, "Socket connection error.");
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    socket = null;
                    running = false;
                    e.printStackTrace();
                    try {
                        Thread.sleep(2000);
                    } catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    startNetworking();
                }

            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }
}
