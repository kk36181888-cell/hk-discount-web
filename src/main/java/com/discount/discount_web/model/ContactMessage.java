package com.discount.discount_web.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 📬 聯絡表格留言（2026-10-27 新增，配合 /contact 頁面）。
 * 訪客自願填寫，僅用於站方回覆及跟進優惠資料更正；不設會員系統。
 * 資料表由 Hibernate ddl-auto=update 自動建立，站方日後可在後台加閱覽頁。
 */
@Entity
@Table(name = "contact_messages")
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String name;

    @Column(length = 200)
    private String contact;

    @Column(length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
