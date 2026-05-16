package com.FluffyTerror.Umagramm_Backend.service.chat;

import com.FluffyTerror.Umagramm_Backend.enums.*;
import com.FluffyTerror.Umagramm_Backend.exceptions.FriendNotFoundException;
import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.models.chatMember.ChatMember;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.repository.chat.ChatRepository;
import com.FluffyTerror.Umagramm_Backend.repository.chatMember.ChatMemberRepository;
import com.FluffyTerror.Umagramm_Backend.repository.friends.FriendsRepository;
import com.FluffyTerror.Umagramm_Backend.repository.message.MessageRepository;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.requests.groupChat.AddChatMemberRequest;
import com.FluffyTerror.Umagramm_Backend.requests.groupChat.CreateGroupChatRequest;
import com.FluffyTerror.Umagramm_Backend.requests.groupChat.KickChatMemberRequest;
import com.FluffyTerror.Umagramm_Backend.responses.ChatMemberResponse;
import com.FluffyTerror.Umagramm_Backend.responses.chat.ChatResponse;
import com.FluffyTerror.Umagramm_Backend.service.attachment.AttachmentService;
import com.FluffyTerror.Umagramm_Backend.service.push.PushNotificationService;
import com.FluffyTerror.Umagramm_Backend.websockets.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final FriendsRepository friendsRepository;
    private final DatabaseClient databaseClient;
    private final AttachmentService attachmentService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final PushNotificationService pushNotificationService;

    /**
     * Создает новый ПРИВАТНЫЙ чат или возвращает существующий с полной информацией
     */
    @Transactional
    public Mono<ChatResponse> createPrivateChat(String creatorId, String partnerId) {
        return chatMemberRepository.findCommonPrivateChat(creatorId, partnerId).switchIfEmpty(Mono.defer(() -> createNewChat(creatorId, partnerId))).flatMap(chat -> enrichChatWithDetails(chat, creatorId)).doOnError(e -> log.error("Error during creating a chat: ", e));
    }

    /**
     * Внутренний метод только для создания записей в БД
     */
    private Mono<Chat> createNewChat(String creatorId, String partnerId) {
        return friendsRepository.findFriendshipBySenderIdAndReceiverId(creatorId, partnerId, FriendStatus.Accepted).switchIfEmpty(Mono.error(new FriendNotFoundException("You are not allowed to create chat with this user, consider adding him into friends first!"))).flatMap(friend -> {
            Chat chat = new Chat();
            chat.setId(UUID.randomUUID().toString());
            chat.setChatType(ChatType.PRIVATE);
            chat.markNew();
            return chatRepository.save(chat).flatMap(savedChat -> {
                ChatMember m1 = createMember(savedChat.getId(), creatorId, null, MemberRole.MEMBER);
                ChatMember m2 = createMember(savedChat.getId(), partnerId, null, MemberRole.MEMBER);
                return chatMemberRepository.save(m1).then(chatMemberRepository.save(m2)).thenReturn(savedChat);
            });
        });
    }

    @Transactional
    public Mono<ChatResponse> createGroupChat(String creatorId, CreateGroupChatRequest request, Mono<FilePart> filePart) {

        return filePart
                // 1. Используем новый метод, который возвращает Mono<Attachment>
                .flatMap(fp -> attachmentService.saveGroupAvatar(fp, creatorId)).map(Attachment::getId) // Теперь тут объект Attachment, берем его ID
                .defaultIfEmpty(1L).flatMap(avatarId -> {
                    Chat chat = new Chat();
                    chat.setId(UUID.randomUUID().toString());
                    chat.setChatType(ChatType.GROUP);
                    chat.setTitle(request.title());
                    chat.setAvatarId(avatarId);
                    chat.markNew();

                    return chatRepository.save(chat).flatMap(savedChat -> {
                        ChatMember admin = createMember(savedChat.getId(), creatorId, null, MemberRole.ADMIN);
                        List<ChatMember> members = request.usersId().stream().map(uId -> createMember(savedChat.getId(), uId, creatorId, MemberRole.MEMBER)).toList();

                        return chatMemberRepository.save(admin).thenMany(Flux.fromIterable(members)).flatMap(chatMemberRepository::save).then(Mono.just(savedChat)); // .then() превращает Flux в Mono<Void>
                    });
                }).flatMap(savedChat -> enrichChatWithDetails(savedChat, creatorId));
    }

    /**
     * Маппинг Chat -> ChatResponse
     */
    private Mono<ChatResponse> enrichChatWithDetails(Chat chat, String currentUserId) {
        boolean isGroup = chat.getChatType() == ChatType.GROUP;

        // 1. Определяем ID партнера: для групп это всегда пустая строка/null, для привата — ищем в базе
        Mono<String> partnerIdMono = isGroup ? Mono.just("") : getPartnerId(chat.getId(), currentUserId).defaultIfEmpty("");

        // 2. Объединяем получение метаданных (имя и ID партнера)
        return Mono.zip(partnerIdMono, getChatName(chat, currentUserId)).flatMap(meta -> {
            String partnerId = meta.getT1();
            String displayName = meta.getT2();

            // 3. Собираем статистику: непрочитанные, статус "в сети" и последнее сообщение
            return Mono.zip(getUnreadMessages(chat.getId(), currentUserId), isPartnerOnline(partnerId, chat.getChatType()), getLastMessage(chat.getId()).defaultIfEmpty(new Message())).map(stats -> createResponse(chat, currentUserId, isGroup ? null : partnerId, // receiverId для групп не нужен
                    stats.getT1(), // unread
                    stats.getT2(), // online
                    stats.getT3(), // lastMessage
                    displayName));
        });
    }

    private ChatResponse createResponse(Chat chat, String currentUserId, String receiverId, Long unread, Boolean online, Message msg, String chatName) {
        // Определяем, кто реально отправил последнее сообщение
        String actualSenderId = (msg != null && msg.getSenderId() != null) ? msg.getSenderId() : currentUserId;

        return new ChatResponse(
                chat.getId(),
                chatName,
                actualSenderId,
                receiverId,
                chat.getChatType().name(),
                unread,
                msg != null ? msg.getContent() : null,
                msg != null ? msg.getInitialisationVector() : null,
                msg !=null ? msg.getKeyVersion() : null,
                msg != null ? msg.getCreatedDate() : null,
                online,
                chat.getAvatarId()
        );
    }

    // --- Вспомогательные методы остаются почти такими же ---

    private Mono<String> getPartnerId(String chatId, String currentUserId) {
        return chatMemberRepository.findAllByChatId(chatId).filter(member -> !member.getUserId().equals(currentUserId)).next().map(ChatMember::getUserId);
    }

    private Flux<String> getPartnersId(String chatId, String currentUserId) {
        return chatMemberRepository.findAllByChatId(chatId).filter(member -> !member.getUserId().equals(currentUserId)).map(ChatMember::getUserId);

    }

    public Mono<Long> getUnreadMessages(String chatId, String currentUserId) {
        return messageRepository.countUnreadMessages(chatId, currentUserId).defaultIfEmpty(0L);
    }

    public Mono<Message> getLastMessage(String chatId) {
        return messageRepository.findLastMessageByChatId(chatId);
    }

    public Mono<Boolean> isPartnerOnline(String partnerId, ChatType type) {
        if (type == ChatType.GROUP || partnerId.isEmpty()) return Mono.just(false);
        return userRepository.findById(partnerId).map(User::isOnline).defaultIfEmpty(false);
    }

    private ChatMember createMember(String chatId, String userId, String invitedBy, MemberRole role) {
        ChatMember member = new ChatMember();
        member.setChatId(chatId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinedAt(LocalDateTime.now());
        member.setNew(true);
        member.setInvitedBy(invitedBy);
        return member;
    }

    /**
     * Определяет название чата для конкретного пользователя.
     * Для групп — это заголовок группы.
     * Для личных чатов — это имя и фамилия собеседника.
     */
    public Mono<String> getChatName(Chat chat, String currentUserId) {
        if (chat.getChatType() == ChatType.GROUP) {
            return Mono.just(chat.getTitle() != null ? chat.getTitle() : "Group Chat");
        }

        // Для личного чата ищем имя "другого" человека
        return getPartnerId(chat.getId(), currentUserId).flatMap(userRepository::findById).map(u -> {
            String firstName = u.getFirstName() != null ? u.getFirstName() : "";
            String lastName = u.getLastName() != null ? u.getLastName() : "";
            String full = (firstName + " " + lastName).trim();
            return full.isEmpty() ? "Unknown User" : full;
        }).defaultIfEmpty("Unknown User");
    }

    public Flux<ChatResponse> getUserChats(String userId) {
        // 1. Ищем все записи в chat_member для этого юзера
        return chatMemberRepository.findAllByUserId(userId)
                // 2. Для каждого участника достаем саму сущность Chat
                .flatMap(member -> chatRepository.findById(member.getChatId()))
                // 3. Используем твой готовый метод для сборки ChatResponse
                .flatMap(chat -> enrichChatWithDetails(chat, userId));
    }

    public Mono<ChatMember> addChatMemberToChat(AddChatMemberRequest request, String initiatorId) {
        return chatRepository.findById(request.chatId())
                .filter(chat -> chat.getChatType() == ChatType.GROUP)
                .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("Only for group chats")))
                // Открываем блок { }, чтобы сохранить переменную 'chat' в области видимости
                .flatMap(chat -> {

                    return chatMemberRepository.findByChatIdAndUserId(chat.getId(), initiatorId)
                            .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("You are not in this chat")))
                            .flatMap(initiatorMember -> chatMemberRepository.findByChatIdAndUserId(request.chatId(), request.userId())
                                    .flatMap(existing -> Mono.<ChatMember>error(new IllegalArgumentException("Already in chat")))
                                    .switchIfEmpty(Mono.defer(() -> {

                                        // 1. Теперь 'chat' доступен, так как мы всё ещё внутри верхнего flatMap
                                        chat.setKeyVersion(chat.getKeyVersion() + 1);

                                        ChatMember chatMember = createMember(request.chatId(), request.userId(), initiatorId, MemberRole.MEMBER);

                                        // 2. Сохраняем обновленный чат и нового участника
                                        return chatRepository.save(chat)
                                                .then(chatMemberRepository.save(chatMember))
                                                .flatMap(savedMember -> userRepository.findById(request.userId())
                                                        .flatMap(user -> {
                                                            String name = (user.getFirstName() + " " + user.getLastName()).trim();
                                                            // Передаем текущую версию ключа в системное сообщение
                                                            return saveSystemMessage(
                                                                    request.chatId(),
                                                                    name + " joined the chat",
                                                                    initiatorId,
                                                                    request.userId(),
                                                                    "CHAT_MEMBER_ADDED",
                                                                    chat.getKeyVersion()
                                                            );
                                                        })
                                                        .thenReturn(savedMember));
                                    }))
                            );
                }); // Закрываем блок flatMap(chat -> { ... })
    }

    @Transactional
    public Mono<Void> kickChatMember(KickChatMemberRequest request, String initiatorId) {
        return chatRepository.findById(request.chatId())
                .filter(chat -> chat.getChatType() == ChatType.GROUP)
                .flatMap(chat -> Mono.zip(
                        chatMemberRepository.findByChatIdAndUserId(request.chatId(), initiatorId),
                        chatMemberRepository.findByChatIdAndUserId(request.chatId(), request.userId()),
                        userRepository.findById(request.userId())
                ).flatMap(tuple -> {
                    ChatMember initiator = tuple.getT1();
                    ChatMember target = tuple.getT2();
                    User targetUser = tuple.getT3();

                    boolean isAdmin = initiator.getRole() == MemberRole.ADMIN;
                    boolean isSelfKick = initiatorId.equals(request.userId());
                    boolean isInviter = initiatorId.equals(target.getInvitedBy());

                    if (isAdmin || isSelfKick || isInviter) {
                        String name = (targetUser.getFirstName() + " " + targetUser.getLastName()).trim();
                        String actionText = isSelfKick ? name + " left the chat" : name + " was removed from the chat";
                        String actionType = isSelfKick ? "CHAT_MEMBER_LEFT" : "CHAT_MEMBER_REMOVED";
                        // 1. Инкрементируем версию ключа чата
                        chat.setKeyVersion(chat.getKeyVersion() + 1);

                        // 2. Сохраняем чат с новой версией, удаляем юзера и шлем системное сообщение
                        return chatRepository.save(chat)
                                .then(chatMemberRepository.delete(target))
                                // ДОБАВЛЕНЫ: request.userId() и actionType
                                .then(saveSystemMessage(
                                        request.chatId(),
                                        actionText,
                                        initiatorId,
                                        request.userId(),
                                        actionType,
                                        chat.getKeyVersion()
                                ));
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                }));
    }

    public Mono<List<ChatMemberResponse>> getChatMembers(String chatId, String requesterId) {
        // 1. Проверяем, что запрашивающий сам является участником этого чата
        return chatMemberRepository.findByChatIdAndUserId(chatId, requesterId)
                .switchIfEmpty(Mono.error(new AccessDeniedException("You are not a member of this chat")))

                // 2. Получаем всех участников чата
                .thenMany(chatMemberRepository.findAllByChatId(chatId))

                // 3. Для каждого участника достаем данные пользователя из UserRepository
                .flatMap(member -> userRepository.findById(member.getUserId())
                        .map(user -> new ChatMemberResponse(
                                user.getId(),
                                member.getRole().name(),
                                user.getUsername(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getProfilePhotoId(), // Предполагаю, что это твой profilePhotoId
                                member.getInvitedBy()
                        ))
                ).collectList();
    }
    // Добавь параметр Long keyVersion
    private Mono<Void> saveSystemMessage(String chatId, String text, String initiatorId, String targetId, String actionType, Long keyVersion) {
        Message systemMsg = new Message();
        systemMsg.setChatId(chatId);
        systemMsg.setContent(text);
        systemMsg.setMessageType(MessageType.SYSTEM);
        systemMsg.setSenderId(initiatorId);
        systemMsg.setCreatedDate(LocalDateTime.now());
        systemMsg.setMessageState(MessageState.SENT);
        systemMsg.setKeyVersion(keyVersion);

        return messageRepository.save(systemMsg)
                .flatMap(savedMsg -> {
                    // 1. Формируем единый payload для вебсокета
                    Map<String, Object> wsPayload = Map.of(
                            "type", actionType,
                            "chatId", chatId,
                            "targetId", targetId,
                            "actorId", initiatorId,
                            "message", savedMsg // Фронт сможет сразу отрисовать это сообщение
                    );

                    // 2. Рассылаем текущим участникам чата
                    Mono<Void> notifyMembers = chatMemberRepository.findAllByChatId(chatId)
                            .flatMap(member -> {
                                // Рассылка по WS всем участникам (включая инициатора)
                                // ВНИМАНИЕ: Замени "sendToUser" на реальный метод из твоего WebSocketNotificationService
                                Mono<Void> wsTask = webSocketNotificationService.sendToUser(member.getUserId(), wsPayload)
                                        .onErrorResume(e -> Mono.empty());

                                // Push-уведомление (всем, КРОМЕ инициатора)
                                Mono<Void> pushTask = Mono.empty();
                                if (!member.getUserId().equals(initiatorId)) {
                                    pushTask = userRepository.findById(member.getUserId())
                                            .doOnNext(user -> pushNotificationService.sendPushNotification(
                                                    user.getFcmToken(),
                                                    "Системное уведомление",
                                                    text
                                            )).then();
                                }

                                return Mono.when(wsTask, pushTask);
                            }).then();

                    // 3. Если пользователя кикнули, его уже нет в chatMemberRepository,
                    // но ему тоже нужно отправить WS-событие, чтобы он закрыл чат у себя
                    Mono<Void> notifyTargetIfKicked = Mono.empty();
                    if (actionType.equals("CHAT_MEMBER_REMOVED")) {
                        notifyTargetIfKicked = webSocketNotificationService.sendToUser(targetId, wsPayload)
                                .onErrorResume(e -> Mono.empty());
                    }

                    return Mono.when(notifyMembers, notifyTargetIfKicked);
                });
    }
}
