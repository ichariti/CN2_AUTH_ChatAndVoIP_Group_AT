package com.cn2.communication;

import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.security.Key; //interface for handling keys
import javax.crypto.Cipher; //functionality for encryption and decryption
import javax.crypto.spec.SecretKeySpec; //construct key from a byte array
import java.util.Base64; //for encoding and decoding data in form Base64(binary representation)
import java.lang.Thread;

public class App2 extends Frame implements WindowListener, ActionListener {
	
	/*
	 * Definition of the app's fields
	 */
	static TextField inputTextField; //input text field
	static JTextArea textArea; //show text area
	static JFrame frame;					
	static JButton sendButton, callButton, stopCallButton;				
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	

	// TODO: Please define and initialize your variables here...
    // Ports and UDP Sockets
    private static final int SEND_PORT_CHAT = 50000, RECEIVE_PORT_CHAT = 50001;
    private static final int SEND_PORT_VOICE = 50002, RECEIVE_PORT_VOICE = 50003;
    private static DatagramSocket sendSocketChat, receiveSocketChat;
    private static DatagramSocket sendSocketVoice, receiveSocketVoice;

    // IP Address
    private static InetAddress remoteAddress;

    // Voice Communication
    static volatile boolean isCalling = false;

    // Encryption Key
    private static final String SECRET_KEY = "2444666668888888"; // 16-byte key

    /**
	 * Construct the app's frame and initialize important parameters
	 */
	public App2(String title) {
		
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
		 * 3. Linking the buttons to the ActionListener in order to handle user's activity
		 */
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	
		stopCallButton.addActionListener(this);
		
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.
	 */
	public static void main(String[] args){
		
		/*
		 * 1. Create the app's window
		 */
        App2 app = new App2("CLIENT George");// TODO: You can add the title that will displayed on the Window of the App here
        app.setSize(500, 250);
        app.setVisible(true);

        try {
        	//create and/or initialize sockets. Specify which IP address you wish to send messages to.
            remoteAddress = InetAddress.getByName("192.168.1.3"); // Replace with the actual remote IP
            sendSocketChat = new DatagramSocket();
            receiveSocketChat = new DatagramSocket(RECEIVE_PORT_CHAT);
            sendSocketVoice = new DatagramSocket();
            receiveSocketVoice = new DatagramSocket(RECEIVE_PORT_VOICE);

            // Thread for receiving text
            Thread chatReceiverThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                try {
                    while (true) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        receiveSocketChat.receive(packet);
                        String encryptedMessage = new String(packet.getData(), 0, packet.getLength());
                        String decryptedMessage = decrypt(encryptedMessage);
                        textArea.append("remote: " + decryptedMessage + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            });
            
            //start the thread
            chatReceiverThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
    @Override
    public void actionPerformed(ActionEvent e) {
    	
    	/*
		 * Check which button was clicked.
		 */
        if (e.getSource() == sendButton) {
            try {
                String message = inputTextField.getText(); //read the message
                String encryptedMessage = encrypt(message); //encrypt the message for safety
                byte[] buffer = encryptedMessage.getBytes(); //convert to bytes
                /*Object that contains the data, their length, receiver's address, and sending port*/
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, remoteAddress, SEND_PORT_CHAT);
                sendSocketChat.send(packet); //socket for sending the packet
                textArea.append("local: " + message + "\n");
                inputTextField.setText(""); //empty the text field
            } catch (Exception ex) {
                ex.printStackTrace(); //write down to console any problem about sockets or encryption
            }
        } else if (e.getSource() == callButton) {
            isCalling = true; 	//this means that the call has started
            callButton.setEnabled(false); 	//disable the callButton
            stopCallButton.setEnabled(true); 	//and enable the stopCallButton
            try { 		//(pulse code modulation, 8000 samples/sec, 8 bits and signed samples, single voice channel
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);//(44100, 16, 1, true, false) for CD quality  
                																//or  (8000, 8, 1, true, true) PCM
                
                TargetDataLine microphone = AudioSystem.getTargetDataLine(format); //record audio using microphone
                microphone.open(format);
                microphone.start();

                SourceDataLine speakers = AudioSystem.getSourceDataLine(format); //play back audio using speakers
                speakers.open(format);
                speakers.start();

                Thread sendVoiceThread = new Thread(() -> { //thread for continuous reading and sending sound 
                    byte[] buffer = new byte[1024];																					//4096 crystal meth
                    try {
                        while (isCalling) { 																						//as far as the call is opened
                            int bytesRead = microphone.read(buffer, 0, buffer.length); //convert sound to packets
                            DatagramPacket packet = new DatagramPacket(buffer, bytesRead, remoteAddress, SEND_PORT_VOICE);
                            sendSocketVoice.send(packet);
                        }
                        microphone.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
                sendVoiceThread.start();

                Thread receiveVoiceThread = new Thread(() -> { //thread for continuous receiving
                    byte[] buffer = new byte[1024]; 																	//4096
                    try {
                        while (isCalling) {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            receiveSocketVoice.receive(packet);
                            speakers.write(packet.getData(), 0, packet.getLength());
                        }
                        speakers.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
                receiveVoiceThread.start();

            } catch (LineUnavailableException ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource() == stopCallButton) {
            isCalling = false;
            callButton.setEnabled(true);
            stopCallButton.setEnabled(false);
            try {
                sendSocketVoice.close();
                receiveSocketVoice.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
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
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the 
	 * window).
	 */
    @Override
    public void windowActivated(WindowEvent e) {}

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e) {
        try {
            if (sendSocketChat != null) sendSocketChat.close();
            if (receiveSocketChat != null) receiveSocketChat.close();
            if (sendSocketVoice != null) sendSocketVoice.close();
            if (receiveSocketVoice != null) receiveSocketVoice.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        dispose();
        System.exit(0);
    }

    @Override
    public void windowDeactivated(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowOpened(WindowEvent e) {}
}