package com.FluffyTerror.Umagramm_Backend.models.chat;

import com.FluffyTerror.Umagramm_Backend.common.BaseAuditingEntity;
import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("chat")
public class Chat extends BaseAuditingEntity {

    @Id
    private Long id;

    private String senderId;
    private String recipientId;

//    @Transient
//    public String getChatName(final String senderId){
//        if (recipient.getId().equals(senderId)){
//            return sender.getFirstName()+ " " + sender.getLastName();
//        }
//        return recipient.getFirstName() + " " + recipient.getLastName();
//    }
//
//    @Transient
//    public String getChatName(final String senderId){
//        if (recipient.getId().equals(senderId)){
//            return sender.getFirstName()+ " " + sender.getLastName();
//        }
//        return recipient.getFirstName() + " " + recipient.getLastName();
//    }
//
//    @Transient
//    public Long getUnreadMessages(final String senderId){
//        return messages
//                .stream()
//                .filter(m-> getRecipient().getId().equals(senderId))
//                .filter(m -> MessageState.SENT == m.getMessageState()).count();
//    }
//    @Transient
//    public String getLastMessage(){
//        if (messages != null && !messages.isEmpty()){
//            if(messages.get(0).getMessageType()!= MessageType.TEXT){
//            return "Attachment";
//            }
//            return messages.get(0).getContent();
//        }
//        return null;
//    }
//    @Transient
//    public LocalDateTime getLastMessageTime(){
//        if (messages != null && !messages.isEmpty()){
//            return messages.get(0).getCreatedDate();
//        }
//        return null;
//    }


}
