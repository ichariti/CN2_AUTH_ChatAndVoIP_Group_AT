package com.cn2.communication;

import java.net.*;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.lang.Thread;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.sound.sampled.*; //implement voice functionality
public class App extends Frame implements WindowListener, ActionListener {

	/*
	 * Definition of the app's fields
	 */
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;	
	static JButton stopCallButton;
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	static JButton callButton;				
	
	// TODO: Please define and initialize your variables here...
	
		static final int textIn=50000;
		static final int textOut=50001;
		static final int callIn=50002;
		static final int callOut=50003;
		public static final int SAMPLE_RATE = 44100;
	    public static final int BUFFER_SIZE = 1024;
	    static byte[] myIPv4Address=new byte[] {(byte) 192, (byte)168, (byte)1, (byte)3}; //replace with your IP
	    private boolean OnCall=false;
	    
	    
	    // Encryption Key
	    private static final String SECRET_KEY = "2444666668888888"; // 16-byte key
	//
	public class  UDPSendMessage { //This class will send messages over UDP.
	    private DatagramSocket OutputSocket;//we need a socket
	    private InetAddress destinationAddress;//we need somewhere to send our texts to
	    private int destinationPort;

	    public UDPSendMessage(byte[] destinationAddress, int destinationPort) throws Exception {//constructor
	        this.OutputSocket = new DatagramSocket(); 
	        this.destinationAddress = InetAddress.getByAddress(destinationAddress);
	        this.destinationPort = destinationPort;
	    }

	    public void sendMessage(String message) throws Exception {
	    	String encryptedMessage = encrypt(message); //encrypt the message for safety
	    	byte[] buffer = encryptedMessage.getBytes();
	        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, destinationAddress, destinationPort);
	        OutputSocket.send(packet);
	    }
	    public void sendVoicePacket(byte[] voicemessage) throws Exception {
	        DatagramPacket packet = new DatagramPacket(voicemessage, voicemessage.length, destinationAddress, destinationPort);
	        OutputSocket.send(packet);
	    }
	    
