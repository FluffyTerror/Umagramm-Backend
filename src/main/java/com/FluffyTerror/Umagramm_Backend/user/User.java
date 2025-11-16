package com.FluffyTerror.Umagramm_Backend.user;

import com.FluffyTerror.Umagramm_Backend.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.common.BaseAuditingEntity;
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
@Table(name = "users")
public class User extends BaseAuditingEntity {
    private static final int LAST_ACTIVE_INTERVAL = 1;
    @Id
    private String id;
    // private Long id; TODO когда будем убирать keycloak id нужно будет поставить тип а LOng, а не String

    private String firstName;

    private String lastName;

    private String username;

    private LocalDateTime lastSeen;
@OneToMany(mappedBy = "sender")
    private List<Chat> chatsAsSender;
    @OneToMany(mappedBy = "recipient")
    private List<Chat> chatsAsRecipient;

    @Transient
    public boolean isUserOnline(){
        //если юзера нет в сети минуту -> он оффлайн
        return lastSeen!= null && lastSeen.isAfter(LocalDateTime.now().minusMinutes(LAST_ACTIVE_INTERVAL));
    }
}
