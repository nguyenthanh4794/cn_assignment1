package xmlparser;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Parser {

	private  int i = 0;
	private EventListener mListener;
	public void ReadXML(String XML) throws IOException
	{
        String XML2=XML.replace("<[^>]+>","&lt;");
		String[] arrString = new String[100];
        for (int i=0;i<100;i++) arrString[i]="";
		try {
			InputSource is = new InputSource();
		    is.setCharacterStream(new StringReader(XML2));

			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = dBuilder.parse(is);
			
			String sessionType = doc.getDocumentElement().getNodeName();
			// Read Node
			if (doc.hasChildNodes()) {
                try {
                    i=0;
                    readNote(doc.getChildNodes(),sessionType,arrString);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
			//Call listener
			if (sessionType.equals("SESSION")) 			mListener.OnRegisteredPort(arrString[0], arrString[1]);
			else if (sessionType.equals("SESSION_KEEP_ALIVE")){
				if(arrString[1].equals("ALIVE"))		mListener.OnStillAliveNoti(arrString[0]);
				else									mListener.OnDisconnectNoti(arrString[0]);
			}
			else if(sessionType.equals("SESSION_DENY")) mListener.OnRegisterDenied();
			else if(sessionType.equals("SESSION_ACCEPT")){
                Triple<String,String,String> item;
                List<Triple<String,String,String>> userList=new ArrayList<Triple<String,String,String>>();
                int n=doc.getFirstChild().getChildNodes().getLength();
                for(int j=0;j<n;j++){
                    int k=4*j;
                    String username=arrString[k];
                    String ip=arrString[k+1];
                    String host=arrString[k+2];
                    item = new Triple<String,String,String>(username,ip,host);
                    userList.add(item);
                }
                mListener.OnRegisterAccepted(userList);
            }
			else if(sessionType.equals("CHAT_REQ")) 	mListener.OnChatRequest(arrString[0]);
			else if(sessionType.equals("CHAT_DENY")) 	mListener.OnChatRequestDenied();
			else if(sessionType.equals("CHAT_ACCEPT")) 	mListener.OnChatRequestAccepted();
			else if(sessionType.equals("CHAT_MSG")) 	mListener.OnChatMessage(arrString[0]);
		
		} catch (Exception e) {
			System.out.println(e.getMessage());
	    }			
	}
	private  void readNote(NodeList nodeList, String Root, String[] arrString) {
		for (int count = 0; count < nodeList.getLength(); count++) {
			 
			Node tempNode = nodeList.item(count);
			// make sure it's element node.
			if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
				// get node name and value 
				if (tempNode.hasChildNodes()) {
		 
					// loop again if has child nodes
					readNote(tempNode.getChildNodes(),Root, arrString);
					if(tempNode.getNodeName() != Root){
						arrString[i++] = tempNode.getTextContent();
					}				 
				}
			}
		}
	}
	public void setListener(EventListener mListener) {
		this.mListener = mListener;
	}
	
}
