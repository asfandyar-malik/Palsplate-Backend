package com.jersey.resources;

import com.jersey.representations.Email;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.cloudinary.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.HashMap;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Transactional
@Component
public class EmailResource {

    private static final Logger log = LogManager.getLogger(EmailResource.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("public/emails")
    public org.json.simple.JSONObject sendEmail(@Valid Email email) throws FileNotFoundException {

        if (!email.token.equalsIgnoreCase(System.getenv().get("PALSPLATE_EMAIL_TOKEN"))) {
            throw new WebApplicationException((Response.Status.UNAUTHORIZED));
        }
        else{

            if(email.type.equalsIgnoreCase("signup_successful")){
                email.from = "Palsplate UG <info@mg.palsplate.com>";
                if(email.person_id == null || email.recipientName == null){
                    throw new WebApplicationException((Response.Status.BAD_REQUEST));
                }
                else if (email.locale.equalsIgnoreCase("en")){
                    email.body = EmailResource.htmlIntoString("en_signup_successful.html");
                }
                else if (email.locale.equalsIgnoreCase("de")){
                    email.body = EmailResource.htmlIntoString("de_signup_successful.html");
                }
            }

            if(email.type.equalsIgnoreCase("reservation_cook")){
                email.from = "Palsplate UG <info@mg.palsplate.com>";
                if(email.recipientName == null || email.foodName ==null
                        || email.reservation_id == null){
                    throw new WebApplicationException((Response.Status.BAD_REQUEST));
                }
                else if (email.locale.equalsIgnoreCase("en")){
                    email.body = EmailResource.htmlIntoString("en_reservation_cook.html");
                }
                else if (email.locale.equalsIgnoreCase("de")){
                    email.body = EmailResource.htmlIntoString("de_reservation_cook.html");
                }
            }

            if(email.type.equalsIgnoreCase("reservation_customer")){
                email.from = "Palsplate UG <info@mg.palsplate.com>";
                if(email.recipientName == null || email.foodName == null || email.foodPrice ==null
                        || email.foodOfferStart == null || email.reservation_id == null){
                    throw new WebApplicationException((Response.Status.BAD_REQUEST));
                }
                else if (email.locale.equalsIgnoreCase("en")){
                    email.body = EmailResource.htmlIntoString("en_reservation_customer.html");
                }
                else if (email.locale.equalsIgnoreCase("de")){
                    email.body = EmailResource.htmlIntoString("de_reservation_customer.html");
                }
            }

            if(email.type.equalsIgnoreCase("reservation_cancel")){
                email.from = "Palsplate UG <info@mg.palsplate.com>";
                if(email.recipientName == null || email.foodName == null || email.foodPrice ==null
                        || email.foodOfferStart == null){
                    throw new WebApplicationException((Response.Status.BAD_REQUEST));
                }
                else if (email.locale.equalsIgnoreCase("en")){
                    email.body = EmailResource.htmlIntoString("en_reservation_cancel.html");
//                email.body = "Hi digga";
                }
                else if (email.locale.equalsIgnoreCase("de")){
                    email.body = EmailResource.htmlIntoString("de_reservation_cancel.html");
//                    email.body = "Hi digga";

                }
            }

            if(email.type.equalsIgnoreCase("contact_us")) {
                if (email.body == null || email.from == null || email.subject == null) {
                    throw new WebApplicationException((Response.Status.BAD_REQUEST));
                }
                email.recipientEmail = "info@palsplate.com";
                email.recipientName = null;
                email.reservation_id = null;
                email.person_id = null;
                email.foodName = null;
                email.foodPrice = null;
                email.foodOfferStart = null;
                email.foodOfferStop = null;
            }

            ClientResponse response = EmailResource.sendComplexMessage(
                    email.subject,
                    email.recipientEmail,
                    email.recipientName,
                    email.from,
                    email.body,
                    email.reservation_id,
                    email.person_id,
                    email.foodName,
                    email.foodPrice,
                    email.foodOfferStart,
                    email.foodOfferStop);

            org.json.simple.JSONObject emailResponse = new org.json.simple.JSONObject();
            emailResponse.put("response date", response.getResponseDate());
            emailResponse.put("response status", response.getStatus());

            return emailResponse;
        }
    }

    public static ClientResponse sendComplexMessage(String subject,
                                             String recipientEmail,
                                             String recipientName,
                                             String from,
                                             String body,
                                             String reservation_id,
                                             String person_id,
                                             String foodName,
                                             String foodPrice,
                                             String foodOfferStart,
                                             String foodOfferStop) {

        JSONObject recipientVariableJson = new JSONObject();
        JSONObject nameObject = new JSONObject();
        nameObject.put("name", recipientName);
        nameObject.put("reservation_id", reservation_id);
        nameObject.put("person_id", person_id);
        nameObject.put("foodName", foodName);
        nameObject.put("foodPrice", foodPrice);
        nameObject.put("foodOfferStart", foodOfferStart);
        nameObject.put("foodOfferStop", foodOfferStop);
        nameObject.put("reservationUrl", "https://www.palsplate.com/reservations/" + reservation_id);
        nameObject.put("personUrl", "https://www.palsplate.com/users/" + person_id);

        recipientVariableJson.put(recipientEmail, nameObject);

        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter("api", System.getenv().get("MAILGUN_APIKEY")));
        WebResource webResource = client.resource("https://api.mailgun.net/v3/mg.palsplate.com/messages");

        FormDataMultiPart formData = new FormDataMultiPart();
        formData.field("from", from);
        formData.field("to", recipientEmail);
        formData.field("subject", subject.toString());
        formData.field("html", body);
        formData.field("recipient-variables", recipientVariableJson.toString());

        ClientResponse clientResponse = webResource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, formData);
        log.info(clientResponse.toString());

        return clientResponse;
    }

    public static ClientResponse sendComplexMessage(String subject,
                                                    String recipientEmail,
                                                    String recipientName,
                                                    String from,
                                                    String body) {

        JSONObject recipientVariableJson = new JSONObject();
        JSONObject nameObject = new JSONObject();
        nameObject.put("name", recipientName);


        recipientVariableJson.put(recipientEmail, nameObject);
        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter("api", System.getenv().get("MAILGUN_APIKEY")));
        WebResource webResource = client.resource("https://api.mailgun.net/v3/mg.palsplate.com/messages");

        FormDataMultiPart formData = new FormDataMultiPart();
        formData.field("from", from);
        formData.field("to", recipientEmail);
        formData.field("subject", subject.toString());
        formData.field("html", body);
        formData.field("recipient-variables", recipientVariableJson.toString());

        ClientResponse clientResponse = webResource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, formData);
        log.info(clientResponse.toString());

        return clientResponse;
    }

    public static String htmlIntoString(String file) throws FileNotFoundException {

        String content = "";
        try {
            ClassLoader classLoader = EmailResource.class.getClassLoader();
            BufferedReader in = new BufferedReader(new FileReader(new File(classLoader.getResource(file).getFile())));
            String str;
            while ((str = in.readLine()) != null) {
                content += str;
            }
            in.close();
        } catch (IOException e) {
        }

        return content;
    }
}
