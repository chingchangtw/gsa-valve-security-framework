 /**
  * Copyright (C) 2008 Sword
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */

package com.google.gsa.valve.sword;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gsa.AuthenticationProcessImpl;
import com.google.gsa.Credential;
import com.google.gsa.Credentials;
import com.google.gsa.WebProcessor;
import com.google.gsa.valve.configuration.ValveConfiguration;
import com.google.gsa.valve.configuration.ValveRepositoryConfiguration;

import javax.security.auth.login.LoginException;

public class DCTMAuthenticationProcessPaul implements AuthenticationProcessImpl{
//	private static final int NB_AUTH_COOKIES = 1;
	private Logger logger = null;
	private WebProcessor  webProcessor = null;
	private Header[] headers = null;
	private HttpMethodBase WebProcResponse = null;
	private ValveConfiguration valveConf = null;    
        
        //CLAZARO: add gsaAuthCookie
        private Cookie gsaAuthCookie;
	
	
	public DCTMAuthenticationProcessPaul() {

		logger = Logger.getLogger(DCTMAuthenticationProcessPaul.class);
		logger.debug("Initializing " + DCTMAuthenticationProcessPaul.class.getName());
		// Set HTTP headers
		headers = new Header[2];
		
		// Set User-Agent
		headers[0] = new Header("User-Agent", "Authentication Web Processor");
		
	}
	
        public void setIsNegotiate (boolean isNegotiate) { 
            //do nothing
        }
    
	public void setWebProcessor(WebProcessor webProcessor) {
		this.webProcessor = webProcessor;
	}
        
        public void setValveConfiguration(ValveConfiguration valveConf) {
            this.valveConf = valveConf;
                         
        }

