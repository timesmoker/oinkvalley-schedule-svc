package com.oinkvalley.schedule_svc.client;

import com.oinkvalley.profile.v1.BatchGetProfileNicknamesRequest;
import com.oinkvalley.profile.v1.ProfileInternalServiceGrpc;
import com.oinkvalley.profile.v1.ProfileNickname;
import io.grpc.StatusRuntimeException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** profile-svc 내부 gRPC — `user_profiles` 는 profile-svc 만 조회한다. */
@Component
@RequiredArgsConstructor
public class UserProfileClient {

    private final ProfileInternalServiceGrpc.ProfileInternalServiceBlockingStub profileStub;

    public String nicknameForUser(long userId) {
        return nicknamesByUserIds(List.of(userId)).get(userId);
    }

    public Map<Long, String> nicknamesByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = userIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            var response = profileStub.batchGetProfileNicknames(
                    BatchGetProfileNicknamesRequest.newBuilder().addAllUserIds(ids).build());
            Map<Long, String> out = new HashMap<>();
            for (ProfileNickname profile : response.getProfilesList()) {
                if (profile.getUserId() > 0 && !profile.getNickname().isBlank()) {
                    out.put(profile.getUserId(), profile.getNickname().trim());
                }
            }
            return out;
        } catch (StatusRuntimeException e) {
            return Map.of();
        }
    }
}
