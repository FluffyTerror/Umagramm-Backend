package com.FluffyTerror.Umagramm_Backend.repository.attachment;

import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AttachmentRepository extends ReactiveCrudRepository<Attachment, Long> {
    @Query("SELECT * FROM attachments WHERE message_id = :messageId")
    Flux<Attachment> findByMessageId(Long messageId);

    @Query("DELETE FROM attachments WHERE message_id = :messageId")
    Mono<Integer> deleteByMessageId(Long messageId);
}
