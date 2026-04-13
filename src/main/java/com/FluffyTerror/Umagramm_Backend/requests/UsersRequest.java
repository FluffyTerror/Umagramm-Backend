package com.FluffyTerror.Umagramm_Backend.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersRequest {
    private List<String> userIds;
}
