package main;

import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.*;

import xmlparser.EventListener;
import xmlparser.Parser;
import xmlparser.Triple;

public class Client extends JFrame implements EventListener {
    private JTextField userText;
	private JTextArea chatWindow;
    private JPanel contentPanel;
    private JScrollPane tablePane;
    private JTable userListView;
    private JTextField txtMessageBox;
    private JLabel lblCurrentUserName;
    private JLabel lblStatus;
    private JLabel txtStatus;

    private ObjectOutputStream output;
	private ObjectInputStream input;
	private byte[] serverIP;
	private Socket connection;



    private int serverAcceptedStatus;
	
	private Parser parser;
	private List<Triple<String,String,String>> userList;
    private String currentUserName;
    private String currentIP;
    private int currentPort;
	
	// Constructor
	public Client(byte[] host){
		super("Client Interface");
        userList=new ArrayList<Triple<String, String, String>>();
		serverIP = host;
		setContentPane(contentPanel);
		setSize(600, 300);
        setLocationRelativeTo(null);
        setDefaultLookAndFeelDecorated(true);
		setVisible(true);

        createUIComponents();
		
		//init parser and listener
		parser=new Parser();
		parser.setListener(this);
	}

    private void createUIComponents() {
        //init table
        userListView.setModel(new Server.UserListTableModel(userList));
        userListView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2){
                    String peername=userList.get(userListView.getSelectedRow()).getFirst();
                    sendChatReq(peername);
                    System.out.println(peername);
                }
            }
        });
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                //((JFrame)e.getSource()).setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                int request = JOptionPane.showConfirmDialog((JFrame) e.getSource(),
                        "Are you sure to close this window?", "Really Closing?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (request == JOptionPane.YES_OPTION) {
                    sendRequestLogOut();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    closeCrap();
                    //System.exit(0);
                }
            }
        });
    }
	
	// Connect to server
	public void startRunning(){

		try{
			connectToServer();
			setupStreams();
			whileChating();
		}catch(EOFException eofException){
			showMessage("\n Client Terminated connection");
		} catch (Exception e) {
            e.printStackTrace();
        } finally{
			closeCrap();
		}
	}
	
	// connect to server
	private void connectToServer() throws IOException{
		showMessage("connectToServer run\n");
		showMessage("Attepting Connection... \n");
		connection = new Socket(InetAddress.getByAddress(serverIP), 9999);
		showMessage("Connection to: " + connection.getInetAddress().getHostName()+"\n");
	}
	
	// setup stream to send and receive
	private void setupStreams() throws IOException{
		showMessage("setupStreams run");
		output = new ObjectOutputStream(connection.getOutputStream());
		output.flush();
		input = new ObjectInputStream(connection.getInputStream());
		showMessage("\nYour Stream are now good to go\n");
	}
	
	// while chatting
	private void whileChating() throws Exception {
        String dialogText;
        //ableToType(true);
        //Dau tien gui yeu cau dang ki den server
        Random r=new Random();
        do {
            dialogText=(serverAcceptedStatus==0)?
                    "Please input your username":
                    "Sorry, the name you've provide has already existed. Please input another one";
            currentUserName = JOptionPane.showInputDialog(
                    contentPanel,
                    dialogText,
                    "Request Dialog",
                    JOptionPane.PLAIN_MESSAGE
            );
            serverAcceptedStatus=0;
            currentPort=Math.abs(r.nextInt()%9999);
            sendRegister(currentUserName, String.valueOf(currentPort));
            String resp = (String) input.readObject();
            parser.ReadXML(resp);
        }
        while (serverAcceptedStatus==1);
        lblCurrentUserName.setText("["+currentUserName+"]");
        this.setTitle(currentUserName);
        showMessage("you're connected\n");
		String message = "Thks Server!";
		//sendMessage(message);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                sendStatus();
            }
        });
        t.start();
		//sendStatus();

		do{
			try{
				message = (String) input.readObject();
				parser.ReadXML(message);
			}catch(ClassNotFoundException classNotFoundException){
				showMessage("\nwtf user send!");
			}
		}while(!message.equals("SERVER - END"));
        t.interrupt();
	}
	
	// close the tream and socket
	private void closeCrap(){
		showMessage("\n Closing connecting... \n");
		ableToType(false);
		try{
			output.close();
			input.close();
			connection.close();
		}catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	// send Status alive when client is online
	private void sendStatus() {
		while(true){
			try{
				output.writeObject("\n" 
						+ "<SESSION_KEEP_ALIVE>\n"
							+ "<PEER_NAME>"+currentUserName+"</PEER_NAME>\n"
							+ "<STATUS>ALIVE</STATUS>\n"
						+ "</SESSION_KEEP_ALIVE>");
				output.flush();
				Thread.sleep(15000);
			}catch(Exception e){}	
		}	
	}
	//TODO client REQ_CHAT to other <CHAT_REQ><PEER_NAME>user name</PEER_NAME ></CHAT_REQ>
	private void sendChatReq(String peer_name) {
		try {
			output.writeObject("<CHAT_REQ><PEER_NAME>"+peer_name+"</PEER_NAME></CHAT_REQ>");
			output.flush();

            //Mo mot cong serversocket cho tra loi tu peer_name
            new Thread(new ChatThread(peer_name)).start();
		} catch(IOException e) {
			chatWindow.append("\n ERRO: CANNOT CONNECT TO OTHER PEER.");
		}
	}
	//TODO client ACCEPT/DENY chat from other <CHAT_ACCEPT /> <CHAT_DENY />
	private void clientAcceptChat(String userName, boolean b){
		try {
            String data=b?"<CHAT_ACCEPT />":"<CHAT_DENY />";
            String ip="";
            String port="";
            for(Triple<String,String,String> tr:userList)
                if (tr.getFirst().equals(userName)){
                    ip = tr.getSecond();
                    port = tr.getThird();
                }
            Socket conn=new Socket(InetAddress.getByName(ip.substring(ip.indexOf('/')+1,ip.indexOf(':'))),Integer.parseInt(port));
            ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
            out.writeObject(data);
            out.flush();
            out.reset();
            if (!b)
            {
                conn.close();
            }
            else{
                //Tao mot P2PChatThread moi
                new Thread(new ChatThread(userName,conn)).start();
            }
		} catch(IOException e) {
            e.printStackTrace();
			chatWindow.append("\n ERRO: CANNOT ACCEPT TO CHAT WITH OTHER PEER.");
		}
	}
	
	//TODO client send CHAT_MES to other <CHAT_MSG>Chat message</CHAT_MSG>
	private void ChatMessage(String message){
		try {
			output.writeObject("<CHAT_MSG>"+ message+ "</CHAT_MSG>");
			output.flush();
		} catch(IOException e) {
			chatWindow.append("\n ERRO: CANNOT SEND MESSAGE.");
		}
	}
	//TODO client send REQUESTS for send and receive file

	private void sendRequestLogOut() {
			try{
				output.writeObject("\n" 
						+ "<SESSION_KEEP_ALIVE>"
							+ "\n<PEER_NAME>"+currentUserName+"</PEER_NAME>"
							+ "\n<STATUS>OOPS WILL BE KILLED </STATUS>\n"
						+ "</SESSION_KEEP_ALIVE>");
				output.flush();
			}catch(IOException e) {
				chatWindow.append("\n ERRO: CANNOT SEND REQUEST TO LOG OUT.");
			}	
		}
	private void sendRegister(String usrName, String clientPort) {
		try {
			output.writeObject("\n"
					+ "<SESSION>"
					+ "<PEER_NAME>" + usrName + "</PEER_NAME>"
					+ "<PORT>" + clientPort + "</PORT>"
					+ "</SESSION>");
            output.flush();
		}catch(IOException e) {
			chatWindow.append("\n ERRO: CANNOT SEND REGISTER.");
		}	
	}
	private void sendMessage(String message){
		try{
			output.writeObject("CLIENT - " + message);
			output.flush();
			showMessage("\nCLIENT - " + message);
		}catch(IOException ioException){
			chatWindow.append("\n ERROR: MAY TYPE CAI GI VAY");
		}
	}
	
	//update chatWindow
    private void showMessage(final String text){
        SwingUtilities.invokeLater(
            new Runnable(){
                public void run(){
                    lblStatus.setText(text);
                }
            }
        );
    }
    private void showMessage2(final String text){
        SwingUtilities.invokeLater(
                new Runnable(){
                    public void run(){
                        chatWindow.append(text);
                    }
                }
        );
    }

    //let the user type
    private void ableToType(final boolean tof){
        SwingUtilities.invokeLater(
            new Runnable(){
                public void run(){
                    userText.setEditable(tof);
                }
            }
        );
    }

		@Override
        @Deprecated
		public void OnRegisteredPort(String userName, String port) {

		}

		@Override
		public void OnStillAliveNoti(String userName) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void OnDisconnectNoti(String userName) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void OnRegisterDenied() {
			this.serverAcceptedStatus=1;
		}

		@Override
		public void OnRegisterAccepted(
				List<Triple<String, String, String>> userList) {
			this.serverAcceptedStatus=2;
			this.userList.clear();
            for(Triple<String, String, String> tr:userList)if (!tr.getFirst().equals(currentUserName)){
                this.userList.add(tr);
            }
            this.userListView.updateUI();
            showMessage("new client was online: " + userList.get(0).getFirst() + "\n");
			//Cho nay co the bo sung them tinh nang hien danh sach nguoi dang online tren JFrame
		}

		@Override
		public void OnChatRequest(String userName) {
			// TODO Auto-generated method stub
			//showMessage2(userName + " invite you to message hihi!\n");
            int answer=JOptionPane.showConfirmDialog(contentPanel,
                    userName+" has invited you to chat with him/her. OK to accept, Cancel to ignore",
                    "Invitation",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (answer==JOptionPane.OK_OPTION)
            {
                clientAcceptChat(userName,true);
            } else {
                clientAcceptChat(userName, false);
            }
		}

		@Override
		public void OnChatRequestDenied() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void OnChatRequestAccepted() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void OnChatMessage(String message) {
			// TODO Auto-generated method stub
			
		}

    private class ChatThread extends P2PChatListener implements Runnable {
        private ServerSocket listener;
        private Socket p2pconnection;
        private ObjectInputStream mInput;
        private ObjectOutputStream mOutput;
        private Parser mParser;
        private boolean receiver;
        private String peername;

        public ChatThread(String userName,Socket existingSocket){
            p2pconnection=existingSocket;
            setAccept(true);
            peername=userName;
        }

        public ChatThread(String peer_name) {
            setAccept(false);
            peername=peer_name;
        }

        @Override
        public void run() {
            mParser=new Parser();
            mParser.setListener(this);
            try {
                createListener();
                if (!isAccept()) listenToResponse();
                System.out.println(currentUserName+": trang thai hien tai = "+(p2pconnection!=null));
                if(isAccept()){
                    whileP2PChatting();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    //System.out.println(currentUserName+": "+(p2pconnection!=null?"connection is alive":"connection is terminated"));
                    mInput.close();
                    mOutput.close();
                    p2pconnection.close();
                    if (listener!=null) listener.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                showMessage2("Sorry, your invitation has been rejected!");
            }
        }

        private void whileP2PChatting() {
            ActionListener enter = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        mOutput.writeObject("<CHAT_MSG>"+txtMessageBox.getText()+"</CHAT_MSG>");
                        mOutput.flush();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            };
            txtMessageBox.addActionListener(enter);
            Thread recvTh=new Thread(new Runnable() {
                @Override
                public void run() {
                    String message;
                    boolean error=false;
                    do{
                        try {
                            message=(String)mInput.readObject();
                            mParser.ReadXML(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                            error=true;
                        }
                    }
                    while (!error);
                }
            });
            recvTh.start();

            try {
                recvTh.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                txtMessageBox.removeActionListener(enter);
            }
        }

        private void listenToResponse() throws IOException, ClassNotFoundException {
            String message= (String)mInput.readObject();
            System.out.println(message);
            mParser.ReadXML(message);
        }

        private void createListener() throws IOException {
            if (p2pconnection==null) {
                listener = new ServerSocket(currentPort);
                p2pconnection = listener.accept();
            }
            System.out.println(currentUserName+": P2P Connected!");
            mInput = new ObjectInputStream(p2pconnection.getInputStream());
            mOutput = new ObjectOutputStream(p2pconnection.getOutputStream());
        }

        @Override
        public void OnChatMessage(String message) {
            showMessage2(String.format("[%s] %s\n",peername,message));
        }
    }
}
