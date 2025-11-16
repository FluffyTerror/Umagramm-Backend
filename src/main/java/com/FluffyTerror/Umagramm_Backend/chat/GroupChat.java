package com.FluffyTerror.Umagramm_Backend.chat;

import com.FluffyTerror.Umagramm_Backend.common.BaseAuditingEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "group_chat")
public class GroupChat extends BaseAuditingEntity {
}
