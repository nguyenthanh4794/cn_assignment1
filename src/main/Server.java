package main;

import xmlparser.EventListener;
import xmlparser.Parser;
import xmlparser.Triple;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server extends JFrame{

    private JTextArea chatWindow;
	private ServerSocket server;

	private List<Triple<String,String,String>> userList;
    private JTable userListView;
    private JPanel contentPanel;
    private JScrollPane tablePane;
    private JTextField txtMessageBox;

    private Object SyncObject;
    private Object Signal;
    private List<ServerThread> clientTh;



    //Constructor
	public Server(){
		super("BK Instant Messenger");
        userList =new ArrayList<Triple<String,String,String>>();
        setContentPane(contentPanel);
        setSize(600, 300);
        setLocationRelativeTo(null);
        setDefaultLookAndFeelDecorated(true);
		setVisible(true);

        createUIComponents();
        SyncObject=new Object();
        Signal=new Object();
        clientTh=new ArrayList<ServerThread>();
	}

    private void createUIComponents() {
        //init table
        userListView.setModel(new UserListTableModel(userList));
        //init message view
        DefaultCaret caret= (DefaultCaret) chatWindow.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }
	
	//set up and run the server
	public void startRunning(){
		try{
			server = new ServerSocket(9999, 100);
			while(true){
				try{
                    showMessage("Waiting for another one to connect...  \n");
                    Socket connection=server.accept();
                    ServerThread th=new ServerThread(connection);
                    clientTh.add(th);
                    new Thread(th).start();

					//waitForConnection();
					//setupStreams();
					//whileChatting();
				}catch(EOFException eofException){
					showMessage("\n Server end conection");
				}finally{
					//closeCrap();
				}
			}	
		}catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	private final void UpdateToAllClient()
    {
        List<ServerThread> noLongerAlive=new ArrayList<ServerThread>();
        for(ServerThread th:clientTh){
            if (!th.alive) noLongerAlive.add(th);
        }
        for(ServerThread th:noLongerAlive)
            clientTh.remove(th);
        for(ServerThread th:clientTh){
            th.accessAccept();
        }
    }
    private final void RedirectChatP2PRequest(String fromUsername,String toUsername){
        for(ServerThread th:clientTh)
            if (th.currentUsername.equals(toUsername)){
                th.SendChatRequest(fromUsername);
            }
    }
	//update chatWindow
	private synchronized void showMessage(final String text){
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
					//userText.setEditable(tof);
				}
			}
		);
	}

    public class ServerThread implements Runnable,EventListener{
        private Socket mSocket;
        private ObjectOutputStream mOutput;
        private ObjectInputStream mInput;
        public Object token;

        private Parser parser;
        private boolean alive;
        private String currentUsername;
        private String currentIP;
        private String currentPort;

        public ServerThread(Socket newSocket) {
            mSocket=newSocket;
            parser=new Parser();
            parser.setListener(this);
            alive=true;
            token=new Object();
            showMessage("Connection set with " + mSocket.getRemoteSocketAddress().toString());
        }

        @Override
        public void run() {
            try {
                this.setupStreams();
                //this.WaitForUpdateAll();
                this.whileChatting();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                this.closeCrap();
            }
        }

        private void setupStreams() throws IOException {
            mOutput = new ObjectOutputStream(mSocket.getOutputStream());
            mOutput.flush();
            mInput = new ObjectInputStream(mSocket.getInputStream());
            showMessage("\nThe Stream is now set up\n");
        }
        private void closeCrap(){
            showMessage("\n Closing connecting... \n");
            ableToType(false);
            try{
                mOutput.close();
                mInput.close();
                mSocket.close();
            }catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
        private void whileChatting() throws IOException {
            String message = "You now are connected!";
            //sendMessage(message);
            //ableToType(true);
            do{
                try{
                    message = (String) mInput.readObject();
                    parser.ReadXML(message);
                    //showMessage("\n" + message);
                }catch(ClassNotFoundException classNotFoundException){
                    showMessage("\nidk wtf user send!");
                }

            }while(alive);
        }
        //region send message to client
        private void accessDeny(){
            try{
                mOutput.writeObject("<SESSION_DENY />");
                mOutput.flush();
            }catch(IOException e) {
                chatWindow.append("\n ERRO: SERVER NOT DENY WHEN CLIENT REGISTER STUPID");
            }
        }
        private void accessAccept() {
            synchronized (userList) {
                showMessage("Update to client \""+currentUsername+"\"\n");
                showMessage("Current number of clients: " + String.valueOf(userList.size()) + "\n");
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<SESSION_ACCEPT>");
                    for (Triple<String, String, String> tr : userList) {
                        sb.append("<PEER>");
                        sb.append("<PEER_NAME>" + tr.getFirst() + "</PEER_NAME>");
                        sb.append("<IP>" + tr.getSecond() + "</IP>");
                        sb.append("<PORT>" + tr.getThird() + "</PORT>");
                        sb.append("</PEER>");
                    }
                    sb.append("</SESSION_ACCEPT>");
                    mOutput.writeObject(sb.toString());
                    mOutput.flush();
                } catch (IOException e) {
                    chatWindow.append("\n ERRO: SERVER NOT DENY WHEN CLIENT REGISTER STUPID");
                }
                showMessage("Update completed!!!\n\n");
            }
        }
        private void SendChatRequest(String fromUsername)
        {
            System.out.println(fromUsername);
            try {
                mOutput.writeObject("<CHAT_REQ><PEER_NAME>"+fromUsername+"</PEER_NAME></CHAT_REQ>");
            } catch (IOException e) {
                chatWindow.append("\n ERROR WHEN REDIRECT CHAT REQUEST FROM "+fromUsername+"\n");
                e.printStackTrace();
            }
        }
        //endregion
        //region Listener implemented
        @Override
        public void OnRegisteredPort(String userName, String port) {
            // TODO Client gửi đăng kí lên Server
            showMessage("Notification received from client "+userName+" "+port+"\n");
            synchronized (SyncObject) {
                boolean conflict = false;
                for (Triple<String, String, String> tr : userList) {
                    if (tr.getFirst().equals(userName))
                        conflict = true;
                }
                if (conflict) accessDeny();
                else {
                    currentUsername=userName;
                    Triple<String, String, String> tr = new Triple<String, String, String>(userName, mSocket.getRemoteSocketAddress().toString(), port);
                    userList.add(tr);
                    userListView.updateUI();
                    //accessAccept();
                    UpdateToAllClient();
                }
            }
        }

        @Override
        public void OnStillAliveNoti(String userName) {
            // TODO Auto-generated method stub
            showMessage(userName+" vẫn còn sống hahaha!\n");
        }

        @Override
        public void OnDisconnectNoti(String userName) {
            // TODO chấm dứt hoạt động của client
            showMessage(userName+" chết con mẹ nó rồi!\n");
            synchronized (SyncObject) {
                Triple<String, String, String> user = null;
                for (Triple<String, String, String> tr : userList) {
                    if (tr.getFirst().equals(userName))
                        user = tr;
                }
                if (user != null) userList.remove(user);
                userListView.updateUI();
                alive=false;
                UpdateToAllClient();
            }
        }

        @Override
        public void OnRegisterDenied() {
            // TODO Auto-generated method stub

        }

        @Override
        public void OnRegisterAccepted(List<Triple<String, String, String>> userList) {
            // TODO Auto-generated method stub

        }

        @Override
        public void OnChatRequest(String userName) {
            // TODO Auto-generated method stub
            RedirectChatP2PRequest(currentUsername,userName);
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
        //endregion
    }
    public static class UserListTableModel extends AbstractTableModel {
        private String[] columns={"Username","IP Address","Port"};
        private List<Triple<String,String,String>> mDataHolder;

        @Override
        public int getRowCount() {
            return mDataHolder.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        public UserListTableModel(List<Triple<String,String,String>> dataHolder) {
            super();
            mDataHolder=dataHolder;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columns[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0,columnIndex).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex){
                case 0:
                    return mDataHolder.get(rowIndex).getFirst();
                case 1:
                    return mDataHolder.get(rowIndex).getSecond();
                case 2:
                    return mDataHolder.get(rowIndex).getThird();
            }
            return null;
        }

        @Override
        @Deprecated
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        }
    }
}
