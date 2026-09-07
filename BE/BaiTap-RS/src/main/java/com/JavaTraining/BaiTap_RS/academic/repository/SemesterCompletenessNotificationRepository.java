package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterCompletenessNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SemesterCompletenessNotificationRepository
        extends JpaRepository<SemesterCompletenessNotification, Long> {

    List<SemesterCompletenessNotification> findAllBySemesterIdOrderByCreatedAtDesc(Long semesterId);

    List<SemesterCompletenessNotification> findAllBySemesterIdAndCheckpointCode(
            Long semesterId,
            String checkpointCode);

    List<SemesterCompletenessNotification> findAllByStatusAndAttemptCountLessThan(
            NotificationStatus status,
            int maxAttempts);

    boolean existsBySemesterIdAndCheckpointCodeAndRecipientEmailAndNotificationChannel(
            Long semesterId,
            String checkpointCode,
            String recipientEmail,
            NotificationChannel notificationChannel);

    Optional<SemesterCompletenessNotification>
            findBySemesterIdAndCheckpointCodeAndRecipientEmailAndNotificationChannel(
                    Long semesterId,
                    String checkpointCode,
                    String recipientEmail,
                    NotificationChannel notificationChannel);
}