	//public int authenticate(HttpServletRequest request, HttpServletResponse response,  Cookie gsaAuthCookie, String url, Properties valveConfig, Credentials creds, String id) throws HttpException, IOException {
	public int authenticate(HttpServletRequest request, HttpServletResponse response,  Vector<Cookie> authCookies, String url, Credentials creds, String id) throws HttpException, IOException {
	
		
		logger.info("DCTMAUTHENTICATION : methode authenticate !");
		
                //CLAZARO: set config
                //valveConf = ValveConfiguration.getInstance();
                //CLAZARO: end set config
                
		logger.debug("fetching configuration for " + id);                                
		ValveRepositoryConfiguration repositoryConfig = valveConf.getRepository(id);
		
		
		//repositoryConfig.getParameterValue("DocBase");
		
		
		
		// Initialize status code
		int statusCode 			= HttpServletResponse.SC_UNAUTHORIZED;
		String userID 			= null;
		String password 		= null;
		String docbase			= null;
		String CMSCookName		= null;
		UsernamePasswordCredentials credentials=null;

		String path_to_conf_file=null;
		
		if (repositoryConfig == null) {
			logger.error("repositoryConfig is null");
		} else {
			
			if (repositoryConfig.getParameterValue("webtopAuthenticationConfFilePath") != null) {
				logger.debug("webtopAuthenticationConfFilePath: " + repositoryConfig.getParameterValue("webtopAuthenticationConfFilePath"));
				path_to_conf_file=repositoryConfig.getParameterValue("webtopAuthenticationConfFilePath");
			}
		}
		
		
		
		
		Cookie extAuthCookie = null;
		
		
		//Protection
		Cookie[] cookies = null;

		// Set counter
		int nbCookies = 0;
		
		
		Credential cred = creds.getCredential(id);
		if (cred != null) {
			logger.debug("Credentials [" + id + "] username: " + cred.getUsername());
		}
		
		
//		 Debug
		if (logger.isDebugEnabled()) logger.debug("[HTTPBASICAUTHENTICATIONPROCESS]  Launching the dctm authentication process");

//		Read cookies
		cookies = request.getCookies();
//		 Protection
		
                //CLAZARO
                Cookie userIDCookie = null;
		
		if (cookies != null) {
			if (logger.isDebugEnabled()) logger.debug("[HTTPBASICAUTHENTICATIONPROCESS]  Cookies trouv�s");	
			// Check if the authentication process already happened by looking at the existing cookies
			
			for (int i = 0; i < cookies.length; i++) {
	
				// Check cookie name
                                //CLAZARO: change the following code
				//if ((cookies[i].getName()).equals("gsaSSOCookie")) {
				 if ((cookies[i].getName()).equals(valveConf.getAuthCookieName())) {
					if (logger.isDebugEnabled()) logger.debug("[HTTPBASICAUTHENTICATIONPROCESS]  Cookie gsaSSOCookie trouv�");
					// Increment counter
					nbCookies++;
					
					
				}
				if ((cookies[i].getName()).equals("userId")) {
					
					if (logger.isDebugEnabled()) logger.debug("[HTTPBASICAUTHENTICATIONPROCESS]  Cookie gsaSSOCookie trouv�");
					
                                        //CLAZARO
                                        userIDCookie = cookies[i];
                                        
                                        // Increment counter
					nbCookies++;
					
					
				}
                                
                                if (cookies[i].getName().equals(valveConf.getAuthCookieName())) {
                                        gsaAuthCookie = cookies[i];
                                }
				
			}
			
		}
		
		// Protection
		if (nbCookies >= 2) {
			
			if (logger.isDebugEnabled()) logger.debug("[HTTPBASICAUTHENTICATIONPROCESS]  Authentication on webtop already happened");
			
                        //CLAZARO
                        //add cookies
                        authCookies.add(userIDCookie);
                        		
			// Set status code
			statusCode = HttpServletResponse.SC_OK;

			// Return
			return statusCode;
			
		}
		
		
		
		
		/*
		userID 	= request.getParameter("user_login");
		logger.info("[HTTPBASICAUTHENTICATIONPROCESS] userID vaut "+userID);
		password = request.getParameter("user_password");
		logger.info("[HTTPBASICAUTHENTICATIONPROCESS] user_password vaut "+password);
		docbase = request.getParameter("Login_docbase_0");
		logger.info("[HTTPBASICAUTHENTICATIONPROCESS] docbase vaut "+docbase);
		*/
		
		userID 	= cred.getUsername();
		logger.info("[HTTPBASICAUTHENTICATIONPROCESS] userID vaut "+userID);
		password = cred.getPassword();
		logger.info("[HTTPBASICAUTHENTICATIONPROCESS] user_password vaut "+password);
		
		docbase = "gdoc";
		logger.info("[HTTPBASICAUTHENTICATIONPROCESS] docbase vaut "+docbase);
		
		
		
		CMSCookName = "JSESSIONID";		
		
		if ((userID.equals("null")) || (userID.equals(""))) {
			
			// Debug
			if (logger.isDebugEnabled()) logger.debug("[HTTPBASICAUTHENTICATIONPROCESS]  HTTP 'UserID' parameter required!");
			
			// Return
			statusCode = HttpServletResponse.SC_UNAUTHORIZED;
			
		}
		
		// Protection
		if ((password.equals("null")) || (password.equals(""))) {

			// Debug
			if (logger.isDebugEnabled()) logger.debug("[HTTPBASICAUTHENTICATIONPROCESS]  HTTP 'Password' parameter required!");
			
			// Return
			statusCode = HttpServletResponse.SC_UNAUTHORIZED;
			
		}

//		 Protection
		if ((docbase.equals("null")) || (docbase.equals(""))) {

			// Debug
			if (logger.isDebugEnabled()) logger.debug("[HTTPBASICAUTHENTICATIONPROCESS]  HTTP 'docbase' parameter required!");
			
			// Return
			statusCode = HttpServletResponse.SC_UNAUTHORIZED;
			
		}
		
		
		try{
			credentials= new UsernamePasswordCredentials(userID, password);
			this.webProcessor=new WebProcessor();
			DOMParser parser = new DOMParser();
			logger.info("[HTTPBASICAUTHENTICATIONPROCESS] path_to_conf_file " + path_to_conf_file);
			parser.parse(path_to_conf_file);
			Document document = parser.getDocument();
			NodeList nodes = document.getChildNodes().item(0).getChildNodes(),nodes2;
			Element e = null;
			Vector vector = new Vector();
			String type = null;
			String urltofetch = null;
			boolean cookieSessionfound = false;
			String authCookieDomain = null;
	    	String authCookiePath = null;
	    	String CMSCookValue=null;
	    	String CMSCookDomain=null;
	    	String CMSCookPath=null;
	    	boolean CMSCookSecure=false;
			
			//read the XML file containing the request for authentication 
			logger.info("[HTTPBASICAUTHENTICATIONPROCESS] nb de noeuds vaut "+nodes.getLength());
			for(int i = 1 ; i< nodes.getLength(); i++){
				logger.info("[HTTPBASICAUTHENTICATIONPROCESS] i vaut "+i);
				if(nodes.item(i).getNodeType() == Node.ELEMENT_NODE){
					e = (Element)nodes.item(i);
					if(e.getNodeName().equalsIgnoreCase("request")){
						
						
						logger.info("[HTTPBASICAUTHENTICATIONPROCESS] nom vaut "+e.getNodeName());	
						
		                i++;
		                Hashtable<String,Header> hashtable = new Hashtable<String,Header>(0);
		                Vector<NameValuePair> vector1 = new Vector<NameValuePair>(0);
		                
		                Header header;
		                for(Enumeration enumeration = vector.elements(); enumeration.hasMoreElements(); hashtable.put(header.getName(), header))
		                    header = (Header)enumeration.nextElement();
	
		                
		                nodes2 = e.getChildNodes();
		                Element element1 = null;
		                String attValue = null;
		    			for(int j = 0; j< nodes2.getLength();j++){
		    				if(nodes2.item(j).getNodeType() == Node.ELEMENT_NODE){
								element1 = (Element)nodes2.item(j);
								logger.info("[HTTPBASICAUTHENTICATIONPROCESS] nom de node2 vaut "+nodes2.item(j).getNodeName());	
			                    if(nodes2.item(j).getNodeName().equalsIgnoreCase("type"))
			                        type = nodes2.item(j).getFirstChild().getNodeValue();
			                    
								if(nodes2.item(j).getNodeName().equalsIgnoreCase("URL"))
			                        urltofetch = nodes2.item(j).getFirstChild().getNodeValue();
			                    if(nodes2.item(j).getNodeName().equalsIgnoreCase("header")){
			                        Header header1 = new Header(element1.getAttribute("name"), element1.getAttribute("value"));
			                        hashtable.put(header1.getName(), header1);
			                    }
			                    if(nodes2.item(j).getNodeName().equalsIgnoreCase("parameter")){
			                    	if(element1.getAttribute("name").equals("Login_username_0")){
			                    		attValue = userID;
			                    	}else if(element1.getAttribute("name").equals("Login_password_0")){
			                    		attValue = password;
			                    	}else if(element1.getAttribute("name").equals("Login_docbase_0")){
			                    		attValue = docbase;
			                    	}else{
			                    		attValue = element1.getAttribute("value");
			                    	}
			                        vector1.add(new NameValuePair(element1.getAttribute("name"), attValue));
			                    }
		    				}
		                    
		                }

		    			logger.info("[HTTPBASICAUTHENTICATIONPROCESS] NEW REQUEST");
		                logger.info("[HTTPBASICAUTHENTICATIONPROCESS] Url to fetch" + urltofetch);
		                logger.info("[HTTPBASICAUTHENTICATIONPROCESS] Request type " + type);
		                logger.info("[HTTPBASICAUTHENTICATIONPROCESS] Pparameters " );
		                Enumeration enumeration1 = vector1.elements();
		                NameValuePair anamevaluepair1[] = new NameValuePair[vector1.size()];
		                for(int l = 0; enumeration1.hasMoreElements(); l++){
		                    anamevaluepair1[l] = (NameValuePair)enumeration1.nextElement();
		                    logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] " + anamevaluepair1[l].getName() + " : " + anamevaluepair1[l].getValue());
		                }
	
		                enumeration1 = hashtable.elements();
		                Header aheader1[] = new Header[hashtable.size()];
		                logger.info("[HTTPBASICAUTHENTICATIONPROCESS] headers " );
		                for(int i1 = 0; enumeration1.hasMoreElements(); i1++){
		                    aheader1[i1] = (Header)enumeration1.nextElement();
		                    logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] " +  aheader1[i1].getName() + " : " + aheader1[i1].getValue());
		                }

		                if (WebProcResponse != null) {
		                	logger.debug("release previous connection");						
		                	WebProcResponse.releaseConnection();
		                }
		                WebProcResponse = webProcessor.sendRequest(credentials,type,aheader1,anamevaluepair1,urltofetch);
		                // if this is the first request done, we retrieve the cookie
		                // generated by the source 
		                
		                // Read webProcessor cookies
		                ///
		    	        org.apache.commons.httpclient.Cookie[] responseCookies = webProcessor.getResponseCookies();
		    	        
		                if(!cookieSessionfound){
		                	///responseHeaders = WebProcResponse.getResponseHeaders();
			    			///for (int j = 0; j < responseHeaders.length; j++) {
		                	logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] first");
		                	
		                	for (int j = 0; j < responseCookies.length; j++) {
			    			    ///if (responseHeaders[j].getName().equals("Set-Cookie")){
		                		
		    				       ///
		    				       if ((responseCookies[j].getName()).equals(CMSCookName)){	
		    				    	   ///modif new valve
		    				    	   ///gsaAuthCookie.setValue(responseCookies[j].getValue());
		    				    	   logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] cookie "+CMSCookName);
		    				    	  
		    				    	   ///int authCookieMaxAge = null;
		    				    	   // Cache cookie properties
		    				    	   authCookieDomain = (request.getAttribute("authCookieDomain")).toString();
		    				    	   authCookiePath = (request.getAttribute("authCookiePath")).toString();
		    				    	   
		    				    	   ///
		    				    	   ///valAuthCook = tabinfocook[1];
		    				        
		    				    	   logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] authCookieDomain "+ authCookieDomain);
		    				    	   logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] authCookiePath "+ authCookiePath);
		    				    	   
		    				    	   
		    				    	   CMSCookValue=responseCookies[j].getValue();
		    				    	   CMSCookDomain=responseCookies[j].getDomain();
		    				    	   CMSCookPath=responseCookies[j].getPath();
		    				    	   CMSCookSecure=responseCookies[j].getSecure();
		    				    	   
		    				    	   
		    						   cookieSessionfound = true;
		    				    	  
		    				       }    
			    			    
			    			}
			    			
