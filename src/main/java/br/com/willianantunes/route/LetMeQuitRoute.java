package br.com.willianantunes.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.willianantunes.component.Messages;

@Component
public class LetMeQuitRoute extends RouteBuilder {

    public static final String ROUTE_ID_FIRST_CONTACT = LetMeQuitRoute.class.getSimpleName();
    
    public static final String DIRECT_ENDPOINT_RECEPTION = ROUTE_ID_FIRST_CONTACT.concat("-DIRECT-RECEPTION");
    
    @Autowired
    private Messages messages;

    @Override
    public void configure() throws Exception {
        
        fromF("direct:%s", DIRECT_ENDPOINT_RECEPTION).routeId(ROUTE_ID_FIRST_CONTACT)
            .setBody(constant(messages.get(Messages.COMMAND_NOT_AVAILABLE)))
            .to("log:INFO?showHeaders=true")
            .to("telegram:bots");
    }
}