package org.utn.frd.lsi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dataloader.DataLoader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.Map;
import java.io.*;
import java.util.*;

import java.security.MessageDigest;



@Path("/files")
public class FileUpload {

	//private static final String SERVER_UPLOAD_LOCATION_FOLDER = "/root/glassfish4/glassfish/domains/domain1/test/";
	private static final String SERVER_UPLOAD_LOCATION_FOLDER = "C:\\ibd\\";

	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
			@FormDataParam("idinvestigacion") String idinvestigacion,
			@FormDataParam("idarchivo") String idarchivo,
			@FormDataParam("hash") String clienthash,
			@FormDataParam("formato") String formato) {

		String FileName=contentDispositionHeader.getFileName();
		String ConfigPath=SERVER_UPLOAD_LOCATION_FOLDER;
		String TempPath=SERVER_UPLOAD_LOCATION_FOLDER;
		String ProcStage=null;
		String Resource=null;
		String serverhash=null;
		
		String res=null;
		String trace=null;
		
		long time_start, time_end_pre, time_end_dl, time_end;
		boolean excelcnv, usetika, dncelda, dntitular;
		
		Date date = new Date();
		DateFormat hourdateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		
		time_start=System.currentTimeMillis();
	    Resource=idinvestigacion + "_" + clienthash;
	    ProcStage="O";		
		
		//Pregunto si existe en la base para la investigacion, si existe envio error.
		if(1==1) {
		  try {
			//Cargo el xml si no existe envio error.
			File fXml = new File(ConfigPath	+ formato + ".xml");

		    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		    Document doc = dBuilder.parse(fXml);

		    doc.getDocumentElement().normalize();

		    NodeList nlGlobal = doc.getElementsByTagName("global");
		    Node nGlobal = nlGlobal.item(0);
		    Element eGlobal = (Element) nGlobal;
		    
		    excelcnv=Boolean.parseBoolean(eGlobal.getElementsByTagName("excelcnv").item(0).getTextContent());
		    usetika=Boolean.parseBoolean(eGlobal.getElementsByTagName("usetika").item(0).getTextContent());
		    dncelda=Boolean.parseBoolean(eGlobal.getElementsByTagName("dncelda").item(0).getTextContent());
		    dntitular=Boolean.parseBoolean(eGlobal.getElementsByTagName("dntitular").item(0).getTextContent());
      
		    //Creo reguistro de archivo
		    idarchivo="1";
		  
		    //Paso a Resource idinvestigacion + "_" + hash y guardo stage

		  
		    //Guardo el archivo como "O" + idinvestigacion + "_" + hash
		    SaveFile(fileInputStream, TempPath + ProcStage + Resource);

		    //Genero y comparo hash
		    serverhash=getSha1(TempPath + ProcStage + Resource);
		    
		    //Si va ecxelcnv lo ejecuto
		    if (excelcnv) {
		      ProcStage=ExcelCnv(TempPath, ProcStage, Resource);
		    }
		    
		    //Si va tika lo ejecuto
		    if (usetika) {
		      ProcStage=TikaProc(TempPath, ProcStage, Resource);
		    }

		    time_end_pre=System.currentTimeMillis();
		    
		    //Ejecuto DataLoader
		    File fileorg = new File(TempPath + ProcStage + Resource);
		    HashMap<String, String> FixValues=new HashMap<String, String>();
		    FixValues.put("ID_Investigacion", idinvestigacion);
		    FixValues.put("ID_Archivo", idarchivo);
		    FixValues.put("Archivo_Origen", contentDispositionHeader.getFileName());
		    FixValues.put("Fecha_Proceso", hourdateFormat.format(date));
		    FixValues.put("Formato", formato);
		    
		    DataLoader dl = new DataLoader(fileorg, fXml, FixValues);
		    dl.LoadData();
		    time_end_dl=System.currentTimeMillis();
		  
		    //Si va DeNormalize lo ejecuto
		    if (dncelda) {
		    	
		    }
		    
		    if (dntitular) {
		    	
		    }
		    
		    time_end = System.currentTimeMillis();
		    
		    //Guardo detalles del proceso en id de archivo
		    res="hash: " + serverhash + " - Read: " + dl.getReadCount() + " - Proc: " + dl.getProcCount() + " - Insert: " + dl.getInsertCount() + " time: " + ( time_end - time_start );
		    //res=System.getProperty("user.dir");
		  } catch(IOException e) {
			  res="error IO " + e.getMessage();
		  } catch(SAXException e) {
			  res="error sax";
	      } catch(TikaException e) {
	    	  res="error tika";
	      } catch (ParserConfigurationException e) {
	    	  res="error parser " + e.getMessage();
	      } catch (Exception e) {
	    	  res="error dl " + trace + " " + e.getMessage();
		  }
		}
		
		return Response.status(200).entity(res).build();

	}

	

    public String getSha1(String resourceLocation)throws Exception
	{
	    MessageDigest md = MessageDigest.getInstance("SHA-1");
	    FileInputStream fis = new FileInputStream(resourceLocation);

	    byte[] dataBytes = new byte[1024];

	    int nread = 0;
	    while ((nread = fis.read(dataBytes)) != -1) {
	      md.update(dataBytes, 0, nread);
	    };
	    byte[] mdbytes = md.digest();

	    //convierte byte a hex
	    StringBuffer sb = new StringBuffer();
	    for (int i = 0; i < mdbytes.length; i++) {
	      sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
	    }

		fis.close();
		
		return sb.toString();
	}
	
	//Convierte excel de BIFF5 a un formato que pueda ser leido por Tika
	private String ExcelCnv(String temppath, String procstage, String resource)
	{
	  List<String> commands = new ArrayList<String>();
	  String s = null;
	  String result = "";
	  String error = "";

	  //commands.add("C:\\Program Files (x86)\\Microsoft Office\\Office12\\excelcnv");
	  commands.add("C:\\Program Files (x86)\\Microsoft Office\\Office15\\excelcnv");
	  commands.add("-oice");
	  commands.add(temppath + "O" + resource);
	  commands.add(temppath + "C" + resource);

	  try {
	    ProcessBuilder pb = new ProcessBuilder(commands);
	    Process process = pb.start();

	    BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

	    while ((s = stdInput.readLine()) != null) {result=result+s+"\n";}
	    while ((s = stdError.readLine()) != null) {error=error+s+"\n";}

	  } catch (IOException e) {
      }
		return "C";
	}
	
	//Extrae texto plano de excel, doc, pdf, etc.
	private String TikaProc(String temppath, String procstage, String resource) throws IOException, SAXException, TikaException
	{
		InputStream input = new FileInputStream(new File(temppath+procstage+resource));
		ContentHandler handler = new BodyContentHandler(-1);
		Metadata metadata = new Metadata();
	    
		AutoDetectParser parser = new AutoDetectParser();
		parser.parse(input, handler, metadata);  
	    
		input.close();

		File file = new File(temppath + "T" + resource);

		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(handler.toString());
		bw.close();	
		return "T";
	}

	
	private void SaveFile(InputStream uploadedInputStream, String resourceLocation) throws IOException
	{
		OutputStream outpuStream = new FileOutputStream(new File(resourceLocation));
		int read = 0;
		byte[] bytes = new byte[1024];

		outpuStream = new FileOutputStream(new File(resourceLocation));
		while ((read = uploadedInputStream.read(bytes)) != -1) {
			outpuStream.write(bytes, 0, read);
		}
		outpuStream.flush();
		outpuStream.close();
	}

}