			    			logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] apr�s first � false");
		                }
		                
		                
		                logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] apr�s fin if first");
		            }
					logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] apr�s fin if request");
				}
				logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] apr�s fin if type node");
			}
			logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] apr�s fin boucle nodes");
			
			if (WebProcResponse.getResponseBodyAsString().indexOf("login.jsp") != -1){
				logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] : on tombe sur login.jsp");
			    statusCode=HttpServletResponse.SC_UNAUTHORIZED;
			    logger.debug("release connection");
			    WebProcResponse.releaseConnection();
			}else{
			    statusCode=HttpServletResponse.SC_OK;
			    
			    ///cr�ation des cookies
			   
		    	   
		    	   extAuthCookie = new Cookie(("gsa_webtop_" + CMSCookName), (CMSCookName + 
						"||" + CMSCookValue + "||" + CMSCookPath + 
						"||" + CMSCookDomain + "||" + CMSCookSecure));
//		        
		    	   logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] new cookie gsa_webtop_" + CMSCookName + 
							"||" + CMSCookValue + "||" + CMSCookPath + 
							"||" + CMSCookDomain + "||" + CMSCookSecure);
		    	  
		    	  
		    	
//			 		Set extra cookie parameters
		    	   extAuthCookie.setDomain(authCookieDomain);
		    	   extAuthCookie.setPath(authCookiePath);
		    	   
		    	   logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] extAuthCookie setDomain "+ gsaAuthCookie.getDomain());
		    	   logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] extAuthCookie setPath "+ gsaAuthCookie.getPath());
					
		    	   // Log info
		    	   if (logger.isDebugEnabled()) logger.debug("[HTTPBASICAUTHENTICATIONPROCESS]  Wrapping HTTP request cookie: " + extAuthCookie.getName() + ":" + extAuthCookie.getValue() 
					+ ":" + extAuthCookie.getPath() + ":" + extAuthCookie.getDomain() + ":" + extAuthCookie.getSecure());
			
		    	   // Add authentication cookie		    	    
                           //CLAZARO: add cookie below
                           //response.addCookie(extAuthCookie);
		    	   logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] apr�s addCookie");
		    	   
