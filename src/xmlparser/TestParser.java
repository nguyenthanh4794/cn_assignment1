package xmlparser;
import java.io.File;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
 
public class TestParser {

private static String[] arrString = {"","",""};
private static int i = 0;
public static void main(String[] args) throws Exception {
 
    try {
 
    String xmlRecords = "<SESSION_KEEP_ALIVE><PEER_NAME>Username</PEER_NAME><STATUS>ALIVE</STATUS></SESSION_KEEP_ALIVE>";
    InputSource is = new InputSource();
    is.setCharacterStream(new StringReader(xmlRecords));

 
	DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance()
                             .newDocumentBuilder();
 
	Document doc = dBuilder.parse(is);
 
	System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
 
	if (doc.hasChildNodes()) {
		//doc = (Document) doc.getChildNodes();
		printNote(doc.getChildNodes(), doc.getDocumentElement().getNodeName()); 
	}
	
	for(int j=0; j<arrString.length;j++)
	{
		if(arrString[j] != "") System.out.println(arrString[j]); else System.out.println("null");
	}
    } catch (Exception e) {
	System.out.println(e.getMessage());
    }
 
  }
 
  private static void printNote(NodeList nodeList, String Root) {
 
    for (int count = 0; count < nodeList.getLength(); count++) {
 
		Node tempNode = nodeList.item(count);
		// make sure it's element node.
		if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
			//System.out.println("\nNode Name =" + tempNode.getNodeName() + " [OPEN]");
			// get node name and value 
			if (tempNode.hasChildNodes()) {
	 
				// loop again if has child nodes
				printNote(tempNode.getChildNodes(),Root);
				if(tempNode.getNodeName() != Root){
					arrString[i++] = tempNode.getTextContent();
					//System.out.println("Node Value =" + tempNode.getTextContent());
				}				 
			}
			//System.out.println("Node Name =" + tempNode.getNodeName() + " [CLOSE]");
		}
    }
 
  }
 
}