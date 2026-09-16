package com.JavaTraining.BaiTap_RS.notification.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationReceiptRepository extends JpaRepository<NotificationReceipt, Long> {

    Optional<NotificationReceipt> findByNotificationIdAndRecipientUserId(Long notificationId, Long recipientUserId);

    boolean existsByNotificationIdAndRecipientUserId(Long notificationId, Long recipientUserId);

    @Query("""
            select receipt from NotificationReceipt receipt
            where receipt.recipientUserId = :recipientUserId
              and exists (
                select notification.id from Notification notification
                where notification.id = receipt.notificationId
                  and notification.status = com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus.PUBLISHED
                  and (notification.publishAt is null or notification.publishAt <= CURRENT_TIMESTAMP)
                  and (notification.expiresAt is null or notification.expiresAt > CURRENT_TIMESTAMP)
              )
            order by receipt.createdAt desc
            """)
    Page<NotificationReceipt> findVisibleByRecipientUserIdOrderByCreatedAtDesc(
            Long recipientUserId, Pageable pageable);

    @Query("""
            select receipt from NotificationReceipt receipt
            where receipt.recipientUserId = :recipientUserId
              and receipt.readAt is null
              and exists (
                select notification.id from Notification notification
                where notification.id = receipt.notificationId
                  and notification.status = com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus.PUBLISHED
                  and (notification.publishAt is null or notification.publishAt <= CURRENT_TIMESTAMP)
                  and (notification.expiresAt is null or notification.expiresAt > CURRENT_TIMESTAMP)
              )
            order by receipt.createdAt desc
            """)
    Page<NotificationReceipt> findVisibleUnreadByRecipientUserIdOrderByCreatedAtDesc(
            Long recipientUserId, Pageable pageable);

    List<NotificationReceipt> findByNotificationId(Long notificationId);
}
