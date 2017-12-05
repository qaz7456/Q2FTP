package tw.com.q2ftp.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLUtil {
	private static final Logger logger = LogManager.getLogger(XMLUtil.class);

	/*
	 * 提供XML路徑得到Document物件
	 * */
	public static Document getDocumentForFilePath(String path) {
		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder dombuilder = null;
		try {
			dombuilder = domfac.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error(e);
		}
		File file = new File(path);
		Document document = null;
		try {
			document = dombuilder.parse(file);
		} catch (SAXException | IOException e) {
			logger.error(e);
		}
		return document;
	}
	
	/*
	 * 提供XML字串得到Document物件
	 * */
	public static Document getDocumentForXml(String xml) throws Exception {

		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder dombuilder = null;
		try {
			dombuilder = domfac.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error(e);
		}

		InputSource is = null;

		try {
			is = new InputSource(new StringReader(xml));
		} catch (Exception e) {
		}
		Document doc = null;
		try {
			doc = dombuilder.parse(is);
		} catch (SAXException | IOException e) {
			logger.error(e);
		}
		return doc;
	}


	/*
	 * 提供Document物件得到XML字串
	 * */
	public static String docToString(Document doc) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			transformer.transform(new DOMSource(doc), new StreamResult(sw));
			return sw.toString();
		} catch (Exception ex) {
			throw new RuntimeException("Error converting to String", ex);
		}
	}

	/*
	 * 提供XML檔案路徑以及要轉換的物件Class得到轉換後的物件
	 * */
	public static Object getXmlToObj(String path, Class<?> classesToBeBound) {
		File file = null;
		Object object = null;
		try {
			file = new File(path);

			JAXBContext jaxbContext = JAXBContext.newInstance(classesToBeBound);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			object = jaxbUnmarshaller.unmarshal(file);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return object;
	}
}
