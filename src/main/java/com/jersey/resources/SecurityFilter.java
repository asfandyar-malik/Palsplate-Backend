package com.jersey.resources;

import com.jersey.persistence.LoginDao;
import com.jersey.representations.Login;
import org.glassfish.jersey.internal.util.Base64;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;

@Provider
public class SecurityFilter implements ContainerRequestFilter {

	private LoginDao loginDao;
	private static final String AUTHORIZATION_HEADER_KEY = "Authorization"; 
	private static final String AUTHORIZATION_HEADER_PREFIX = "Basic "; 
	private static final String SECURED_URL_PREFIX = "secured"; 

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if (requestContext.getUriInfo().getPath().contains(SECURED_URL_PREFIX)) {
			List<String> authHeader = requestContext.getHeaders().get(AUTHORIZATION_HEADER_KEY);
			if (authHeader != null && authHeader.size() > 0) {
				String authToken = authHeader.get(0);
				authToken = authToken.replaceFirst(AUTHORIZATION_HEADER_PREFIX, "");
				String decodedString = Base64.decodeAsString(authToken);
				StringTokenizer tokenizer = new StringTokenizer(decodedString, ":");
				String username = tokenizer.nextToken();
				String password = tokenizer.nextToken();

//				List<Login> logins = this.loginDao.findAll();
//				System.out.println("all Logins: " + logins);
//
//				for(Login thisLogin: logins){
//					if(thisLogin.getUserName().equals(username) && thisLogin.getPassword().equals(password)){
//						return;
//					}
//				}

				if ("user".equals(username) && "password".equals(password)) {
					return;
				}
			}

			Response unauthorizedStatus = Response
								            .status(Response.Status.UNAUTHORIZED)
								            .entity("User cannot access the resource. Please pass in Basic Authentication header")
								            .build();
					
			requestContext.abortWith(unauthorizedStatus);
		}
	}
}
