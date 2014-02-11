package com.baasbox.controllers;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.push.PushService;
import com.baasbox.service.push.providers.PushNotInitializedException;
import com.baasbox.service.user.UserService;
import com.google.android.gcm.server.InvalidRequestException;

@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
@BodyParser.Of(BodyParser.Json.class)
public class Push extends Controller {
	 public static Result send(String username)  {
		 if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		 Http.RequestBody body = request().body();
		 JsonNode bodyJson= body.asJson(); //{"message":"Text"}
		 if (Logger.isTraceEnabled()) Logger.trace("send bodyJson: " + bodyJson);
		 if (bodyJson==null) return badRequest("The body payload cannot be empty.");		  
		 JsonNode messageNode=bodyJson.findValue("message");
		 if (messageNode==null) return badRequest("The body payload doesn't contain key message");
		 String message=messageNode.asText();	  
		 PushService ps=new PushService();
		 try{
		    	ps.send(message, username);
		 }
		 catch (UserNotFoundException e) {
			    Logger.error("Username not found " + username, e);
			    return notFound("Username not found");
		 }
		 catch (SqlInjectionException e) {
			    return badRequest("the supplied name appears invalid (Sql Injection Attack detected)");
		 }
		 catch (InvalidRequestException e){
			 	Logger.error(e.getMessage());
			 	return status(CustomHttpCode.PUSH_INVALID_REQUEST.getBbCode(),CustomHttpCode.PUSH_INVALID_REQUEST.getDescription());
		 }
		 catch (PushNotInitializedException e){
			 	Logger.error(e.getMessage());
			 	return status(CustomHttpCode.PUSH_CONFIG_INVALID.getBbCode(), CustomHttpCode.PUSH_CONFIG_INVALID.getDescription());
		 }
		 catch (UnknownHostException e){
			 	Logger.error(e.getMessage());
			 	return status(CustomHttpCode.PUSH_HOST_UNREACHABLE.getBbCode(),CustomHttpCode.PUSH_HOST_UNREACHABLE.getDescription());
		 }catch (IOException e) {
			 return badRequest(e.getMessage());
		}
		 
		
		 if (Logger.isTraceEnabled()) Logger.trace("Method End");
		 return ok();
	  }
	
	 public static Result enablePush(String os, String pushToken) throws SqlInjectionException{
		 if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		 if(os==null) return badRequest("Os value doesn't not null");
		 if(pushToken==null) return badRequest("pushToken value doesn't not null");
		 if (Logger.isDebugEnabled()) Logger.debug("Trying to enable push to os: "+os+" pushToken: "+ pushToken); 
		 HashMap<String, Object> data = new HashMap<String, Object>();
         data.put("os",os);
         data.put(UserDao.USER_PUSH_TOKEN, pushToken);
		 UserService.registerDevice(data);
		 if (Logger.isTraceEnabled()) Logger.trace("Method End");
		 return ok();
		 
	  }
	   
	 
	/*
	 public static Result sendAll(String message) throws PushNotInitializedException {
	}*/
}

