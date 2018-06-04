package br.com.willianantunes.route;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.telegram.model.IncomingMessage;

import br.com.willianantunes.model.ChatTransaction;
import br.com.willianantunes.model.Room;

public class RouteHelper {
    
    public static String verifyUserConversation() {
        
        return String.format("jpa:%s?namedQuery=%s&consumeDelete=%s&parameters={\"chatId\":${body.chat.id}}",
                ChatTransaction.class.getName(), ChatTransaction.CHAT_TRANSACTION_NAMED_QUERY_SELECT_NOT_FINISHED_BY_CHAT_ID, false);
    }

    public static String verifyUserConversation(String propertyTelegramMessage) {

        return String.format("jpa:%s?namedQuery=%s&consumeDelete=%s&parameters={\"chatId\":${exchangeProperty[%s].chat.id}}",
            ChatTransaction.class.getName(), ChatTransaction.CHAT_TRANSACTION_NAMED_QUERY_SELECT_NOT_FINISHED_BY_CHAT_ID, false, propertyTelegramMessage);
    }
    
    public static String verifyWhoTheUserIsWatching() {
        
        return String.format("jpa:%s?namedQuery=%s&consumeDelete=%s&parameters={\"chatId\":${body.chat.id}}",
                Room.class.getName(), Room.NQ_ROOM_SELECT_BY_CHAT_ID, false);
    }
    
    public static String verifyTheUserIsWatching() {
        
        return String.format("jpa:%s?namedQuery=%s&consumeDelete=%s&parameters={\"chatId\":${body.chat.id}, \"name\":\"${in.body}\"}",
                Room.class.getName(), Room.NQ_ROOM_SELECT_BY_CHAT_ID_AND_POLITICIAN_NAME, false);
    }

    public static String finishChatTransaction(String propertyTelegramMessage) {

        return String.format("jpa:%s?namedQuery=%s&useExecuteUpdate=%s&parameters={\"chatId\":${exchangeProperty[%s].chat.id}}",
                ChatTransaction.class.getName(), ChatTransaction.CHAT_TRANSACTION_NAMED_QUERY_FINISH_CONVERSATION_BY_CHAT_ID, true, propertyTelegramMessage);
    }

    public static Processor prepareChatTransactionToBeUpdatedUsingBodyMessage(String propertyTelegramMessage) {

        return exchange -> {

            Optional<ChatTransaction> previousMessage = Optional.ofNullable(exchange.getIn().getBody(ChatTransaction.class));
            IncomingMessage message = (IncomingMessage) exchange.getProperty(propertyTelegramMessage);

            previousMessage.ifPresent(chatTransaction -> {

                chatTransaction.setMessageId(message.getMessageId().intValue());
                chatTransaction.setMessage(message.getText());
                chatTransaction.setFirstName(message.getFrom().getFirstName());
                chatTransaction.setLastName(message.getFrom().getLastName());
                chatTransaction.setSentAt(LocalDateTime.ofInstant(message.getDate(), ZoneId.systemDefault()));

                exchange.getIn().setBody(chatTransaction);
            });
        };
    }

    public static Processor prepareChatTransactionToBeUpdatedAsExecuting() {

        return exchange -> {

            IncomingMessage incomingMessage = exchange.getIn().getBody(IncomingMessage.class);

            ChatTransaction chatTransaction = ChatTransaction.builder()
                .chatId(Integer.parseInt(incomingMessage.getChat().getId()))
                .executed(false)
                .build();

            exchange.getIn().setBody(chatTransaction);
        };
    }

    public static Processor prepareChatTransactionToBeUpdatedWithCustomProperty(String propertyTelegramMessage, String chatEndpoint, String property) {

        return exchange -> {

            SimpleBuilder simple = new SimpleBuilder(String.format("${exchangeProperty[%s]}", property));
            String value = simple.evaluate(exchange, String.class);

            IncomingMessage message = exchange.getProperty(propertyTelegramMessage, IncomingMessage.class);
            ChatTransaction chatTransaction = exchange.getIn().getBody(ChatTransaction.class);

            Map<String, String> properties = Optional.ofNullable(chatTransaction.getChatProperties()).orElse(new HashMap<>());
            properties.put(property, value);
            chatTransaction.setChatProperties(properties);
            chatTransaction.setChatEndpoint(chatEndpoint);
            chatTransaction.setExecuted(true);

            exchange.getIn().setBody(chatTransaction);
        };
    }
    
    public static Processor prepareMessageToBePersisted(String chatEndpoint) {
        
        return exchange -> {
            
            IncomingMessage message = exchange.getIn().getBody(IncomingMessage.class);
            
            ChatTransaction chatTransaction = ChatTransaction.builder()
                .chatId(Integer.parseInt(message.getChat().getId()))
                .messageId(message.getMessageId().intValue())
                .message(message.getText())
                .firstName(message.getFrom().getFirstName())
                .lastName(message.getFrom().getLastName())
                .sentAt(LocalDateTime.ofInstant(message.getDate(), ZoneId.systemDefault()))
                .finished(false)
                .chatEndpoint(chatEndpoint).build();
            
            exchange.getIn().setBody(chatTransaction);
        };
    }

    public static Processor prepareMessageToBePersistedByProperty(String chatEndpoint, String propertyTelegramMessage) {

        return exchange -> {
            
            IncomingMessage message = exchange.getProperty(propertyTelegramMessage, IncomingMessage.class);
            
            ChatTransaction chatTransaction = ChatTransaction.builder()
                .chatId(Integer.parseInt(message.getChat().getId()))
                .messageId(message.getMessageId().intValue())
                .message(message.getText())
                .firstName(message.getFrom().getFirstName())
                .lastName(message.getFrom().getLastName())
                .sentAt(LocalDateTime.ofInstant(message.getDate(), ZoneId.systemDefault()))
                .finished(false)
                .chatEndpoint(chatEndpoint).build();
            
            exchange.getIn().setBody(chatTransaction);
        };
    }
}