	    public void close() {  //this happens automatically because java but whatever
	    	OutputSocket.close();
	    }
	}
	
	public class UDPReceiveMessage {
		private DatagramSocket InputSocket;
		public UDPReceiveMessage(int localPort) throws Exception{
			this.InputSocket=new DatagramSocket(localPort);		//messages will be received at local port
		}
		
		public String receiveMessage() throws Exception{
	    	byte[] buffer = new byte[BUFFER_SIZE];
	    	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	    	InputSocket.receive(packet);
	        return new String(packet.getData(), 0, packet.getLength());
	    }
		public byte[] receiveVoicePacket() throws Exception {
			byte[] buffer = new byte[BUFFER_SIZE];
	    	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	    	InputSocket.receive(packet);
	        return Arrays.copyOfRange(packet.getData(), 0, packet.getLength()); //had to import java.util.Arrays. making sure we only get the valid portion of the data
		}
		public void close() {  //this happens automatically because java but whatever
	    	InputSocket.close();
	    }
		
	}
	
	
	
	
	public class UDPSendAudio{

	    private TargetDataLine microphone;
	    private UDPSendMessage sendAudio;
	    
	    public UDPSendAudio() {//constructor
	    	try {
		    	sendAudio = new UDPSendMessage(myIPv4Address,callOut);
		    }catch(Exception e) {
		    	sendAudio=null; //might need this to be initialized for later (make java happy)
		    	e.printStackTrace();
		    }
	    }
	    
	    public void start() throws Exception {
	        // Set up the microphone (audio capture)
	        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false); //recommendation from project description 8000, 8, 1,true,false
	        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
	        microphone = (TargetDataLine) AudioSystem.getLine(info);
	        microphone.open(format);
	        microphone.start();

	        // Start capturing and sending audio in a separate thread
	        Thread captureThread = new Thread(() -> captureAndSendAudio());
	        captureThread.start();
	    }
	    private void captureAndSendAudio() {
	        byte[] buffer = new byte[BUFFER_SIZE];
	        try {
	            while (true) {
	            	if(OnCall) {	
	            		int bytesRead = microphone.read(buffer, 0, buffer.length);
	            		if (bytesRead > 0) {
	            			sendAudio.sendVoicePacket(buffer);
	            		}
	            	}
	            	else {
	            		microphone.stop();
	         	        microphone.close();
	         	        sendAudio.OutputSocket.close();
	         	        break;
	            	}
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	    
	    
	}
	
	//I have to make another class UDPReceiveAudio. this is new 12-12-24
	public class UDPReceiveAudio{
		
		private SourceDataLine speaker;
	    private UDPReceiveMessage receiveAudio;
	    
	    public UDPReceiveAudio() {//constructor
	    	try {
		    	receiveAudio = new UDPReceiveMessage(callIn); 
		    }catch(Exception e) {
		    	receiveAudio=null; //might need this to be initialized for later (make java happy)
		    	e.printStackTrace();
		    }
	    }
	    public void start() throws Exception {
	    	
	    	AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
	    	// Set up the speaker (audio playback)
	        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
	        speaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
	        speaker.open(format);
	        speaker.start();

	        // Start receiving and playing audio in a separate thread
	        Thread playThread = new Thread(() -> receiveAndPlayAudio());
	        playThread.start();
	    }
	    
	    private void receiveAndPlayAudio() {
	        try {
	            while (true) {
	            	if(OnCall) {	
	            		speaker.write(receiveAudio.receiveVoicePacket(), 0, receiveAudio.receiveVoicePacket().length); // Play the audio
	            	}
	            	else {
	            		speaker.stop();
	        	        speaker.close();
	        	        receiveAudio.InputSocket.close();
	        	        break;
	            	}
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	    
	}
	//end new 12-12-24
	
	/**
	 * Construct the app's frame and initialize important parameters
	 */
	public App(String title) {
		
		/*
		 * 1. Defining the components of the GUI
		 */
		
		// Setting up the characteristics of the frame
		super(title);									
		gray = new Color(254, 254, 254);		
		setBackground(gray);
		setLayout(new FlowLayout());			
		addWindowListener(this);	
		
		// Setting up the TextField and the TextArea
		inputTextField = new TextField();
		inputTextField.setColumns(20);
		
		// Setting up the TextArea.
		textArea = new JTextArea(10,40);			
		textArea.setLineWrap(true);				
		textArea.setEditable(false);			
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		//Setting up the buttons
		sendButton = new JButton("Send");			
		callButton = new JButton("Call");			
		stopCallButton = new JButton("Stop Call");
        stopCallButton.setEnabled(false); //False so as not to be able to respond to user's input				
		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);								
		add(inputTextField);
		add(sendButton);
		add(callButton);
		add(stopCallButton);
		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	
		stopCallButton.addActionListener(this);	

		
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.
	 */
	public static void main(String[] args) throws Exception{
	
		/*
		 * 1. Create the app's window
		 */
		App app = new App("SERVER Ino");  // TODO: You can add the title that will displayed on the Window of the App here																		  
		app.setSize(500,250);				  
		app.setVisible(true);				  

		/*
		 * 2. 
		 */
	
		final UDPReceiveMessage receiveText=app.new UDPReceiveMessage (textIn);
			
            // Start a new thread to listen for incoming texts
            Thread listenerThread = new Thread(() -> {
                try {
                    while (true) {
                    	String encryptedMessage = receiveText.receiveMessage();
                        String decryptedMessage = decrypt(encryptedMessage);
                        SwingUtilities.invokeLater(() -> {				// Update the textArea with the received message in a thread-safe manner
                            textArea.append("Received: " + decryptedMessage + newline);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            listenerThread.start();
            
       final UDPReceiveAudio receiveCall=app.new UDPReceiveAudio();
            Thread listenerAudioThread = new Thread(() -> {
                try {
                    receiveCall.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            listenerAudioThread.start();
            
	}
	
    // Use of Advanced Encryption Standard
    private static String encrypt(String message) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        Key key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private static String decrypt(String encryptedMessage) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        Key key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage);
        return new String(cipher.doFinal(decodedBytes));
    }	
	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e){
		
		UDPSendMessage sendText;
		try {
			sendText = new UDPSendMessage (myIPv4Address,textOut);
		} catch (Exception e1) {
			sendText = null;
			e1.printStackTrace();
		}
		
		UDPSendAudio call;
		try {
			call = new UDPSendAudio ();
		} catch (Exception e2) {
			call = null; //make java happy
			e2.printStackTrace();
		}
		
		
		/*
		 * Check which button was clicked.
		 */
		if (e.getSource() == sendButton){
			
			String message = inputTextField.getText();
            if (!message.isEmpty()) {
                try {
                    // Send the message
                    sendText.sendMessage(message);	

                    // Display the sent message in the textArea
                    textArea.append("Sent: " + message + newline);
                    inputTextField.setText(""); // Clear the input field
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            
        }else if(e.getSource() == callButton){		// The "Call" button was clicked
			OnCall=true; //if you wish to end the call, press the call button again OnCall=!OnCall;
			 callButton.setEnabled(false); 	//disable the callButton
	         stopCallButton.setEnabled(true); 	//and enable the stopCallButton
	         
			//if(OnCall) {
				try {
	                call.start();
	            } catch (Exception ex2) {
	                ex2.printStackTrace();
	            }
				
			//}
			
		}else if(e.getSource() == stopCallButton) {
            System.out.print("stop the call");
			OnCall = false;
            callButton.setEnabled(true);
            stopCallButton.setEnabled(false);
            try {
                call.start();
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }
			

	}

	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the 
	 * window).
	 */
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		dispose();
        	System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
}