///cr�ation du cookie Userid � ce niveau pour utilisation au niveau du process d'autorisation
		    	   Cookie userIdCookie = new Cookie ("userId", userID);
				   logger.info("[HTTPBASICAUTHENTICATIONPROCESS] Cookie userId = " + userIdCookie.getValue());
				   userIdCookie.setPath(authCookiePath);
				  /// userIdCookie.setMaxAge(authCookieMaxAge);
				   userIdCookie.setDomain(authCookieDomain);
				   
				   logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] userIdCookie setDomain "+ userIdCookie.getDomain());
		    	   logger.info("\t[HTTPBASICAUTHENTICATIONPROCESS] userIdCookie setPath "+ userIdCookie.getPath());
					
                                    //CLAZARO: add cookie below
				   //response.addCookie(userIdCookie);
///cr�ation d'un cookie docbase pour utilisation au niveau du process d'autorisation
				   Cookie userDocBaseCookie = new Cookie ("userDocBase", docbase);
				   logger.info("[HTTPBASICAUTHENTICATIONPROCESS] Cookie userDocBase = " + userDocBaseCookie.getValue());
				   userDocBaseCookie.setPath(authCookiePath);
				   userDocBaseCookie.setDomain(authCookieDomain);
				   //CLAZARO: add cookie below
                                   //response.addCookie(userDocBaseCookie);
                                   
                                    //CLAZARO: add sendCookies support
                                    boolean isSessionEnabled = new Boolean (valveConf.getSessionConfig().isSessionEnabled()).booleanValue();
                                    boolean sendCookies = false;
                                    if (isSessionEnabled) {
                                        sendCookies = new Boolean (valveConf.getSessionConfig().getSendCookies()).booleanValue();
                                    }
                                    if ((!isSessionEnabled)||((isSessionEnabled)&&(sendCookies))) {
                                        response.addCookie(extAuthCookie);
                                        response.addCookie(userIdCookie);
                                        response.addCookie(userDocBaseCookie);
                                    }
                                    
                                    //CLAZARO: add cookies to array
                                    authCookies.add(extAuthCookie); 
                                    authCookies.add(userIdCookie);
                                    authCookies.add(userDocBaseCookie);
				   
				   logger.debug("release connection");
				   WebProcResponse.releaseConnection();
			    
			}
		
			///request.setAttribute("status",Integer.toString(statusCode));
			logger.info("[HTTPBASICAUTHENTICATIONPROCESS] Return status is :" +Integer.toString(statusCode));
			
//			Clear webProcessor cookies
	        webProcessor.clearCookies();
			
		}catch (HttpException he) {
			logger.error("[HTTPBASICAUTHENTICATIONPROCESS] HttpException "+he.getMessage() + " ");
			logger.error(he.getCause() + " " );
		} catch (IOException ioe) {
			logger.error("[HTTPBASICAUTHENTICATIONPROCESS] IOException "+ioe.getMessage());
                } catch (LoginException le) {
		            logger.error("[HTTPBASICAUTHENTICATIONPROCESS] LoginException "
		                                    + le.getMessage(),le);
		} catch (SAXException e) {
			logger.error("[HTTPBASICAUTHENTICATIONPROCESS] SAXException "+e.getMessage());
			logger.error(e.getCause() + " " );
		}
		return statusCode;					
	}


}
